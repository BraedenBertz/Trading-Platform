package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import utilities.PasswordHasher;
import utilities.User;

import java.util.function.Function;

import static utilities.GlobalStatics.*;

public class CreateUserController {

    @FXML
    private TextField usernameTF, eMailTF;
    @FXML
    private PasswordField passwordTF, rPassTF;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            KeyCombination enter = new KeyCodeCombination(KeyCode.ENTER);
            usernameTF.getScene().setOnKeyPressed(event -> {
                if (enter.match(event)) {
                    createNewUser();
                }
            });
        });
    }

    @FXML
    private void loadPrimaryLogin() {
        try {
            ((Stage) usernameTF.getScene().getWindow()).close();

            Function<WindowEvent, Void> onClose = e -> {
                System.exit(0);
                return null;
            };

            //primaryStage.show();
            showX("/fxmlFiles/PrimaryLogin.fxml",
                "Personal Finance Stock Trading Simulation",
                600,
                327,
                Modality.APPLICATION_MODAL,
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
        if (errorsPresent()) return;

        try {
            PasswordHasher hasher = new PasswordHasher(passwordTF.getText());

            String username = usernameTF.getText();
            String password = hasher.getPassword();
            System.out.println(password);
            String email = eMailTF.getText();
            String salt = hasher.getSalt();
            int iter = hasher.getMAXITER();

            User primaryUser = new User(0, username, password, email, salt, iter);

            primaryUser.insertNewUser();
            loadPrimaryLogin();

        } catch (Throwable e) {
            e.printStackTrace();
            highlightErrorsV("Username already in use, either login with username or choose a new one", usernameTF);
        }
    }

    private boolean errorsPresent(){
        deHighlightErrors(passwordTF, eMailTF, usernameTF, rPassTF);
        if (usernameTF.getText().isEmpty()) {
            highlightErrorsV("Username must have a value", usernameTF);
            return true;
        }
        else if (usernameTF.getText().contains("\'")) {
            highlightErrorsV("Don't try to sqli me buddo, \' are not allowed", eMailTF, rPassTF, passwordTF, usernameTF);
            return true;
        }
        else if (passwordTF.getText().isEmpty()) {
            highlightErrorsV("Password must have a value", passwordTF);
            return true;
        }
        else if (rPassTF.getText().isEmpty()) {
            highlightErrorsV("Re-enter your password", rPassTF);
            return true;
        }
        else if (eMailTF.getText().isEmpty()) {
            highlightErrorsV("E-Mail must have a value", eMailTF);
            return true;
        }
        else if (!passwordTF.getText().equals(rPassTF.getText())) {
            highlightErrorsV("Passwords must match", passwordTF, rPassTF);
            return true;
        }
        return false;
    }
}
