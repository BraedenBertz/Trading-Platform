package controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import utilities.GlobalStatics;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchController {

    //Show what symbols have matched the user's input thus far and allow the user to
    //click on any of them and then have it plot on the main application window
    @FXML
    TableColumn<String, String> searchSymbol;
    //What the user will type in; input used to sift through universe of equities
    @FXML
    TextField searchTF;
    @FXML
    TableView<String> searchTV;

    //First level is the 6 types of instruments available
    //  Second level is the alphabetized indices
    //  Third level is the alphabet grouped instrument singles
    private static final String[][][] tables = new String[6][26][];

    //The currently selected instrument type the user is on, stock, option, etc
    private static int staticShownInstrument = 0;

    //The first character the user typed on the main application window
    private static String staticTypedSymbol = "";
    private static Controller controller = null;
    private boolean initialized = false;

    //The string that we will compare the start of ticker symbols for equality with
    private String typedSymbol = "";

    //For when the user types, this gives the index of the first letter associated  with
    //the user's input and the alphabet with a=0, z=25
    private int index = 0;

    /**
     * Called before the window shows. Assign the ActionEvents associated with the window and
     * construct the TableColumn.
     * */
    @FXML
    void initialize() {
        searchTF.setOnKeyReleased(this::changeSearchField);

        searchSymbol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue()));

        searchTV.setOnMouseClicked(e -> plotStock());

        Platform.runLater(() -> searchTV.getScene().getWindow().setOnCloseRequest(p -> staticTypedSymbol = ""));
    }

    /**
    * Whenever the user types while focused on the application, we try to find
     * all equities that start with the key inputs, for example:
     *
     * 'a' -> all stocks that start with a
     * 'b' -> all stocks that start with ab
     * 'del' -> all stocks that start with a
     *
     * @param event The KeyEvent that we will get the character from for searching
     * */
    private void changeSearchField(KeyEvent event) {
        if (!initialized) searchTF.appendText(event.getText());
        initialized = true;
        typedSymbol = searchTF.getText();
        index = typedSymbol.length()==0 ? 0 : GlobalStatics.getIndex(typedSymbol.toUpperCase().charAt(0));
        populateSearchField();
    }

    /**
     * Put all of the equities whose start of name matches the typedSymbol, or what the
     * user has typed thus far, into the searchTV or TableView tied to this controller
     * */
    private void populateSearchField() {
        String[] data = tables[staticShownInstrument][index];
        GlobalStatics.populateSearchField(typedSymbol, data, searchTV);
    }

    /**
     * Create an alphabetized container array, so all instruments with starting letter A will
     * go into one of the 26 indices created, B will go into another, etc...
     * for whatever instrument type we will have. This allows us to easily segment
     * and search by first letter when trying to find the stocks that
     * match the user's inputs. Then put the data of the type of shownInstruments
     * into their respective container.
     *
     * @param data The data that we will sort into alphabetized containers
     * @param shownInstruments The index of tables that we will populate
     * */
    public static void addTable(String[] data, int shownInstruments) {
        //Error checking
        if (data.length == 0 || shownInstruments < 0) { return; }

        final HashMap<String, ArrayList<String>> holderForAlphabetizedData = new HashMap<>();
        final String[] alphabet = {"A", "B", "C", "D",
                "E", "F", "G", "H",
                "I", "J", "K", "L",
                "M", "N", "O", "P",
                "Q", "R", "S", "T",
                "U", "V", "W", "X",
                "Y", "Z"};

        for (String letter : alphabet) {
            holderForAlphabetizedData.put(letter, new ArrayList<>());
        }
        for (String key : data) {
            holderForAlphabetizedData.get(key.charAt(0) + "").add(key);
        }

        //Transfer the data from holderForAlphabetizedData.get(letter) to a new String[][]
        String[][] datum = new String[26][];
        for (int i = 0; i < 26; i++) {
            datum[i] = holderForAlphabetizedData.get(alphabet[i]).toArray(new String[0]);
        }
        tables[shownInstruments] = datum;
    }

    /**
     * Return the alphabetized String[][] of whatever container the user is currently on,
     * for example: If the user is on stocks and starts typing, we will return the
     * String[][] that has all of the alphabetized stocks.
     *
     * @return the String[][] of whatever container the user is currently selected on
     * */
    public static String[][] getTables() {
        /*
         * 0 = Stocks
         * 1 = Currencies
         * 2 = Commoditites
         * 3 = bonds
         * 4 = options
         * 5 = indices
         * 6 = ETFs
         * */
        int staticShownInstruments = 0;
        return tables[staticShownInstruments];
    }

    /**
     * Take the selected cell from the TableView and get its ticker symbol. Plot that ticker symbol
     * on the main application window.
     * */
    @FXML
    private void plotStock() {
        String symbol = searchSymbol.getCellData(searchTV.getSelectionModel().getSelectedIndex());
        if (symbol == null) return;
        controller.plotStockOnBothCharts(symbol, controller.getLineChart(), controller.getInterval());
    }

    /**
     * Allow other classes to pass the necessary references to this class statically
     *
     * @param c The main application Controller, useful for when we want to plot a selected equity
     * @param shownInstrument The type of instrument the user is currently on, stock, option, etc
     * @param startingCharacter The first letter the user typed on the main application window
     *                          for use in finding the stocks they are looking for
     * */
    static void setup(String startingCharacter, Controller c, int shownInstrument) {
        staticTypedSymbol = startingCharacter;
        controller = c;
        staticShownInstrument = shownInstrument;
    }
}
