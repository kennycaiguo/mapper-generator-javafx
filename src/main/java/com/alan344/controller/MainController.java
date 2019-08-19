package com.alan344.controller;

import com.alan344.bean.Column;
import com.alan344.bean.DataItem;
import com.alan344.bean.DataSource;
import com.alan344.bean.Table;
import com.alan344.constants.BaseConstants;
import com.alan344.service.ColumnService;
import com.alan344.service.DataSourceService;
import com.alan344.service.TableService;
import com.alan344.utils.Toast;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author AlanSun
 * @date 2019/8/7 17:04
 */
@Controller
public class MainController implements Initializable {
    @FXML
    private BorderPane borderPane;

    @FXML
    private BorderPane borderPane1;

    @FXML
    private MenuBar menuBar;

    @Getter
    @FXML
    private TreeItem<DataItem> treeItemRoot;

    @Getter
    @FXML
    private ListView<VBox> anchorPaneListView;

    @FXML
    private HBox rightBorderTopHbox;

    @FXML
    private TreeView<DataItem> treeViewDataSource;

    @FXML
    private CheckBox insertReturnCheckBox;
    @FXML
    private CheckBox insertCheckBox;
    @FXML
    private CheckBox countCheckBox;
    @FXML
    private CheckBox updateCheckBox;
    @FXML
    private CheckBox deleteCheckBox;
    @FXML
    private CheckBox selectCheckBox;

    @Autowired
    private DateSourceController dateSourceController;

    @Autowired
    private ConfigController configController;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private TableService tableService;

    @Autowired
    private ColumnService columnService;

    @Autowired
    private BeanFactory beanFactory;

    //--------------------------------init----------------------------------------------------------------------------//

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        menuBar.prefWidthProperty().bind(borderPane.widthProperty());

        this.treeViewInit();

        this.checkBoxInit();

        //从文件加载数据源至pane
        dataSourceService.loadDataSourceFromFile(treeItemRoot);
        if (treeItemRoot.getChildren().size() == 1) {
            treeItemRoot.getChildren().get(0).setExpanded(true);
        }
    }

    /**
     * treeView init
     */
    private void treeViewInit() {
        //设置多选
        treeViewDataSource.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        treeViewDataSource.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                ObservableList<TreeItem<DataItem>> selectedItems = treeViewDataSource.getSelectionModel().getSelectedItems();

                //item is selected - this prevents fail when clicking on empty space
                ContextMenu contextMenu;
                if (selectedItems != null) {
                    //open context menu on current screen position
                    MenuItem exportMenuItem = new MenuItem("导出");
                    exportMenuItem.setOnAction(event1 -> export());
                    if (selectedItems.size() == 1 && selectedItems.get(0).getValue() instanceof DataSource) {

                        MenuItem refreshMenuItem = new MenuItem("刷新");
                        refreshMenuItem.setOnAction(event1 -> refreshDataSource());
                        MenuItem modifyMenuItem = new MenuItem("修改数据源");
                        MenuItem deleteMenuItem = new MenuItem("删除数据源");
                        deleteMenuItem.setOnAction(event1 -> deleteDataSource());
                        contextMenu = new ContextMenu(exportMenuItem, refreshMenuItem, modifyMenuItem, deleteMenuItem);
                    } else {
                        contextMenu = new ContextMenu(exportMenuItem);
                    }
                    treeViewDataSource.setContextMenu(contextMenu);
                }
            }
        });
    }

    /**
     * checkbox init
     */
    private void checkBoxInit() {
        insertReturnCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(0, newValue));
        insertCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(1, newValue));
        countCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(2, newValue));
        updateCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(3, newValue));
        deleteCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(4, newValue));
        selectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> checkBoxAction(5, newValue));
    }

    private void checkBoxAction(int index, boolean selected) {
        ObservableList<VBox> items = anchorPaneListView.getItems();
        for (VBox item : items) {
            ((CheckBox) ((HBox) item.getChildren().get(1)).getChildren().get(index)).setSelected(selected);
        }
    }

    /*---------------------------------------MenuBar------------------------------------------------------------------*/

    /**
     * 添加数据源
     */
    @FXML
    public void addSource() throws IOException {
        dateSourceController.addDataSource((Stage) borderPane.getScene().getWindow());
    }

    @FXML
    public void exit() {
        Platform.exit();
    }

    @FXML
    public void openGithub() {
        HostServices hostServices = beanFactory.getBean(HostServices.class);
        hostServices.showDocument("https://github.com/alansun2/mapper-generator-javafx");
    }

    /*------------------------------------TreeView ContextMenu--------------------------------------------------------*/

    /**
     * 全部导出
     */
    private void export() {
        ObservableList<TreeItem<DataItem>> selectedItems = treeViewDataSource.getSelectionModel().getSelectedItems();
        List<Table> tables;
        if (selectedItems.size() == 1) {
            TreeItem<DataItem> dataItemTreeItem = selectedItems.get(0);
            if (dataItemTreeItem.getValue() instanceof DataSource) {
                //选中数据源时的导出
                ObservableList<TreeItem<DataItem>> children = dataItemTreeItem.getChildren();
                tables = new ArrayList<>();
                if (!children.isEmpty()) {
                    children.forEach(itemTreeItem -> tables.add(((Table) itemTreeItem.getValue())));
                }

                BaseConstants.selectedDateSource = (DataSource) dataItemTreeItem.getValue();
            } else {
                //单独选中table的导出
                Table table = (Table) dataItemTreeItem.getValue();
                tables = Collections.singletonList(table);

                BaseConstants.selectedDateSource = ((DataSource) dataItemTreeItem.getParent().getValue());
            }
        } else {
            //选中多个table的导出
            tables = new ArrayList<>();
            for (TreeItem<DataItem> selectedItem : selectedItems) {
                DataItem dataItem = selectedItem.getValue();
                if (dataItem instanceof Table) {
                    Table table = (Table) dataItem;
                    tables.add(table);
                }
            }

            BaseConstants.selectedDateSource = ((DataSource) selectedItems.get(0).getParent().getValue());
        }
        //把选中要导出的表在右边的listView展示
        this.setListView(tables);
        //选中的表
        BaseConstants.selectedTableNameTableMap = tables.stream().collect(Collectors.toMap(Table::getTableName, o -> o));

        for (Table table : tables) {
            if (table.getColumns() != null) {
                List<Column> columns = columnService.getColumnsFromRemote(BaseConstants.selectedDateSource, table.getTableName());
                table.setColumns(columns);
            }
        }

        //show rightBorderTopHbox
        if (!rightBorderTopHbox.isVisible() && !rightBorderTopHbox.isManaged()) {
            rightBorderTopHbox.setVisible(true);
            rightBorderTopHbox.setManaged(true);
        }
    }

    /**
     * 设置listView
     *
     * @param tables 已选表
     */
    private void setListView(List<Table> tables) {
        ObservableList<VBox> anchorPanes = FXCollections.observableArrayList();
        anchorPaneListView.setItems(anchorPanes);
        for (Table table : tables) {
            Label tableNameLabel = new Label(table.getTableName());
            tableNameLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold");

            HBox hBox = new HBox(tableNameLabel);
            hBox.setAlignment(Pos.CENTER);

            CheckBox returnId = new CheckBox("insert返回id");
            CheckBox insert = new CheckBox("insert");
            insert.setSelected(true);
            CheckBox count = new CheckBox("count");
            count.setSelected(true);
            CheckBox update = new CheckBox("update");
            update.setSelected(true);
            CheckBox delete = new CheckBox("delete");
            delete.setSelected(true);
            CheckBox select = new CheckBox("select");
            select.setSelected(true);

            Button expand = new Button();
            expand.setGraphic(new ImageView("/image/expand.png"));
            expand.setPrefWidth(80);
            expand.setStyle("-fx-background-color: transparent");

            expand.setOnAction(event -> {
                Button source = (Button) event.getSource();
                VBox selectedVBox = ((VBox) source.getParent().getParent());

                ObservableList<Node> children = selectedVBox.getChildren();
                if (children.size() == 2) {
                    expand.setGraphic(new ImageView("/image/close.png"));
                    expandColumns(selectedVBox);
                } else {
                    TableView tableView = (TableView) children.get(2);
                    if (tableView.isVisible()) {
                        expand.setGraphic(new ImageView("/image/expand.png"));
                        tableView.setVisible(false);
                        tableView.setManaged(false);
                    } else {
                        expand.setGraphic(new ImageView("/image/close.png"));
                        tableView.setVisible(true);
                        tableView.setManaged(true);
                    }
                }
            });
            HBox hBox2 = new HBox(20, returnId, insert, count, update, delete, select, expand);
            hBox2.setAlignment(Pos.CENTER);

            VBox vBox = new VBox(10, hBox, hBox2);

            anchorPanes.add(vBox);
        }
    }

    /**
     * 删除数据源
     */
    private void deleteDataSource() {
        Stage primaryStage = (Stage) borderPane.getScene().getWindow();
        ObservableList<TreeItem<DataItem>> selectedItems = treeViewDataSource.getSelectionModel().getSelectedItems();
        if (selectedItems.size() != 1) {
            Toast.makeText(primaryStage, "只能选择一个", 3000, 500, 500, 15, 5);
            return;
        }

        TreeItem<DataItem> dataItemTreeItem = selectedItems.get(0);
        if (dataItemTreeItem.getValue() instanceof Table) {
            Toast.makeText(primaryStage, "无法对表进行操作", 3000, 500, 500, 15, 5);
            return;
        }

        treeItemRoot.getChildren().remove(dataItemTreeItem);

        dataSourceService.deleteDataSource(((DataSource) dataItemTreeItem.getValue()));
    }

    /**
     * 刷新数据源下的table
     * <p>
     * 对table
     */
    private void refreshDataSource() {
        TreeItem<DataItem> selectedItem = treeViewDataSource.getSelectionModel().getSelectedItem();
        tableService.refreshTables(selectedItem);
    }

    /*------------------------------------ListView ContextMenu--------------------------------------------------------*/

//    private void refresh() {
//
//    }

    /**
     * 展开字段
     */
    private void expandColumns(VBox selectedItem) {
        String tableName = ((Label) (((HBox) selectedItem.getChildren().get(0))).getChildren().get(0)).getText();
        List<Column> columns = BaseConstants.selectedTableNameTableMap.get(tableName).getColumns();

        ObservableList<Column> gridPanes = FXCollections.observableArrayList(columns);
        TableView<Column> columnTableView = new TableView<>(gridPanes);
        columnTableView.setEditable(true);

        double borderPane1Width = borderPane1.getWidth();
        double columnWidth = borderPane1Width / 3;

        TableColumn<Column, String> tcColumnNam = new TableColumn<>("字段名");
        tcColumnNam.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getColumnName()));
        tcColumnNam.setPrefWidth(columnWidth);
        tcColumnNam.setSortable(false);

        TableColumn<Column, String> tcType = new TableColumn<>("类型");
        tcType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType()));
        tcType.setPrefWidth(columnWidth);
        tcType.setSortable(false);

        TableColumn<Column, Boolean> ignoreCheckBox = new TableColumn<>("是否忽略");
        ignoreCheckBox.setCellFactory(CheckBoxTableCell.forTableColumn(param -> {
            final Column column = columnTableView.getItems().get(param);
            column.ignoreProperty().addListener((observable, oldValue, newValue) -> column.setIgnore(newValue));
            return column.ignoreProperty();
        }));
        ignoreCheckBox.setPrefWidth(columnWidth);
        ignoreCheckBox.setSortable(false);

        columnTableView.getColumns().add(tcColumnNam);
        columnTableView.getColumns().add(tcType);
        columnTableView.getColumns().add(ignoreCheckBox);

        columnTableView.setFixedCellSize(28);
        columnTableView.prefHeightProperty().bind(columnTableView.fixedCellSizeProperty().multiply(Bindings.size(columnTableView.getItems()).add(1.01)));

        selectedItem.getChildren().add(columnTableView);

        MenuItem overrideColumnMenuItem = new MenuItem("重写");
        overrideColumnMenuItem.setOnAction(event -> {

        });
        columnTableView.setContextMenu(new ContextMenu(overrideColumnMenuItem));
    }

    //-------------

    /**
     * 导出窗口
     *
     * @throws IOException e
     */
    @FXML
    public void openConfigWindow() throws IOException {
        configController.openConfigPane((Stage) borderPane.getScene().getWindow());
    }
}
