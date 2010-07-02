/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.view;

import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.presenter.DatasourcePresenter;
import org.obiba.opal.web.gwt.app.client.ui.HasFieldUpdater;
import org.obiba.opal.web.model.client.TableDto;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.ListView.Delegate;

public class DatasourceView extends Composite implements DatasourcePresenter.Display {
  @UiTemplate("DatasourceView.ui.xml")
  interface DatasourceViewUiBinder extends UiBinder<Widget, DatasourceView> {
  }

  private static DatasourceViewUiBinder uiBinder = GWT.create(DatasourceViewUiBinder.class);

  @UiField
  Label datasourceName;

  @UiField
  Label tablesTableTitle;

  @UiField
  FlowPanel toolbarPanel;

  @UiField
  CellTable<TableDto> table;

  SelectionModel<TableDto> selectionModel = new SingleSelectionModel<TableDto>();

  SimplePager<TableDto> pager;

  private Image spreadsheetDownloadImage;

  private TableNameColumn tableNameColumn;

  private Translations translations = GWT.create(Translations.class);

  public DatasourceView() {
    initWidget(uiBinder.createAndBindUi(this));
    addTableColumns();
    initToolbar();
  }

  private void addTableColumns() {

    table.addColumn(tableNameColumn = new TableNameColumn() {

      @Override
      public String getValue(TableDto object) {
        return object.getName();
      }
    }, translations.nameLabel());

    table.addColumn(new TextColumn<TableDto>() {

      @Override
      public String getValue(TableDto object) {
        return object.getEntityType();
      }
    }, translations.entityTypeLabel());

    table.addColumn(new TextColumn<TableDto>() {

      @Override
      public String getValue(TableDto object) {
        return Integer.toString(object.getVariableCount());
      }
    }, translations.variablesLabel());

    table.setSelectionEnabled(true);
    table.setSelectionModel(selectionModel);
    table.setPageSize(50);
    pager = new SimplePager<TableDto>(table);
    table.setPager(pager);
    FlowPanel p = ((FlowPanel) table.getParent());
    p.insert(pager, 0);
    DOM.removeElementAttribute(pager.getElement(), "style");
    DOM.setStyleAttribute(pager.getElement(), "cssFloat", "right");
  }

  private void initToolbar() {
    spreadsheetDownloadImage = new Image("image/spreadsheet-download-icon.png");
    toolbarPanel.add(spreadsheetDownloadImage);
    DOM.setStyleAttribute(spreadsheetDownloadImage.getElement(), "cssFloat", "right");
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  @Override
  public void startProcessing() {
  }

  @Override
  public void stopProcessing() {
  }

  @Override
  public void renderRows(final JsArray<TableDto> rows) {
    final JsArray<TableDto> tableRows = (rows != null) ? rows : (JsArray<TableDto>) JsArray.createArray();

    tablesTableTitle.setText(translations.tablesLabel() + " (" + tableRows.length() + ")");

    table.setDelegate(new Delegate<TableDto>() {

      @Override
      public void onRangeChanged(ListView<TableDto> listView) {
        int start = listView.getRange().getStart();
        int length = listView.getRange().getLength();
        listView.setData(start, length, JsArrays.toList(tableRows, start, length));
      }

    });
    pager.firstPage();
    table.setData(0, table.getPageSize(), JsArrays.toList(tableRows, 0, table.getPageSize()));
    table.setDataSize(tableRows.length(), true);
    table.redraw();
  }

  @Override
  public Label getDatasourceNameLabel() {
    return datasourceName;
  }

  @Override
  public HasClickHandlers getSpreadsheetIcon() {
    return spreadsheetDownloadImage;
  }

  private abstract class TableNameColumn extends Column<TableDto, String> implements HasFieldUpdater<TableDto, String> {
    public TableNameColumn() {
      super(new ClickableTextCell());
    }
  }

  @Override
  public HasFieldUpdater<TableDto, String> getTableNameColumn() {
    return tableNameColumn;
  }
}
