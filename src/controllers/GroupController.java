package controllers;

import utilities.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupController {

    @FXML
    private TableView topTableView, bottomTableView;
    @FXML
    private TableColumn<Map, String> groupNameTC, creatorTC, rankColumn, usernameColumn;
    @FXML
    private TableColumn<Map, Double> percentChangeColumn;

    //TableView Variables
    private static final String COLUMN_1_MAP_KEY = "Group Name";
    private static final String COLUMN_2_MAP_KEY = "Creator";
    private static final String COLUMN_3_MAP_KEY = "Rank";
    private static final String COLUMN_4_MAP_KEY = "Username";
    private static final String COLUMN_5_MAP_KEY = "Percent Change";

    //UI Variables
    private static User staticUser;
    private static String selectedTheme;
    private String theme;
    private String selectedGroup = "";

    @FXML
    void initialize() {
        theme = selectedTheme;
        loadTopTableView(staticUser);
    }

    public GroupController() {
    }

    void loadTopTableView(User p1) {
        groupNameTC.setCellValueFactory(new MapValueFactory<>(COLUMN_1_MAP_KEY));
        creatorTC.setCellValueFactory(new MapValueFactory<>(COLUMN_2_MAP_KEY));
        topTableView.getColumns().setAll(groupNameTC, creatorTC);
        Callback<TableColumn<Map, String>, TableCell<Map, String>>
                cellFactoryForMap = p -> new TextFieldTableCell(new StringConverter() {
            @Override
            public String toString(Object t) {
                return t.toString();
            }

            @Override
            public Object fromString(String string) {
                return string;
            }
        });

        groupNameTC.setCellFactory(cellFactoryForMap);
        creatorTC.setCellFactory(cellFactoryForMap);

        loadPossibleGroups(p1);
    }

    static void setupTheme(String Theme) {
        selectedTheme = Theme;
    }

    @FXML
    public void loadBottomTableView() throws SQLException {
        //Get the selected cell
        ObservableList<TablePosition> posOL = topTableView.getSelectionModel().getSelectedCells();
        if (posOL.size() == 0) return;
        TablePosition pos = posOL.get(0);
        String group = groupNameTC.getCellData(pos.getRow());

        //see if the click is really a new group or if they are just sorting/clicking
        if (!group.equals(selectedGroup)) {
            staticUser.setGroupID(selectedGroup);
            Object[][] data = staticUser.getGroupMemberData();
            ResultSet rs = staticUser.groupRules();
            rs.next();
            ObservableList<Map> allData = FXCollections.observableArrayList();
            for (int i =0; i < data.length; i++) {
                Map<String, Object> dataRow = new HashMap<>(3);
                dataRow.put(COLUMN_3_MAP_KEY, i+1);//Rank
                dataRow.put(COLUMN_4_MAP_KEY, data[i][0]);//Username
                dataRow.put(COLUMN_5_MAP_KEY, ((Double)(data[i][1]) - rs.getDouble(9)) / 100.000);//AccountValue
                allData.add(dataRow);
                i++;
            }
            if (!rs.isClosed()) rs.close();
            rankColumn.setCellValueFactory(new MapValueFactory<>(COLUMN_3_MAP_KEY));
            usernameColumn.setCellValueFactory(new MapValueFactory<>(COLUMN_4_MAP_KEY));
            percentChangeColumn.setCellValueFactory(new MapValueFactory<>(COLUMN_5_MAP_KEY));
            bottomTableView.setItems(allData);
            bottomTableView.getColumns().setAll(rankColumn, usernameColumn, percentChangeColumn);
            Callback<TableColumn<Map, String>, TableCell<Map, String>>
                    cellFactoryForMap = p -> new TextFieldTableCell(new StringConverter() {
                @Override
                public String toString(Object t) {
                    return t.toString();
                }

                @Override
                public Object fromString(String string) {
                    return string;
                }
            });

            rankColumn.setCellFactory(cellFactoryForMap);
            usernameColumn.setCellFactory(cellFactoryForMap);
            percentChangeColumn.setCellFactory(new Callback<TableColumn<Map, Double>, TableCell<Map, Double>>() {
                public TableCell call(TableColumn p) {
                    return new TableCell<Map, Double>() {

                        @Override
                        public void updateItem(Double item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {
                                // Get fancy and change color based on data
                                if (item < 0.0)
                                    this.setTextFill(Color.RED);
                                else if (item > 0.0)
                                    this.setTextFill(Color.GREEN);
                                setText(item.toString());
                            }
                        }
                    };
                }
            });
            selectedGroup = group;
        }
    }

    private void loadPossibleGroups(User p1) {
        try {
            ObservableList<Map> allData = FXCollections.observableArrayList();

            Object[][] groupSet = p1.getGroupSet();

            for (Object[] data : groupSet) {
                Map<String, Object> dataRow = new HashMap<>(2);
                dataRow.put(COLUMN_1_MAP_KEY, data[0]);
                dataRow.put(COLUMN_2_MAP_KEY, data[1]);
                allData.add(dataRow);
            }
            topTableView.setItems(allData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void joinGroup() {
        GroupInteractionController.getUserData(staticUser);
        showX("/fxmlFiles/JoinNewGroup.fxml", "Join Group", 320, 250, Modality.APPLICATION_MODAL, 1.0, true);
        loadPossibleGroups(staticUser);
    }

    static void getUserData(User p1) {
        staticUser = p1;
    }

    @FXML
    void createNewGroup() {
        GroupInteractionController.getUserData(staticUser);
        showX("/fxmlFiles/groupCreationOptions.fxml", "Group Creation", 378, 472, Modality.APPLICATION_MODAL, 1.0,
              true);
        loadPossibleGroups(staticUser);
    }

    private void showX(String resource, String title, int w, int h, Modality modality, double opacity, boolean resize) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(resource));
            Parent root = fxmlLoader.load();
            Stage primaryStage = new Stage();
            primaryStage.setTitle(title);
            root.getStylesheets().add(theme);
            primaryStage.setScene(new Scene(root, w, h));
            primaryStage.setResizable(resize);
            primaryStage.setOnCloseRequest(e -> primaryStage.close());
            primaryStage.initModality(modality);
            primaryStage.setOpacity(opacity);
            primaryStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}