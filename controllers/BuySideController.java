package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import userItems.User;

import javax.swing.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Class tasked with handling the logic for when the user initiates a buy when there is an ask value above 0
 * */
public class BuySideController {

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

    /**
     * Collect data necessary for the window to operate
     *
     * @param symbol The symbol that the user is trying to buy
     * @param p1 A reference to the logged in user
     * @param price A function that gets the price of the symbol
     * */
    static void setup(String symbol, User p1, Function<String, BigDecimal> price) {
        security = symbol;
        staticUser = p1;
        getPrice = price;
    }

    /**
     * Setup the environment that the window will be held in
     * */
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

        Platform.runLater(() -> orderSizeTF.getScene().getWindow().setOnCloseRequest(p -> exs.shutdown()));
    }

    /**
     * Take the price from teh label and multiply it by the number in the orderSize TextField
     * */
    @FXML
    private void updateOrderSize() {
            if (orderSizeTF.getText().isEmpty()) return;
            //Get the order size (or number of shares of the equity the user wants to buy)
            BigDecimal orderSize = new BigDecimal(orderSizeTF.getText());
            //Set the label to be the product of the equity trade price and the amount of shares to be bought
            orderSizeLabel.setText("$" + orderSize.multiply(new BigDecimal(label.getText())));
    }

    /**
     * Call the user class and try to set the number of shares to the specified amount, then minus that
     * amount from the user's current account value
     * */
    @FXML
    private void finalizeOrder() throws SQLException {
        if (orderSizeTF.getText().contains("'") || orderSizeTF.getText().contains("\\")) {
            JOptionPane.showMessageDialog(null, "Don't try to sql inject me buddo!");
            return;
        }
       // primaryUser.executeTrade(security, new BigDecimal(label.getText()), Long.valueOf(orderSizeTF.getText()));
        ((Stage) label.getScene().getWindow()).close();
    }
}
