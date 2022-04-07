package utilities;

import yahoofinance.Stock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.*;

/**
 * A utility class that takes:
 *      an arbitrary amount of stocks
 *      a name for the list
 *      a refresh period
 *      a conditional criteria
 *
 * Must have an action to do once a certain condition is met
 * */
public class StockWatchlist {

    private ArrayList<Stock> stockArrayList = new ArrayList<>();
    private final HashMap<String, Stock> successes = new HashMap<>();
    private String conditional = "0";
    private String name;
    private long refreshPeriod = 5;
    private double epsilon = 1.15;
    private Evaluator evaluator = new Evaluator();
    private ScheduledExecutorService scanner = Executors.newScheduledThreadPool(1);
    private Function<Stock, Boolean> expression = stock -> {
        evaluator.setActiveStock(stock);
        return stock.getQuote().getBid().doubleValue() <= evaluator.evaluate(conditional)*epsilon &&
            stock.getQuote().getBid().doubleValue() >= evaluator.evaluate(conditional)/epsilon;
    };

    public StockWatchlist(String name){
        this.name = name;
    }

    public StockWatchlist(String name, String conditional){
        this(name);
        this.conditional = conditional;
    }

    public StockWatchlist(String name, String conditional, long refreshPeriod){
        this(name, conditional);
        this.refreshPeriod = refreshPeriod;
    }

    public StockWatchlist(String name, String conditional, long refreshPeriod, ArrayList<Stock> stocks){
        this(name, conditional, refreshPeriod);
        this.stockArrayList = stocks;
    }

    /////////////////////////////////////////////////////////////////////
    /*Getters and Setters*/
    /////////////////////////////////////////////////////////////////////

    public void addStock(Stock stock){
        stockArrayList.add(stock);
    }

    public void addStocks(Collection<Stock> stocks){
        for (Stock stock : stocks){
            addStock(stock);
        }
    }

    public void addStocks(Stock[] stocks){
        for (Stock stock : stocks){
            addStock(stock);
        }
    }

    public ArrayList<Stock> getStockArrayList() {
        return this.stockArrayList;
    }

    public void setStockArrayList(ArrayList<Stock> stockArrayList) {
        this.stockArrayList = stockArrayList;
    }

    public HashMap<String, Stock> getSuccesses() {
        return this.successes;
    }

    public String getConditional(){
        return this.conditional;
    }

    public void setConditional(String conditional){
        this.conditional = conditional;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    public long getRefreshPeriod(){
        return this.refreshPeriod;
    }

    public void setRefreshPeriod(long refreshPeriod){
        this.refreshPeriod = refreshPeriod;
    }

    public Function<Stock, Boolean> getExpression() {
        return this.expression;
    }

    public void setExpression(Function<Stock, Boolean> expression) {
        this.expression = expression;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public ScheduledExecutorService getScanner() {
        return scanner;
    }

    public void setScanner(ScheduledExecutorService scanner) {
        this.scanner = scanner;
    }

    /////////////////////////////////////////////////////////////////////
    /*Utility Methods*/
    /////////////////////////////////////////////////////////////////////

    public void startScan(){
        Runnable scan = new Runnable() {
            /**
             * When an object implementing interface <code>Runnable</code> is used
             * to create a thread, starting the thread causes the object's
             * <code>run</code> method to be called in that separately executing
             * thread.
             * <p>
             * The general contract of the method <code>run</code> is that it may
             * take any action whatsoever.
             *
             * @see Thread#run()
             */
            @Override
            public void run() {
                for (Stock stock: stockArrayList) {
                    successes.put(stock.getSymbol(), expression.apply(stock) ? stock : null);
                }
                successes.entrySet().removeIf(entries->entries.getValue() == null);
            }
        };
        //Create a new thread that does the scan
        scanner.scheduleAtFixedRate(scan, 0L, refreshPeriod, SECONDS);
    }

    public void stopScan(){
        //Delete the scanning thread
        scanner.shutdown();
    }

}
