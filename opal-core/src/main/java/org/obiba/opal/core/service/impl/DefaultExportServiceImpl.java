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
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import org.obiba.magma.Datasource;
import org.obiba.magma.MagmaEngine;
import org.obiba.magma.NoSuchDatasourceException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.ValueTable;
import org.obiba.magma.VariableEntity;
import org.obiba.magma.audit.VariableEntityAuditLogManager;
import org.obiba.magma.datasource.excel.ExcelDatasource;
import org.obiba.magma.support.DatasourceCopier;
import org.obiba.magma.support.MagmaEngineTableResolver;
import org.obiba.magma.support.MultithreadedDatasourceCopier;
import org.obiba.magma.support.DatasourceCopier.Builder;
import org.obiba.magma.views.IncrementalWhereClause;
import org.obiba.magma.views.View;
import org.obiba.opal.core.magma.FunctionalUnitView;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.obiba.opal.core.service.ExportException;
import org.obiba.opal.core.service.ExportService;
import org.obiba.opal.core.service.NoSuchFunctionalUnitException;
import org.obiba.opal.core.unit.FunctionalUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.google.common.base.Function;

/**
 * Default implementation of {@link ExportService}.
 */
@Transactional
public class DefaultExportServiceImpl implements ExportService {

  private final PlatformTransactionManager txManager;

  private final OpalRuntime opalRuntime;

  private final VariableEntityAuditLogManager auditLogManager;

  /** Configured through org.obiba.opal.keys.tableReference */
  private final String keysTableReference;

  /** Configured through org.obiba.opal.keys.entityType */
  private final String keysTableEntityType;

  @Autowired
  public DefaultExportServiceImpl(PlatformTransactionManager txManager, OpalRuntime opalRuntime, VariableEntityAuditLogManager auditLogManager, @Value("${org.obiba.opal.keys.tableReference}") String keysTableReference, @Value("${org.obiba.opal.keys.entityType}") String keysTableEntityType) {
    if(txManager == null) throw new IllegalArgumentException("txManager cannot be null");
    if(opalRuntime == null) throw new IllegalArgumentException("opalRuntime cannot be null");
    if(auditLogManager == null) throw new IllegalArgumentException("auditLogManager cannot be null");
    if(keysTableReference == null) throw new IllegalArgumentException("keysTableReference cannot be null");
    if(keysTableEntityType == null) throw new IllegalArgumentException("keysTableEntityType cannot be null");

    this.txManager = txManager;
    this.opalRuntime = opalRuntime;
    this.auditLogManager = auditLogManager;
    this.keysTableReference = keysTableReference;
    this.keysTableEntityType = keysTableEntityType;
  }

  public Builder newCopier(Datasource destinationDatasource, Function<VariableEntity, VariableEntity> entityMapper) {
    return DatasourceCopier.Builder.newCopier().withLoggingListener().withThroughtputListener().withVariableEntityCopyEventListener(auditLogManager, destinationDatasource, entityMapper);
  }

  public Builder newCopier(Datasource destinationDatasource) {
    return newCopier(destinationDatasource, null);
  }

  public void exportTablesToDatasource(String unitName, List<String> sourceTableNames, String destinationDatasourceName, boolean incremental) {
    Assert.notEmpty(sourceTableNames, "sourceTableNames must not be null or empty");
    Assert.hasText(destinationDatasourceName, "destinationDatasourceName must not be null or empty");
    Datasource destinationDatasource = MagmaEngine.get().getDatasource(destinationDatasourceName);
    Set<ValueTable> sourceTables = getValueTablesByName(sourceTableNames);
    DatasourceCopier datasourceCopier = newCopier(destinationDatasource).build();
    exportTablesToDatasource(unitName, sourceTables, destinationDatasource, datasourceCopier, incremental);
  }

  public void exportTablesToDatasource(String unitName, List<String> sourceTableNames, String destinationDatasourceName, DatasourceCopier datasourceCopier, boolean incremental) {
    Assert.notEmpty(sourceTableNames, "sourceTableNames must not be null or empty");
    Assert.hasText(destinationDatasourceName, "destinationDatasourceName must not be null or empty");
    Datasource destinationDatasource = MagmaEngine.get().getDatasource(destinationDatasourceName);
    Set<ValueTable> sourceTables = getValueTablesByName(sourceTableNames);
    exportTablesToDatasource(unitName, sourceTables, destinationDatasource, datasourceCopier, incremental);
  }

  public void exportTablesToDatasource(String unitName, Set<ValueTable> sourceTables, Datasource destinationDatasource, DatasourceCopier datasourceCopier, boolean incremental) {
    Assert.notEmpty(sourceTables, "sourceTables must not be null or empty");
    Assert.notNull(destinationDatasource, "destinationDatasource must not be null");
    Assert.notNull(datasourceCopier, "datasourceCopier must not be null");

    FunctionalUnit unit = null;
    if(unitName != null) {
      unit = validateFunctionalUnit(unitName);
    }

    validateSourceDatasourceNotEqualDestinationDatasource(sourceTables, destinationDatasource);

    try {
      for(ValueTable table : sourceTables) {
        // If the incremental option was specified, create an incremental view of the table (leaving out what has
        // already been exported).
        table = incremental ? getIncrementalView(table, destinationDatasource) : table;

        // If the table contains an entity that requires key separation, create a "unit view" of the table (replace
        // public identifiers with private, unit-specific identifiers).
        // Also, replace the copier with one that persists the "public" identifiers in the audit log.
        if((unit != null) && table.isForEntityType(keysTableEntityType)) {
          FunctionalUnitView unitView = getUnitView(unit, table);
          table = unitView;
          datasourceCopier = newCopier(destinationDatasource, unitView.getVariableEntityReverseTransformer()).build();
        }

        // Go ahead and copy the result to the destination datasource.
        MultithreadedDatasourceCopier.Builder.newCopier().from(table).to(destinationDatasource).withCopier(DatasourceCopier.Builder.newCopier(datasourceCopier)).withReaders(4).withThreads(new ThreadFactory() {

          @Override
          public Thread newThread(Runnable r) {
            return new TransactionalThread(txManager, r);
          }
        }).build().copy();
      }
    } catch(IOException ex) {
      // When implementing the ExcelDatasource:
      // Determine if this the ExcelDatasource. If yes then display the filename.
      throw new ExportException("An error was encountered while exporting to datasource '" + destinationDatasource + "'.", ex);
    }
  }

  public void exportTablesToExcelFile(String unitName, List<String> sourceTableNames, File destinationExcelFile, boolean incremental) {
    Assert.notEmpty(sourceTableNames, "sourceTableNames must not be null or empty");
    Assert.notNull(destinationExcelFile, "destinationExcelFile must not be null");

    Datasource outputDatasource = buildExcelDatasource(destinationExcelFile);
    try {
      // Create a DatasourceCopier that will copy only the metadata and export.
      exportTablesToDatasource(unitName, sourceTableNames, outputDatasource.getName(), incremental);
    } finally {
      outputDatasource.dispose();
    }

  }

  public void exportTablesToExcelFile(String unitName, List<String> sourceTableNames, File destinationExcelFile, DatasourceCopier datasourceCopier, boolean incremental) {
    Assert.notEmpty(sourceTableNames, "sourceTableNames must not be null or empty");
    Assert.notNull(destinationExcelFile, "destinationExcelFile must not be null");

    Datasource outputDatasource = buildExcelDatasource(destinationExcelFile);
    try {
      // Create a DatasourceCopier that will copy only the metadata and export.
      exportTablesToDatasource(unitName, sourceTableNames, outputDatasource.getName(), datasourceCopier, incremental);
    } finally {
      outputDatasource.dispose();
    }

  }

  private FunctionalUnitView getUnitView(FunctionalUnit unit, ValueTable valueTable) {
    return new FunctionalUnitView(unit, valueTable, lookupKeysTable());
  }

  private ValueTable lookupKeysTable() {
    return MagmaEngineTableResolver.valueOf(keysTableReference).resolveTable();
  }

  private Datasource buildExcelDatasource(File destinationExcelFile) {
    Datasource outputDatasource = new ExcelDatasource(destinationExcelFile.getName(), destinationExcelFile);
    outputDatasource.initialise();
    return outputDatasource;
  }

  private Set<ValueTable> getValueTablesByName(List<String> tableNames) throws NoSuchDatasourceException, NoSuchValueTableException, ExportException {
    Set<ValueTable> tables = new HashSet<ValueTable>();
    for(String tableName : tableNames) {
      try {
        if(!tables.add(MagmaEngineTableResolver.valueOf(tableName).resolveTable())) {
          throw new ExportException("Source tables include duplicate '" + tableName + "'.");
        }
      } catch(IllegalArgumentException e) {
        throw new ExportException("Source table '" + tableName + "' does not exist.");
      }
    }
    return tables;
  }

  private FunctionalUnit validateFunctionalUnit(String unitName) {
    FunctionalUnit unit = opalRuntime.getFunctionalUnit(unitName);
    if(unit == null) {
      throw new NoSuchFunctionalUnitException(unitName);
    }
    return unit;
  }

  private void validateSourceDatasourceNotEqualDestinationDatasource(Set<ValueTable> sourceTables, Datasource destinationDatasource) {
    for(ValueTable sourceTable : sourceTables) {
      if(sourceTable.getDatasource().equals(destinationDatasource)) {
        throw new ExportException("Cannot export when datasource of source table '" + sourceTable.getDatasource().getName() + "." + sourceTable.getName() + "' matches the destintation datasource '" + destinationDatasource.getName() + "'.");
      }
    }
  }

  private View getIncrementalView(ValueTable valueTable, Datasource destination) {
    IncrementalWhereClause whereClause = new IncrementalWhereClause(destination.getName() + "." + valueTable.getName());
    whereClause.setAuditLogManager(auditLogManager);

    // Cache the where clause as it is quite expensive. This is ok since the incremental view is transient: used this
    // time only then thrown away.
    return View.Builder.newView(valueTable.getName(), valueTable).where(whereClause).cacheWhere().build();
  }

  static class TransactionalThread extends Thread {

    private PlatformTransactionManager txManager;

    private Runnable runnable;

    public TransactionalThread(PlatformTransactionManager txManager, Runnable runnable) {
      this.txManager = txManager;
      this.runnable = runnable;
    }

    public void run() {
      DefaultTransactionDefinition txdef = new DefaultTransactionDefinition();
      txdef.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
      new TransactionTemplate(txManager, txdef).execute(new TransactionCallbackWithoutResult() {
        @Override
        protected void doInTransactionWithoutResult(TransactionStatus status) {
          runnable.run();
        }
      });
    }
  }
}
