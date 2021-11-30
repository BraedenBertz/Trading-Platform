package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import utilities.User;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GroupInteractionController {

    @FXML
    private TextField groupPasswordTF, groupNameTF, start, end, newName, password, minimumPrice, commission, positionLimit, adminPasswordTF;
    @FXML
    private RadioButton noReset, yesReset, noShort, yesShort;
    @FXML
    private ToggleGroup shortSelling, reset;

    private static User staticUser;

    @FXML
    void insertUserIntoGroup() {
        boolean success = false;
        String errorBorder = "-fx-border-color: red";
        String errorMessage = "One or more of the fields were incorrect";
        try {
            if (!groupNameTF.getText().isEmpty())
                success = staticUser.joinGroup(groupNameTF.getText(), groupPasswordTF.getText());
            if (!success) {
                groupNameTF.clear();
                groupNameTF.setStyle(errorBorder);
                groupNameTF.setTooltip(new Tooltip(errorMessage));
                groupPasswordTF.clear();
                groupPasswordTF.setStyle(errorBorder);
                groupPasswordTF.setTooltip(new Tooltip(errorMessage));
            } else {
                closeWindow();
            }
        } catch (Throwable e) {
            groupNameTF.clear();
            groupNameTF.setStyle(errorBorder);
            groupNameTF.setTooltip(new Tooltip(errorMessage));
            groupPasswordTF.clear();
            groupPasswordTF.setStyle(errorBorder);
            groupPasswordTF.setTooltip(new Tooltip(errorMessage));
        }
        GroupController gc = new GroupController();
        gc.loadTopTableView(staticUser);
    }

    @FXML
    void tryToCreateNewGroup() {
        if (!checkForErrors()) return;

        noShort.setUserData("NO");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate startDate = LocalDate.parse(start.getText(), formatter);
        LocalDate endDate = LocalDate.parse(end.getText(), formatter);

        if (startDate.isAfter(endDate)) highlightErrors("Start Date cannot be after End Date");
        if (startDate.isEqual(endDate)) highlightErrors("Start Date cannot be the same as End Date");

        String groupName = newName.getText();
        String groupPassword = password.getText();
        boolean shortSelling = this.shortSelling.getSelectedToggle().getUserData().toString().equals("NO");
        double commission = this.commission.getText().isEmpty() ? 4.99 : Double.parseDouble(this.commission.getText());
        int limit =  positionLimit.getText().isEmpty() ? 20 : Integer.parseInt(this.commission.getText());
        double minPrice = minimumPrice.getText().isEmpty() ? 1.000 : Double.parseDouble(minimumPrice.getText());
        String adminPass = adminPasswordTF.getText();

        try {
            staticUser.createGroup(groupName,
                    groupPassword,
                    startDate,
                    endDate,
                    commission,
                    limit,
                    minPrice,
                    100000.000,
                    shortSelling,
                    adminPass);
            staticUser.joinGroup(groupName, groupPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ((Stage) adminPasswordTF.getScene().getWindow()).close();
    }

    private boolean checkForErrors() {
        if (adminPasswordTF.getText().isEmpty()) {
            highlightErrors("AdminPassword must not be empty");
        }
        if (newName.getText().isEmpty()) {
            highlightErrors("There must be a name");
        }
        if (newName.getText().contains("\'")) {
            highlightErrors("\' is not allowed");
        }
        if (start.getText().isEmpty()) {
            highlightErrors("Start Date must not be empty");
        }
        if (end.getText().isEmpty()) {
            highlightErrors("End Date must not be empty");
        }
        if (start.getText().contains("\'")) {
            highlightErrors("\' is not allowed");
        }
        if (end.getText().contains("\'")) {
            highlightErrors("\' is not allowed");
        }

        return true;
    }

    private void highlightErrors(String errorMessage) {
        String border = "-fx-border-color: red";
        adminPasswordTF.setStyle(border);
        adminPasswordTF.setTooltip(new Tooltip(errorMessage));
        adminPasswordTF.clear();
        start.setStyle(border);
        start.setTooltip(new Tooltip(errorMessage));
        end.setStyle(border);
        end.setTooltip(new Tooltip(errorMessage));
    }

    static void getUserData(User p1){
        staticUser = p1;
    }

    @FXML
    void closeWindow(){
        ((Stage) groupPasswordTF.getScene().getWindow()).close();
    }
}