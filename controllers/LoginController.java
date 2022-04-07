package controllers;

import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import utilities.GlobalStatics;
import utilities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.awt.*;
import java.sql.SQLException;
import java.util.function.Function;

import static utilities.GlobalStatics.onClose;

public class LoginController {

    //Text field where the user inputs their username
    @FXML
    private TextField usernameTF;
    //Text field where the user inputs their password
    @FXML
    private PasswordField passwordTF;

    /**
     *
     */
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            KeyCombination enter = new KeyCodeCombination(KeyCode.ENTER);
            passwordTF.getScene().setOnKeyPressed(e -> {
                if (enter.match(e)) {
                    attemptLogin();
                }
            });
        });
    }

    @FXML
    private void attemptLogin() {
            GlobalStatics.deHighlightErrors(usernameTF, passwordTF);
            //Error Checking
            if (usernameTF.getText().isEmpty()) {
                GlobalStatics.highlightErrorsV("Username must have a value", usernameTF, passwordTF);
                return;
            }
            if (usernameTF.getText().contains("\'")) {
                GlobalStatics.highlightErrorsV("Don't try to sqli me buddo, \' are not allowed", usernameTF, passwordTF);
                return;
            }
            if (passwordTF.getText().isEmpty()) {
                GlobalStatics.highlightErrorsV("Password must have a value", passwordTF);
                return;
            }

            String username = usernameTF.getText();
            String password = passwordTF.getText();

            User primaryUser = new User(0, username, password);

        try {
            if (primaryUser.validateLogin()) {
                Controller.setID(primaryUser.getUserID());
                PaperTrading();
            } else {
                GlobalStatics.highlightErrorsV("Incorrect password for username found in database. " +
                    "Try again or create a new account.", usernameTF, passwordTF);
            }
        } catch (Throwable e) {
            if (e instanceof SQLException) {
                GlobalStatics.highlightErrorsV("Username not found in database. " +
                    "Try again with a different username or create a new account.", usernameTF, passwordTF);
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void PaperTrading() {
        try {
            ((Stage) usernameTF.getScene().getWindow()).close();
            //Handle a window's close event on the PaperTrading
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = (int) screenSize.getWidth();
            int height = (int) screenSize.getHeight();

            GlobalStatics.showX("/fxmlFiles/PaperTradingOriginal.fxml",
                    "Equities Trading Simulator",
                    width - 250,
                    height - 250,
                    Modality.WINDOW_MODAL,
                    1.0,
                    true,
                    onClose,
                    getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createNewUser() {
        try {
            ((Stage) usernameTF.getScene().getWindow()).close();
            //Handle a window's close event on CreateUser
            Function<WindowEvent, Void> onClose = e -> {
                    System.exit(0);
                    return null;
            };
            GlobalStatics.showX("/fxmlFiles/CreateUser.fxml",
                    "Create a new user",
                    600,
                    400,
                    Modality.APPLICATION_MODAL,
                    1.0,
                    true,
                    onClose,
                    getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
