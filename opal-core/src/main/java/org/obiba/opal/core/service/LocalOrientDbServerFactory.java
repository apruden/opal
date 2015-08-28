package org.obiba.opal.core.service;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;

@Component
public class LocalOrientDbServerFactory implements OrientDbServerFactory {

  private static final Logger log = LoggerFactory.getLogger(LocalOrientDbServerFactory.class);

  private static final String ORIENTDB_HOME = "${OPAL_HOME}/data/orientdb/opal-config";

  public static final String DEFAULT_SCHEME = "plocal";

  public static final String USERNAME = "admin";

  public static final String PASSWORD = "admin";

  public static final String URL = DEFAULT_SCHEME + ":" + ORIENTDB_HOME;

  private String url;

  private OServer server;

  private OPartitionedDatabasePoolFactory poolFactory;

  // TODO: wait for this issue to be fixed to change admin password
  // https://github.com/orientechnologies/orientdb/pull/1870

//  private OpalConfigurationService opalConfigurationService;
//
//  @Autowired
//  public void setOpalConfigurationService(OpalConfigurationService opalConfigurationService) {
//    this.opalConfigurationService = opalConfigurationService;
//  }

  @Value(URL)
  @Override
  public void setUrl(@NotNull String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public synchronized void init() {
    log.info("Start OrientDB server ({})", url);

    System.setProperty("ORIENTDB_HOME", url.replaceFirst("^" + DEFAULT_SCHEME + ":", ""));
    System.setProperty("ORIENTDB_ROOT_PASSWORD", PASSWORD);

    try {
      server = new OServer() //
          .startup(LocalOrientDbServerFactory.class.getResourceAsStream("/orientdb-server-config.xml")) //
          .activate();
    } catch(Exception e) {
      throw Throwables.propagate(e);
    }

    poolFactory = new OPartitionedDatabasePoolFactory();

    ensureDatabaseExists();
  }

  @PreDestroy
  public void stop() {
    log.info("Stop OrientDB server ({})", url);
    if(server != null) server.shutdown();
  }

  @NotNull
  @Override
  public OServer getServer() {
    if(server == null && !server.isActive()) init();

    return server;
  }

  @NotNull
  @Override
  public ODatabaseDocumentTx getDocumentTx() {
    //TODO cache password
//    String password = opalConfigurationService.getOpalConfiguration().getDatabasePassword();
    log.trace("Open OrientDB connection with username: {}", USERNAME);

    if(poolFactory == null) init();

    return poolFactory.get(url, USERNAME, PASSWORD).acquire();
  }

  private void ensureDatabaseExists() {
    try(ODatabaseDocumentTx database = new ODatabaseDocumentTx(url)) {
      if(!database.exists()) {
        log.info("Creating OrientDB database {}", getUrl());
        database.create();
      }
    }
  }
}
