/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.service.impl;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.obiba.magma.Datasource;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.NoSuchDatasourceException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.ValueTable;
import org.obiba.magma.audit.hibernate.HibernateVariableEntityAuditLogManager;
import org.obiba.magma.support.MagmaEngineReferenceResolver;
import org.obiba.opal.core.service.ExportService;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ExportService}.
 */
public class DefaultExportServiceImpl implements ExportService {

  private HibernateVariableEntityAuditLogManager auditLogManager;

  public void setAuditLogManager(HibernateVariableEntityAuditLogManager auditLogManager) {
    this.auditLogManager = auditLogManager;
  }

  public void exportTablesToDatasource(List<String> fromTableNames, String destinationDatasourceName) {
    Assert.notEmpty(fromTableNames, "fromTableNames must not be null or empty");
    Assert.hasText(destinationDatasourceName, "destinationDatasourceName must not be null or empty");
    Datasource destinationDatasource = getDatasourceByName(destinationDatasourceName);
    Set<ValueTable> sourceTables = getValueTablesByName(fromTableNames);
    validateSourceDatasourceNotEqualDestinationDatasource(sourceTables, destinationDatasource);
    System.out.println("There are " + sourceTables.size() + " tables");
    throw new UnsupportedOperationException("Exporting to an existing datasource destination is not currently supported.");
  }

  public void exportTablesToExcelFile(List<String> fromTableNames, File destinationExcelFile) {
    Assert.notEmpty(fromTableNames, "fromTableNames must not be null or empty");
    Assert.notNull(destinationExcelFile, "destinationExcelFile must not be null");
    // Create ExcelDatasource
    // Call exportTablesToDatasource with the ExcelDatasource
    throw new UnsupportedOperationException("Exporting to an Excel file is not currently supported.");
  }

  private Datasource getDatasourceByName(String datasourceName) {
    Datasource datasource = MagmaEngine.get().getDatasource(datasourceName);

    if(datasource == null) {
      throw new NoSuchDatasourceException("No such datasource '" + datasourceName + "'.");
    }
    return datasource;
  }

  private Set<ValueTable> getValueTablesByName(List<String> tableNames) throws NoSuchDatasourceException, NoSuchValueTableException, ExportException {
    Set<ValueTable> tables = new HashSet<ValueTable>();
    for(String tableName : tableNames) {
      // Resolver expects ValueTable names to be delimited with a colon.
      if(!tables.add(MagmaEngineReferenceResolver.valueOf(tableName + ":").resolveTable())) {
        throw new ExportException("Source tables include duplicate '" + tableName + "'.");
      }
    }
    return tables;
  }

  private void validateSourceDatasourceNotEqualDestinationDatasource(Set<ValueTable> sourceTables, Datasource destinationDatasource) {
    for(ValueTable sourceTable : sourceTables) {
      if(sourceTable.getDatasource().equals(destinationDatasource)) {
        throw new ExportException("Cannot export when datasource of source table '" + sourceTable.getDatasource().getName() + "." + sourceTable.getName() + "' matches the destintation datasource '" + destinationDatasource.getName() + "'.");
      }
    }
  }

  // private void copyValueTables(Set<ValueTable> sourceTables, Datasource destination, String owner) throws IOException
  // {
  // DatasourceCopier copier =
  // DatasourceCopier.Builder.newCopier().dontCopyNullValues().withLoggingListener().withVariableEntityCopyEventListener(auditLogManager,
  // source, destination).build();
  //
  // for(ValueTable valueTable : sourceTables) {
  // copier.copy(valueTable, destination);
  // }
  // }

  private class ExportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ExportException(String string) {
      super(string);
    }

  }

}
