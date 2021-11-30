package controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import utilities.StockWatchlist;
import yahoofinance.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.RED;

public class WatchListEditor {
	@FXML
	private TableView<String[]> topTableView, bottomTableView;
	@FXML
	private TableColumn<String[], String> symbolTC, watchlists;
	
	private ArrayList<StockWatchlist> stockWatchlists = new ArrayList<>();
	private static final ArrayList<StockWatchlist> sstockWatchlists = new ArrayList<>();
	private final ScheduledExecutorService exs = Executors.newScheduledThreadPool(1);
	private static final int COLUMN_1_NUM = 0;
	
	@FXML
	private void initialize(){
		stockWatchlists = sstockWatchlists;
		loadTopTableView();
		Platform.runLater(() -> topTableView.getScene().getWindow().setOnCloseRequest(event -> exs.shutdown()));
	}

	private void loadTopTableView() {
		topTableView.getColumns().set(0, watchlists);
		
		watchlists.setCellValueFactory(p -> new SimpleStringProperty(p.getValue()[COLUMN_1_NUM]));
		
		for (StockWatchlist stockWatchlist : stockWatchlists){
			topTableView.getItems().add(new String[]{stockWatchlist.getName()});
		}
	}
	
	private int getSelectedCell(){
		return topTableView.getFocusModel().getFocusedIndex();
	}

	@FXML
	public void loadBottomTableView() {
		symbolTC.setCellValueFactory(p -> new SimpleStringProperty(p.getValue()[COLUMN_1_NUM]));
		symbolTC.setCellFactory(column -> new TableCell<String[], String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty); //This is mandatory
				if (empty) return;
				HashMap<String, Stock> success = stockWatchlists.get(getSelectedCell()).getSuccesses();
				if (success.get(item) != null) this.setTextFill(GREEN);//it is a success, make it green
				else this.setTextFill(RED);
				setText(item);
			}
		});
		
		bottomTableView.getColumns().set(0, symbolTC);
		bottomTableView.getItems().clear();
		ArrayList<Stock> stocks = stockWatchlists.get(getSelectedCell()).getStockArrayList();
		for (Stock stock : stocks) bottomTableView.getItems().add(new String[]{stock.getSymbol()});
		
		//A hacky way of updating the table
		Runnable jl = () -> bottomTableView.getItems().set(0, bottomTableView.getItems().get(0));
		exs.scheduleAtFixedRate(jl, 2L, 10, SECONDS);
	}
	
	public ArrayList<StockWatchlist> getStockWatchlists() {
		return stockWatchlists;
	}
	
	public void setStockWatchlist(ArrayList<StockWatchlist> stockWatchlists) {
		this.stockWatchlists = stockWatchlists;
	}
	
	public static void addStockWatchlist(StockWatchlist stockWatchlist){
			sstockWatchlists.add(stockWatchlist);
	}
}
