/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.web.shell.reporting;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.opal.core.cfg.ReportTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope("request")
@Path("/project/{project}/report-template/{name}")
public class ProjectReportTemplateResource extends ReportTemplateResource {

  @PathParam("project")
  private String project;

  @PathParam("name")
  private String name;

  public ProjectReportTemplateResource() {
    setName(name);
  }

  @Override
  protected boolean authzReadReportTemplate(ReportTemplate template) {
    return template.hasProject() && template.getProject().equals(name) &&
        getAuthorizer().isPermitted("magma:/project/" + name + "/report-template/" + template.getName() + ":GET");
  }

}
