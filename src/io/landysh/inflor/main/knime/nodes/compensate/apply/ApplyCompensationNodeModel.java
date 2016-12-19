package io.landysh.inflor.main.knime.nodes.compensate.apply;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import io.landysh.inflor.main.core.compensation.SpilloverCompensator;
import io.landysh.inflor.main.core.data.FCSFrame;
import io.landysh.inflor.main.core.utils.FCSConcatenator;
import io.landysh.inflor.main.core.utils.FCSUtilities;
import io.landysh.inflor.main.knime.core.NodeUtilities;
import io.landysh.inflor.main.knime.dataTypes.FCSFrameCell.FCSFrameCellColumnFilter;
import io.landysh.inflor.main.knime.dataTypes.FCSFrameCell.FCSFrameDataValue;
import io.landysh.inflor.main.knime.dataTypes.FCSFrameCell.FCSFrameFileStoreDataCell;
import io.landysh.inflor.main.knime.ports.compensation.CompMatrixPortObject;
import io.landysh.inflor.main.knime.ports.compensation.CompMatrixPortSpec;

/**
 * This is the model implementation of ApplyCompensation.
 * Attempts to apply a supplied compensation matrix to a dataset.  
 *
 * @author Aaron Hart
 */
public class ApplyCompensationNodeModel extends NodeModel {
    
  private static final NodeLogger logger = NodeLogger.getLogger(ApplyCompensationNodeModel.class);
  
  public static final String KEY_SELECTED_COLUMN = "Selected Column";
  public static final String KEY_RETAIN_UNCOMPED = "Retain Uncomped Dimensions";

  public static final boolean DEFAULT_RETAIN_UNCOMPED = false;
  public static final String DEFAULT_SELECTED_COLUMN = "Select...";
  
  private SettingsModelColumnName mSelectedColumn=  new SettingsModelColumnName(KEY_SELECTED_COLUMN, DEFAULT_SELECTED_COLUMN);
  private SettingsModelBoolean    mRetainUncomped = new SettingsModelBoolean(KEY_RETAIN_UNCOMPED, DEFAULT_RETAIN_UNCOMPED);
  
    /**
     * Constructor for the node model.
     */
    protected ApplyCompensationNodeModel() {
    
      super(new PortType[] {PortTypeRegistry.getInstance().getPortType(CompMatrixPortObject.class),
                            PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class)},
            
            new PortType[] {PortTypeRegistry.getInstance().getPortType(BufferedDataTable.class)});
    }

    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    
    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws IOException{
        
      logger.info("Beginning Execution.");
      final FileStoreFactory fileStoreFactory = FileStoreFactory.createWorkflowFileStoreFactory(exec);

      // Create the output spec and data container.
      DataTableSpec outSpec = createSpec(new PortObjectSpec[]{inObjects[0].getSpec(), inObjects[1].getSpec()});
      BufferedDataContainer container = exec.createDataContainer(outSpec);
      String columnName = mSelectedColumn.getColumnName();
      int index = outSpec.findColumnIndex(columnName);
      CompMatrixPortObject cmpo = (CompMatrixPortObject) inObjects[0];
      CompMatrixPortSpec cmSpec = (CompMatrixPortSpec) cmpo.getSpec();
      SpilloverCompensator compr = new SpilloverCompensator(cmSpec.getInputDimensions(), cmSpec.getOutputDimensions(),cmpo.getSpilloverValues());
      
      BufferedDataTable inTable = (BufferedDataTable) inObjects[1];
      int i = 0;
      for (final DataRow inRow : inTable) {

        DataCell[] outCells = new DataCell[inRow.getNumCells()];
        FCSFrame columnStore = ((FCSFrameFileStoreDataCell) inRow.getCell(index)).getFCSFrameValue();

        // now create the output row
        FCSFrame outStore = compr.compensateFCSFrame(columnStore, mRetainUncomped.getBooleanValue());
        String fsName = i + "ColumnStore.fs";
        FileStore fileStore = fileStoreFactory.createFileStore(fsName);
        FCSFrameFileStoreDataCell fileCell = new FCSFrameFileStoreDataCell(fileStore, outStore);

        for (int j = 0; j < outCells.length; j++) {
          if (j == index) {
            outCells[j] = fileCell;
          } else {
            outCells[j] = inRow.getCell(j);
          }
        }
        DataRow outRow = new DefaultRow("Row " + i, outCells);
        container.addRowToTable(outRow);
        i++;
      }
      container.close();
      return new BufferedDataTable[] {container.getTable()};
    }

    private DataTableSpec createSpec(PortObjectSpec[] inSpecs) {
      CompMatrixPortSpec compMatrixSpec = (CompMatrixPortSpec) inSpecs[0];
      DataTableSpec dataTableSpec = (DataTableSpec) inSpecs[1];
      String columnName = mSelectedColumn.getColumnName();
      DataColumnSpec selectedColSpec = dataTableSpec.getColumnSpec(columnName);
      DataColumnProperties properties = selectedColSpec.getProperties();
      String dimensionNameString = properties.getProperty(NodeUtilities.DIMENSION_NAMES_KEY);
      String[] dimensionNames = dimensionNameString.split(NodeUtilities.DELIMITER_REGEX);
      String[] updatedNames = updateDimensionNames(dimensionNames, compMatrixSpec);
      String combinedNames = String.join(NodeUtilities.DELIMITER, updatedNames);
      
      HashMap<String, String> newColumnNames = new HashMap<>();
      newColumnNames.put(NodeUtilities.DIMENSION_NAMES_KEY, combinedNames);
      DataColumnProperties newProps = properties.cloneAndOverwrite(newColumnNames);
      
      DataColumnSpecCreator creator = new DataColumnSpecCreator(columnName, FCSFrameFileStoreDataCell.TYPE);
      creator.setProperties(newProps);
      DataColumnSpec newSpec = creator.createSpec();
      DataColumnSpec[] colSpecs = new DataColumnSpec[dataTableSpec.getColumnNames().length];
      for (int i=0;i<colSpecs.length;i++){
        DataColumnSpec currentSpec = dataTableSpec.getColumnSpec(i);
        if (currentSpec.getName().equals(columnName)){
          colSpecs[i] = newSpec;
        } else {
          colSpecs[i] = dataTableSpec.getColumnSpec(i);
        }
      }
      return new DataTableSpec(colSpecs);
    }
    
    private String[] updateDimensionNames(String[] dimensionNames, CompMatrixPortSpec compMatrixSpec) {
      String[] newNames;
      String[] outDimensions = compMatrixSpec.getOutputDimensions();
      if (mRetainUncomped.getBooleanValue()){
        newNames = new String[dimensionNames.length+outDimensions.length];
        for (int i=0;i<dimensionNames.length;i++){
          newNames[i] = dimensionNames[i];
        }
        for (int j=0;j<outDimensions.length;j++){
          newNames[dimensionNames.length+j] = outDimensions[j];
        }
      } else {
        newNames = dimensionNames.clone();
      }
      for(int i=0;i<newNames.length;i++){
        for (int j=0;j<outDimensions.length;j++){
          String[] inDimensions = compMatrixSpec.getInputDimensions();
          if (newNames[i].equals(inDimensions[j])){
            newNames[i] = FCSUtilities.formatCompStainName(newNames[i]);
          }
        }
      }
      return newNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {}

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
      //identify any compatible FCS Columns
      if (mSelectedColumn.getColumnName().equals(DEFAULT_SELECTED_COLUMN)){
        DataTableSpec dataTableSpec = (DataTableSpec) inSpecs[1];
        dataTableSpec.containsCompatibleType(FCSFrameDataValue.class);
        for (int i=0;i<dataTableSpec.getNumColumns();i++){
          if (dataTableSpec.getColumnSpec(i).getType().equals(FCSFrameFileStoreDataCell.TYPE)){
            mSelectedColumn.setSelection(dataTableSpec.getColumnSpec(i).getName(), false);
            break;
          }
        }
      }
      return new PortObjectSpec[]{createSpec(inSpecs)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         mRetainUncomped.saveSettingsTo(settings);
         mSelectedColumn.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {    
      mRetainUncomped.loadSettingsFrom(settings);
      mSelectedColumn.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {}
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {    }

}
