/*******************************************************************************
 * Copyright (c) 2011 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.datashield;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.obiba.opal.datashield.DataShieldLog;
import org.obiba.opal.datashield.RestrictedRScriptROperation;
import org.obiba.opal.datashield.expr.ParseException;
import org.obiba.opal.r.service.OpalRSession;
import org.obiba.opal.web.r.RSymbolResource;

public class DataShieldSymbolResource extends RSymbolResource {

  // TODO: Extract from configuration
  private boolean restricted = false;

  public DataShieldSymbolResource(OpalRSession rSession, String name) {
    super(rSession, name);
  }

  @Override
  public Response putMagma(UriInfo uri, String path) {
    DataShieldLog.userLog("creating symbol '{}' from opal data '{}'", getName(), path);
    return super.putMagma(uri, path);
  }

  @Override
  public Response putRScript(UriInfo uri, String script) {
    DataShieldLog.userLog("creating symbol '{}' from R script '{}'", getName(), script);
    if(restricted) {
      return putRestrictedRScript(uri, script);
    }
    return super.putRScript(uri, script);
  }

  @Override
  public Response putString(UriInfo uri, String content) {
    DataShieldLog.userLog("creating text symbol '{}' as '{}'", getName(), content);
    return super.putString(uri, content);
  }

  @Override
  public Response rm() {
    DataShieldLog.userLog("deleting symbol '{}'", getName());
    return super.rm();
  }

  /**
   * Overridden to prevent accessing individual-level data
   */
  @Override
  public Response getSymbol() {
    return Response.noContent().build();
  }

  protected Response putRestrictedRScript(UriInfo uri, String content) {
    try {
      getRSession().execute(new RestrictedRScriptROperation(getName(), content));
      return Response.created(getSymbolURI(uri)).build();
    } catch(ParseException e) {
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
  }
}
