/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.widgets.view;

import org.obiba.opal.web.gwt.app.client.fs.presenter.FileSystemTreePresenter;
import org.obiba.opal.web.gwt.app.client.fs.presenter.FolderDetailsPresenter;
import org.obiba.opal.web.gwt.app.client.i18n.Translations;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.FileSelectorPresenter.Display;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.PopupViewImpl;

/**
 *
 */
public class FileSelectorView extends PopupViewImpl implements Display {
  //
  // Constants
  //

  private static final String DIALOG_HEIGHT = "38.5em";

  private static final String DIALOG_SHORT_HEIGHT = "36em";

  private static final String DIALOG_WIDTH = "60em";

  //
  // Static Variables
  //

  private static FileSelectorViewUiBinder uiBinder = GWT.create(FileSelectorViewUiBinder.class);

  private static Translations translations = GWT.create(Translations.class);

  //
  // Instance Variables
  //

  private final DialogBox dialog;

  @UiField
  DockLayoutPanel content;

  @UiField
  HTMLPanel namePanel;

  @UiField
  TextBox newFileName;

  @UiField
  ScrollPanel fileSystemTreePanel;

  @UiField
  ScrollPanel folderDetailsPanel;

  @UiField
  HTMLPanel createFolderPanel;

  @UiField
  TextBox createFolderName;

  @UiField
  Button createFolderButton;

  @UiField
  Button uploadButton;

  @UiField
  Button selectButton;

  @UiField
  Button cancelButton;

  //
  // Constructors
  //

  @Inject
  public FileSelectorView(EventBus eventBus) {
    super(eventBus);
    dialog = uiBinder.createAndBindUi(this);

    content.setHeight(DIALOG_HEIGHT);
    content.setWidth(DIALOG_WIDTH);

    dialog.setText(translations.fileSelectorTitle());
    dialog.setHeight(DIALOG_HEIGHT);
    dialog.setWidth(DIALOG_WIDTH);
  }

  private void updateHeight(String height) {
    dialog.setHeight(height);
    content.setHeight(height);
  }

  //
  // FileSelectorPresenter.Display Methods
  //

  @Override
  public void showDialog() {
    center();
    show();
  }

  public void hideDialog() {
    hide();
  }

  public void setTreeDisplay(FileSystemTreePresenter.Display treeDisplay) {
    getFileSystemTreePanel().clear();
    getFileSystemTreePanel().add(treeDisplay.asWidget());
  }

  public void setDetailsDisplay(FolderDetailsPresenter.Display detailsDisplay) {
    getFolderDetailsPanel().clear();
    getFolderDetailsPanel().add(detailsDisplay.asWidget());
  }

  public void setNewFilePanelVisible(boolean visible) {
    namePanel.setVisible(visible);
    updateHeight(visible ? DIALOG_HEIGHT : DIALOG_SHORT_HEIGHT);
  }

  public void setNewFolderPanelVisible(boolean visible) {
    createFolderPanel.setVisible(visible);
  }

  @Override
  public void setDisplaysUploadFile(boolean visible) {
    uploadButton.setVisible(visible);
  }

  public HasWidgets getFileSystemTreePanel() {
    return fileSystemTreePanel;
  }

  public HasWidgets getFolderDetailsPanel() {
    return folderDetailsPanel;
  }

  public HandlerRegistration addSelectButtonHandler(ClickHandler handler) {
    return selectButton.addClickHandler(handler);
  }

  public HandlerRegistration addCancelButtonHandler(ClickHandler handler) {
    return cancelButton.addClickHandler(handler);
  }

  public HandlerRegistration addCreateFolderButtonHandler(ClickHandler handler) {
    return createFolderButton.addClickHandler(handler);
  }

  public String getNewFileName() {
    return newFileName.getText();
  }

  public void clearNewFileName() {
    newFileName.setText("");
  }

  public HasText getCreateFolderName() {
    return createFolderName;
  }

  public void clearNewFolderName() {
    createFolderName.setText("");
  }

  public void startProcessing() {
  }

  public void stopProcessing() {
  }

  public Widget asWidget() {
    return dialog;
  }

  //
  // Inner Classes / Interfaces
  //

  @UiTemplate("FileSelectorView.ui.xml")
  interface FileSelectorViewUiBinder extends UiBinder<DialogBox, FileSelectorView> {
  }

  @Override
  public HandlerRegistration addUploadButtonHandler(ClickHandler handler) {
    return uploadButton.addClickHandler(handler);
  }
}