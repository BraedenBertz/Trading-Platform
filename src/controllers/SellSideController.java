package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utilities.User;

import javax.swing.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Class tasked with handling the logic for when the user initiates a buy
 * */
public class SellSideController {

    //Declare FXML Variables
    @FXML
    private Label label, orderSizeLabel;
    @FXML
    private TextField orderSizeTF;
    @FXML
    private Button commit;

    //Declare User Variables
    private User primaryUser;//The user that logged in

    //Declare Static Variables
    private static String security;//The security that the user is trying to buy
    private static User staticUser;
    private static ScheduledExecutorService exs;
    private static Function<String, BigDecimal> getPrice;

    static void setup(String symbol, User p1, Function<String, BigDecimal> price) {
        security = symbol;
        staticUser = p1;
        getPrice = price;
    }

    @FXML
    private void initialize() {
        exs = Executors.newScheduledThreadPool(1);
        commit.setText(commit.getText()+" "+security);
        primaryUser = staticUser;
        Runnable updateOrderSize = () -> Platform.runLater(() -> {
            label.setText(getPrice.apply(security).toString());
            updateOrderSize();
        });
        exs.scheduleAtFixedRate(updateOrderSize, 0L, 5, TimeUnit.SECONDS);

        Platform.runLater(() -> orderSizeTF.getScene().getWindow().setOnCloseRequest(p -> exs.shutdownNow()));
    }

    @FXML
    private void updateOrderSize() {
        try {
            if (orderSizeTF.getText().isEmpty()) return;
            //Get the order size (or number of shares of the equity the user wants to buy)
            BigDecimal orderSize = new BigDecimal(orderSizeTF.getText());
            //Set the label to be the product of the equity trade price and the amount of shares to be bought
            orderSizeLabel.setText("$" + orderSize.multiply(new BigDecimal(label.getText())));
        } catch (Exception e) {
            orderSizeTF.deletePreviousChar();
        }
    }

    @FXML
    private void finalizeOrder() throws SQLException {
        if (orderSizeTF.getText().contains("'")) {
            JOptionPane.showMessageDialog(null, "Don't try to sql inject me buddo!");
            return;
        }
        primaryUser.executeTrade(security, new BigDecimal(label.getText()), Long.valueOf(orderSizeTF.getText()));
        ((Stage) label.getScene().getWindow()).close();
    }
}
