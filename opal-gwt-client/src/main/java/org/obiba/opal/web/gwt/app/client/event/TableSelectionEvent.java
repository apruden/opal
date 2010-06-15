/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.event;

import org.obiba.opal.web.model.client.TableDto;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event to indicate that a Magma Table has been selected.
 */
public class TableSelectionEvent extends GwtEvent<TableSelectionEvent.Handler> {

  public interface Handler extends EventHandler {

    void onNavigatorSelectionChanged(TableSelectionEvent event);

  }

  private static Type<Handler> TYPE;

  private final TableDto tableDto;

  /**
   * @param selectedItem
   */
  public TableSelectionEvent(TableDto tableDto) {
    this.tableDto = tableDto;
  }

  public TableDto getSelection() {
    return tableDto;
  }

  public static Type<Handler> getType() {
    return TYPE != null ? TYPE : (TYPE = new Type<Handler>());
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onNavigatorSelectionChanged(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }
}
