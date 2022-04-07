package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import utilities.Evaluator;
import utilities.GlobalStatics;
import utilities.Operator;
import utilities.StockWatchlist;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static utilities.GlobalStatics.getIndex;

public class WatchListController {

    @FXML
    TextField conditionalTF, nameTF, discriminatorTF, epsilonTF, searchTF;
    @FXML
    TableColumn<String, String> searchSymbol, addedSymbols;
    @FXML
    TableView<String> searchTV, addedTV;
    @FXML
    ChoiceBox<String> propertyCB;
    @FXML
    Button finalButton;

    private final Evaluator evaluator = new Evaluator();
    private Operator operator = null;
    private final StockWatchlist stockWatchlist = new StockWatchlist("");
    private final WatchListEditor watchListEditor;
    private static final String[][] tables = SearchController.getTables();
    private static final int COLUMN_1_NUM = 0;
    private static WatchListEditor staticWLE;

    private String typedSymbol = "";
    private int index;
    private String conditional;
    private String discriminator;

    public WatchListController() {
        watchListEditor = staticWLE;
    }

    public void initialize() {
        propertyCB.getItems().addAll(">", "≥", "<", "≤", "=", "≠");
        searchTF.setOnKeyReleased(this::changeSearchField);

        //Pre-generation processes
        index = 0;

        //Set the columns
        searchTV.getColumns().set(0, searchSymbol);
        addedTV.getColumns().set(0, addedSymbols);

        //Set each TableColumn's cellFactory to cellFactory for map
        searchSymbol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue()));
        addedSymbols.setCellValueFactory(p -> new SimpleStringProperty(p.getValue()));

        searchTV.sort();
        typedSymbol = "";
        searchTV.setOnMouseClicked(e -> {
            try {
                addStock();
            } catch(IOException e1) {
                e1.printStackTrace();
            }
        });
    }

    private void changeSearchField(KeyEvent event) {
        typedSymbol = searchTF.getText();
        index = typedSymbol.length() == 0 ? 0 : getIndex(typedSymbol.toUpperCase().charAt(0));
        populateSearchField();
    }

    private void populateSearchField() {
        String[] data = tables[index];
        GlobalStatics.populateSearchField(typedSymbol, data, searchTV);
    }

    @FXML
    private void createWatchlist() {
        WatchListEditor.addStockWatchlist(stockWatchlist);
        stockWatchlist.setName(nameTF.getText());
        stockWatchlist.setConditional(conditionalTF.getText());
        stockWatchlist.setEpsilon(epsilonTF.getText().isEmpty() ? 1.15 : Double.parseDouble(epsilonTF.getText()));
        switch(propertyCB.getValue()) {
            case ">":
                operator = Operator.lessThan;
                break;
            case "≥":
                operator = Operator.lessThanOrEqual;
                break;
            case "<":
                operator = Operator.greaterThan;
                break;
            case "≤":
                operator = Operator.greaterThanOrEqual;
                break;
            case "=":
                operator = Operator.equals;
                break;
            case "≠":
                operator = Operator.doesNotEqual;
                break;
            default:
                operator = Operator.equals;
        }

        stockWatchlist.setEvaluator(evaluator);
        conditional = evaluator.reduce(conditionalTF.getText());
        discriminator = evaluator.reduce(discriminatorTF.getText());
        stockWatchlist.setExpression(stock -> {
            evaluator.setActiveStock(stock);
            return operator.compare(evaluator.evaluate(conditional),
                evaluator.evaluate(discriminator));
        });
        stockWatchlist.startScan();
        Runnable jl = () -> stockWatchlist.getSuccesses().forEach((k, v) -> System.out.print(""));
        ScheduledExecutorService exs = Executors.newScheduledThreadPool(1);
        exs.scheduleAtFixedRate(jl, 2L, 10, SECONDS);
        ((Stage) searchTV.getScene().getWindow()).close();
    }

    @FXML
    private void addStock() throws IOException {
        int index = searchTV.getFocusModel().getFocusedIndex();
        if(index == -1) return;
        String toAdd = searchSymbol.getCellData(index);
        for(String object : addedTV.getItems()) {
            if(object.equals(toAdd)) {
                return;
            }
        }
        addedTV.getItems().add(toAdd);
        stockWatchlist.addStock(YahooFinance.get(toAdd));
    }

    public static void setStaticWLE(WatchListEditor w) {
        staticWLE = w;
    }
}
