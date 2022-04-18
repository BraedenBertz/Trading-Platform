package utilities;

import controllers.Controller;
import controllers.SearchController;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

class Scanner {
	
	private static final String[][] tables = SearchController.getTables();
	private final Controller controller;
	private final ArrayList<String> symbols = new ArrayList<>();
	private final ExecutorService exs = Executors.newFixedThreadPool(2);
	private final HashMap<String, String> successes = new HashMap<>();
	private final Function<Stock, Boolean> expression;
	private int current = 0;
	private final FutureTask<Set<String>> future =
			new FutureTask<>(this::call);
	
	Scanner(Function<Stock, Boolean> expression, Controller controller){
		this.expression = expression;
		this.controller = controller;
		startScan();
	}
	
	private void startScan() {
		//Create a new thread that does the scan
		exs.submit(future);
	}

	private Set<String> call() throws IOException {

		controller.showProgressBar(true, "Full Stock Scan");
		long totalSize = 0;
		for (int i = 0; i < 26; i++) {
			totalSize += tables[i].length;
		}
		final long finalTotalSize = totalSize;

		for (String[] table : tables) {
			Map<String, Stock> stocks = YahooFinance.get(table);
			stocks.forEach((k, v) -> {
				increment();
				if (expression.apply(v))
					successes.put(k, k);
				controller.updateProgressBar(current / (double) finalTotalSize);
			});
			symbols.clear();
		}

		controller.showProgressBar(false, "");
		return successes.keySet();
	}

	private void increment() {
		current++;
	}

	Set<String> getFutureSuccesses(){
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
}
