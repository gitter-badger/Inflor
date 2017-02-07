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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromInputStream;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.FileUtil;

public class FCSFrameCellFactory extends AbstractCellFactory implements FromInputStream {

  private final FileStoreFactory mfileStoreFactory;

  public FCSFrameCellFactory() {
    mfileStoreFactory = FileStoreFactory.createNotInWorkflowFileStoreFactory();
  }

  public FCSFrameCellFactory(ExecutionContext exec) {
    mfileStoreFactory = FileStoreFactory.createWorkflowFileStoreFactory(exec);
  }

  @Override
  public DataCell createCell(InputStream input) throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    FileUtil.copy(input, output);
    output.close();
    final byte[] buffer = output.toByteArray();
    // Create the file store.
    final FileStore fs = mfileStoreFactory.createFileStore("column.store");
    return new FCSFrameContent(buffer).toColumnStoreCell(fs);
  }

  @Override
  public DataType getDataType() {
    return FCSFrameFileStoreDataCell.TYPE;
  }

  @Override
  public DataCell[] getCells(DataRow row) {
    // TODO Auto-generated method stub
    return null;
  }
}
