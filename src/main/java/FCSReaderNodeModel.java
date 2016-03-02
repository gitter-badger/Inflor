package main.java;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the node model implementation for FCSReader (rows). It is designed to use the Inflor 
 * FCSFileReader in the context of a KNIME Source node which produces a standard KNIME data table.
 * @author Aaron Hart
 */
public class FCSReaderNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger.getLogger(FCSReaderNodeModel.class);

	/**
	 * the settings key which is used to retrieve and store the settings (from
	 * the dialog or from a settings file) (package visibility to be usable from
	 * the dialog).
	 */
	static final String CFGKEY_FileLocation = "File Location";

	/** initial default count value. */
	static final String DEFAULT_FileLocation = "";

	// example value: the models count variable filled from the dialog
	// and used in the models execution method. The default components of the
	// dialog work with "SettingsModels".
	private final SettingsModelString m_FileLocation = new SettingsModelString(CFGKEY_FileLocation,
			DEFAULT_FileLocation);

	static FCSFileReader FCS_READER;

	/**
	 * Constructor for the node model.
	 */
	protected FCSReaderNodeModel() {

		// Top port contains header information, bottom, array data
		super(0, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			{

		logger.info("Starting Execution");
		// get table specs
		FCSFileReader FCSReader;
		BufferedDataContainer header = null;
		BufferedDataContainer data = null;
		try {
			FCSReader = new FCSFileReader(m_FileLocation.getStringValue());
			EventFrame frame = FCSReader.getEventFrame();
			Hashtable <String, String> keywords = FCSReader.getHeader();
			DataTableSpec[] tableSpecs = createPortSpecs(frame);
			// Read header section
			header = exec.createDataContainer(tableSpecs[0]);
			Enumeration<String> enumKey = keywords.keys();
			int i = 0;
			while (enumKey.hasMoreElements()) {
				String key = enumKey.nextElement();
				String val = keywords.get(key);
				RowKey rowKey = new RowKey("Row " + i);
				// the cells of the current row, the types of the cells must match
				// the column spec (see above)
				DataCell[] keywordCells = new DataCell[2];
				keywordCells[0] = new StringCell(key);
				keywordCells[1] = new StringCell(val);
				DataRow keywordRow = new DefaultRow(rowKey, keywordCells);
				header.addRowToTable(keywordRow);
				i++;
				if (key.equals("0") && val.equals("0"))
					keywords.remove(key);
			}
			header.close();

			// a quick breath before we move on.
			exec.checkCanceled();
			exec.setProgress(0.01, "Header read.");

			// Read data section
			data = exec.createDataContainer(tableSpecs[1]);
			FCSReader.initRowReader();
			for (int j = 0; j <= frame.eventCount - 1; j++) {
				RowKey rowKey = new RowKey("Row " + j);
				DataCell[] dataCells = new DataCell[frame.parameterCount];
				double[] FCSRow = FCSReader.readRow();
				for (int k = 0; k <= frame.parameterCount - 1; k++) {
					dataCells[k] = new DoubleCell(FCSRow[k]);
				}
				DataRow dataRow = new DefaultRow(rowKey, dataCells);
				data.addRowToTable(dataRow);
				if (j % 1000 == 0) {
					exec.checkCanceled();
					exec.setProgress(j / frame.eventCount, j + " rows read.");
				}
			}
			// once we are done, we close the container and return its table
			data.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new BufferedDataTable[] { header.getTable(), data.getTable() };
	}

	private DataTableSpec[] createPortSpecs(EventFrame frame) {
		DataTableSpec[] specs = new DataTableSpec[2];
		specs[0] = createKeywordSpec();
		specs[1] = createDataSpec(frame);
		return specs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// TODO: check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message
		DataTableSpec[] specs = null;
		try {
			FCSFileReader FCSReader = new FCSFileReader(m_FileLocation.getStringValue());
			EventFrame eventsFrame = FCSReader.getEventFrame();
			specs = createPortSpecs(eventsFrame);
			FCSReader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return specs;
	}

	private DataTableSpec createDataSpec(EventFrame frame) {
		int parCount = frame.parameterCount;
		DataColumnSpec[] colSpecs = new DataColumnSpec[parCount];
		String[] columnNames = frame.getColumnNames();
		for (int i=0; i<columnNames.length; i++) {
			colSpecs[i] = new DataColumnSpecCreator(columnNames[i], DoubleCell.TYPE).createSpec();
		}
		DataTableSpec dataSpec = new DataTableSpec(colSpecs);
		return dataSpec;
	}

	private DataTableSpec createKeywordSpec() {
		DataColumnSpec[] colSpecs = new DataColumnSpec[2];
		colSpecs[0] = new DataColumnSpecCreator("keyword", StringCell.TYPE).createSpec();
		colSpecs[1] = new DataColumnSpecCreator("value", StringCell.TYPE).createSpec();

		DataTableSpec headerSpec = new DataTableSpec(colSpecs);
		return headerSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		// TODO save user settings to the config object.

		m_FileLocation.saveSettingsTo(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		// TODO load (valid) settings from the config object.
		// It can be safely assumed that the settings are valided by the
		// method below.

		m_FileLocation.loadSettingsFrom(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		// TODO check if the settings could be applied to our model
		// e.g. if the count is in a certain range (which is ensured by the
		// SettingsModel).
		// Do not actually set any values of any member variables.

		m_FileLocation.validateSettings(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

		// TODO load internal data.
		// Everything handed to output ports is loaded automatically (data
		// returned by the execute method, models loaded in loadModelContent,
		// and user settings set through loadSettingsFrom - is all taken care
		// of). Load here only the other internals that need to be restored
		// (e.g. data used by the views).

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {

		// TODO save internal models.
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).

	}

}