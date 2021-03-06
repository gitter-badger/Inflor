/*
 * ------------------------------------------------------------------------
 *  Copyright by Aaron Hart
 *  Email: Aaron.Hart@gmail.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 * ---------------------------------------------------------------------
 *
 * Created on December 13, 2016 by Aaron Hart
 */
package main.java.inflor.knime.data.type.cell.fcs;

import java.io.FileInputStream;
import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.node.NodeLogger;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.utils.FCSUtilities;
import main.java.inflor.knime.core.NodeUtilities;

public class FCSFrameFileStoreDataCell extends FileStoreCell implements FCSFrameDataValue  {

  // the logger instance
  private static final NodeLogger logger = NodeLogger.getLogger(FCSFrameFileStoreDataCell.class);
  
  public static final class Serializer implements DataCellSerializer<FCSFrameFileStoreDataCell> {

    @Override
    public FCSFrameFileStoreDataCell deserialize(DataCellDataInput input) throws IOException {
      try {
        
        String id = input.readUTF();
        String displayName = input.readUTF();
        String[] dimensionKeys = input.readUTF().split(FCSUtilities.DELIMITER_REGEX);
        String[] dimensionLabels = input.readUTF().split(FCSUtilities.DELIMITER_REGEX);
        String description = input.readUTF();
        int messageSize = input.readInt();
        int rowCount = input.readInt();

        FCSFrameMetaData newMetadata = new FCSFrameMetaData(id, displayName, dimensionKeys, dimensionLabels, description, messageSize, rowCount);
                    
        return new FCSFrameFileStoreDataCell(newMetadata);
      } catch (Exception e) {
        logger.error("Unable to deserialize cell", e);
        throw new IOException("Error during deserialization");
      }
    }

    @Override
    public void serialize(FCSFrameFileStoreDataCell cell, DataCellDataOutput output) throws IOException {
      
      FCSFrameMetaData md = cell.getFCSFrameMetadata();
      String id = md.getID();
      String displayName = md.getDisplayName();
      String dimesnionKeys = String.join(FCSUtilities.DELIMITER, md.getDimensionNames());
      String dimensionLabels = String.join(NodeUtilities.DELIMITER, md.getDimensionLabels());
      String description = md.getMultilineDescription();
      output.writeUTF(id);
      output.writeUTF(displayName);
      output.writeUTF(dimesnionKeys);
      output.writeUTF(dimensionLabels);
      output.writeUTF(description);
      output.writeInt(md.getSize());
      output.writeInt(md.getRowCount());

    }
  }

  private static final long serialVersionUID = 1L;
  public static final DataType TYPE = DataType.getType(FCSFrameFileStoreDataCell.class, FCSFrameFileStoreDataCell.TYPE);
  /**
   * A cell type matching the functionality of the ColumnStorePortObject.
   */
  private final FCSFrameMetaData metaData;

  FCSFrameFileStoreDataCell(FCSFrameMetaData metaData) {
    
    // Use with deserializer.
    super();
    this.metaData = metaData;
  }

  public FCSFrameFileStoreDataCell(FileStore fs, FCSFrameMetaData metaData) {
    super(fs);
    this.metaData = metaData;
  }

  public FCSFrameFileStoreDataCell(FileStore fileStore, FCSFrame dataFrame, int messageSize) {
    this(fileStore, new FCSFrameMetaData(dataFrame, messageSize));
  }

  @Override
  public String toString() {
    return metaData.toString();
  }

  @Override
  public FCSFrameMetaData getFCSFrameMetadata() {
    return metaData;
  }
  
  @Override
  public FileStore getFileStore() {
    return super.getFileStore();
  }

  public FCSFrame getFCSFrameValue() throws IOException {
      int size = metaData.getSize();//currently 2gb max.
      byte[] bytes = new byte[size];
      FileInputStream fis = new FileInputStream(super.getFileStore().getFile());
      fis.read(bytes);
      fis.close();
      return FCSFrame.load(bytes);
  }
}
