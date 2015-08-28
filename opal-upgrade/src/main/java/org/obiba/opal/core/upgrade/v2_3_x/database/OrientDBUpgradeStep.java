/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.core.upgrade.v2_3_x.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.obiba.opal.core.service.LocalOrientDbServerFactory;
import org.obiba.opal.core.service.OrientDbServerFactory;
import org.obiba.opal.core.service.OrientDbService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.AbstractUpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Throwables;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

@SuppressWarnings("SpringJavaAutowiringInspection")
public class OrientDBUpgradeStep extends AbstractUpgradeStep {

  private static final Logger log = LoggerFactory.getLogger(OrientDBUpgradeStep.class);

  @Autowired
  OrientDbService orientDbService;

  @Autowired
  OrientDbServerFactory factory;

  @Override
  public void execute(Version currentVersion) {
    log.info("Upgrading orientdb");
    String source = factory.getUrl().replace("plocal:", "");
    String dist = System.getenv("OPAL_DIST");

    try {
      //factory.getServer().shutdown();

      File exportFile = File.createTempFile("opal_orientdb_export", null);
      ProcessBuilder pb = new ProcessBuilder("bash", "-c", String.format("java -jar opal-config-migrator-*-cli.jar %s %s", source,
          exportFile.getAbsolutePath()));
      pb.redirectErrorStream(true);
      pb.directory(Paths.get(dist, "tools", "lib").toFile());
      Process p = pb.start();
      p.waitFor();

      BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      StringBuilder stringBuilder = new StringBuilder();

      while ((line = br.readLine()) != null) {
        stringBuilder.append(line);
      }

      log.info("Export completed to " + stringBuilder.toString());

      if(p.exitValue() == 0) {
        Files.move(Paths.get(source), Paths.get(String.format("%s.bak", source)), ATOMIC_MOVE);
      } else {
        throw new RuntimeException("Error upgrading orientdb" );
      }

      orientDbService.importDatabase(Paths.get(exportFile.getAbsolutePath() + ".gz").toFile());
      log.info("Upgraded orientdb successfully");
    } catch(Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
