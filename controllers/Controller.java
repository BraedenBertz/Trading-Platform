package controllers;
/*
 * TODO: Scroll zoom in has some weird functionality when using a trackpad
 *     : Fix this god forsaken volume bar chart
 *     : Get our category axis to take localdatetimes
 *     : Make flush a lil' more generic
 *
 * Reduce Memory Consumption - Peaks at around 1250MB Heap Size with effort (Lots of phase changes and erratic mouse movements)
 */

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.Cursor;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import utilities.*;
import technicalAnalysis.overlays.ExponentialMovingAverage;
import technicalAnalysis.overlays.LinearRegression;
import technicalAnalysis.overlays.SimpleMovingAverage;
import technicalAnalysis.overlays.VolumeWeightedAveragePrice;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Main logic for the base application
 * Houses the graphs, A real-time tableView of TickerSymbols, all the buttons that get the user to utilities Stages
 * */
public class Controller extends utilities.DateAxis {

    //Declare FXML Variables
    @FXML
    private StackPane stackPane;//highest parent in fxml
    @FXML
    private TableColumn<Object[], BigDecimal> priceTC, bidTC, askTC, percentChangeTC;
    @FXML
    private TableColumn<Object[], Long> currentVolumeTC;
    @FXML
    private TableColumn<Object[], String> symbolTC, symbolName;
    @FXML
    private TableView<Object[]> tableView;
    @FXML
    private LineChartWithMarkers<LocalDateTime, Number> lineChart =
        new LineChartWithMarkers<>(new DateAxis(LocalDateTime.now().minus(5, ChronoUnit.YEARS),
            LocalDateTime.now()), new NumberAxis());
    @FXML
    private BarChartWithMarkers<String, Number> volumeBarChart =
        new BarChartWithMarkers<>(new CategoryAxis(), new NumberAxis());
    @FXML
    private ToggleGroup cssThemes, timeFrame;
    @FXML
    private RadioMenuItem darkThemeCssRMI, lightThemeCssRMI, aquaThemeCssRMI;
    @FXML
    private Label netShares;
    @FXML
    private ColorPicker cPicker;
    @FXML
    private SplitPane chartSplitPane, tableSplitPane;
    @FXML
    private ProgressBar progressBar;

    //Declare Variables
    private final int COLUMN_1_MAP_NUM = 0;//Symbol
    private final int COLUMN_2_MAP_NUM = 1;//Price
    private final int COLUMN_3_MAP_NUM = 2;//% change since open
    private final int COLUMN_4_MAP_NUM = 3;//Current Volume
    private final int COLUMN_5_MAP_NUM = 4;//Bid
    private final int COLUMN_6_MAP_NUM = 5;//Ask
    private final int COLUMN_7_MAP_NUM = 6;//Equity Name
    private final ArrayList<Data<LocalDate, Number>> allVolumeData = new ArrayList<>(252*2);//Directly associated with TimeUnits
    //The order and metadata about the drawings and calls to redraw lineChart and volumeBarChart
    private final HashMap<String, Integer> orderedSymbolCalls = new HashMap<>();
    private final WatchListEditor watchListEditor = new WatchListEditor();
    private final String darkTheme = getClass().getResource("/cssFiles/DarkTheme.bss").toExternalForm();
    private final String lightTheme = getClass().getResource("/cssFiles/modena.bss").toExternalForm();

    //Declare Other Variables
    //private static String username;
    private static int id;
    private final String[] arrayOfData = populateDataForTableView();
    private boolean threwException = false; //Used when filtering
    //Differences are for limiting the amount of redraws which slow the program down and cost lots of memory
    private long timeDifferenceTV = 0;
    private long timeDifferenceSP = 0;
    private long timeDifferenceTT = 0;
    private long timeDifferenceZoom = 0;
    private long timeDifferencePan = 0;
    private long zoomDebouncer = 5;
    private final long splitPaneDebouncer = 75;
    private final long progressBarDebouncer = 5000;
    private User primaryUser;//The user that logged in

    //Declare Variables for DateAxis manipulation
    private LocalDateTime firstDate;//Oldest date available in the data
    private LocalDateTime lastDate;//Latest date available in the data
    private LocalDateTime lowerBoundDate = LocalDateTime.now();//Date shown on the left hand side of the axis
    private LocalDateTime upperBoundDate = LocalDateTime.now();//Date shown on the right hand side of the axis

    //Declare Variables for tableView and Graph manipulations
    //Color for the gradient on the top part of the charts
    private final Color topGradient = Color.color(1, 1, 1, .3);
    //Color for the gradient on the bottom part of the charts
    private final Color bottomGradient = Color.color(0, 0, 0, .3);
    //Color for the lineChart line and the volumeBarChart bars
    private final Color chartSeriesColor = Color.color(.953, .384, .176, 1);
    private final ColorPicker cp1 = new ColorPicker(topGradient);//For top gradient changing
    private final ColorPicker cp2 = new ColorPicker(bottomGradient);//For bottom gradient changing
    private final ColorPicker cp3 = new ColorPicker(chartSeriesColor);//For series color changing
    //Possible intervals of time to display the data
    private final Interval[] intervals = {Interval.DAILY, Interval.WEEKLY, Interval.MONTHLY};
    private Interval interval = intervals[0];//Default interval is daily
    private Object[][] userPositions;//All of the current positions of the user
    private String selectedSymbol;
    private int lowerIndex = 0, upperIndex;

    //Declare Mouse Variables
    private double mousePosX;
    private double mousePosY;

    //Declare UserEvent Variables
    //When holding Control, the speed at which User Events happen get multiplied by this
    private final double controlModifier = .5;
    //When holding Alt, the speed at which User Events happen get multiplied by this
    private final int altModifier = 2;

    /**
     * Default constructor so that we can throw the reference to the UI Thread around classes
     * NOTE: Must be public otherwise javaFX can't access it
     * */
    public Controller() {}

    /**
     * Setup all of the necessary components for the window to operate
     * (table filled, data graphed, other processes working on the background threads)
     * */
    @FXML
    public void initialize() throws SQLException {
        long start = System.currentTimeMillis();

        ((NumberAxis) volumeBarChart.getYAxis()).setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return (String.format("%2.0e", object.doubleValue()));
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        volumeBarChart.setBarGap(0);//Centers the bars on the tickLabels

        primaryUser = new User(id);

        primaryUser.setGroupID("Global");//FIXME Get last group id from config file

        populateTableView();

        tableView.getSelectionModel().select(0);
        tableView.getFocusModel().focusNext();

        //this gives the value in the selected cell:
        selectedSymbol = symbolTC.getCellData(0);

        orderedSymbolCalls.put(selectedSymbol, 0);

        //Display selected stock on graph with the default interval of Daily
        plotStockOnBothCharts(selectedSymbol, lineChart, interval);

        //Default lineChart to pan
        pan();

        //Other Extraneous Processes
        Platform.runLater(this::doExtraneousProcesses);

        System.out.println("Initialize time: " + (System.currentTimeMillis() - start));
    }

    /**
     * Background stuff that refreshes the tableView, makes sure that the gradient never ends,
     * and provides a limiter to the method calling
     * */
    private void doExtraneousProcesses(){
        //Ensure that the Gradient on lineChart never reaches the end and shows the user
        ObservableList<SplitPane.Divider> dividers = chartSplitPane.getDividers();
        for (SplitPane.Divider divider : dividers) {
            divider.positionProperty().addListener((observable, oldValue, newValue) -> {
                if (debouncer(timeDifferenceSP, splitPaneDebouncer))
                    return;
                if (!oldValue.equals(newValue)) {
                    changeGradient();
                    timeDifferenceSP = System.currentTimeMillis();
                }
            });
        }

        //Ensure that the Gradient on lineChart never reaches the end and shows the user
        ObservableList<SplitPane.Divider> dividers2 = tableSplitPane.getDividers();
        for (SplitPane.Divider divider : dividers2) {
            divider.positionProperty().addListener((observable, oldValue, newValue) -> {
                if (debouncer(timeDifferenceSP, splitPaneDebouncer))
                    return;
                if (!oldValue.equals(newValue)) {
                    changeGradient();
                    timeDifferenceSP = System.currentTimeMillis();
                }
            });
        }

        //Declare themes
        darkThemeCssRMI.setUserData("/cssFiles/DarkTheme.bss");
        aquaThemeCssRMI.setUserData("/cssFiles/AquaTheme.bss");
        lightThemeCssRMI.setUserData("/cssFiles/modena.bss");

        //Get the user's positions relative to their group
        try {
            userPositions = primaryUser.getOpenPositions();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Add the keyListeners
        final KeyCombination controlB = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);
        final KeyCombination controlS = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        final KeyCombination controlG = new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN);
        stackPane.getScene().setOnKeyTyped(event -> {
            try {
                if (controlB.match(event)) {
                    buyStock();
                } else if (controlS.match(event)) {
                    sellStock();
                } else if (controlG.match(event)) {
                    showGroups();
                } else if (!event.isControlDown() && !event.isAltDown() && !event.isShiftDown()) {
                    searchPanel(event.getCharacter());
                }
            }catch (Throwable throwable){
                throwable.printStackTrace();
            }
        });

        Runnable refreshTV = () -> {
            try {
                int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
                int[] visibleCells = getVisibleRows();
                if (visibleCells[0] == -1 || visibleCells[1] == -1) {
                    return;
                }
                ObservableList<Object[]> datas = tableView.getItems();
                String[] tickers = new String[visibleCells[1] - visibleCells[0] + 1];
                //PRIME FOR SIMD
                int p = 0;
                for (int i = visibleCells[0]; i <= visibleCells[1]; i++) {
                    tickers[p++] = datas.get(i)[COLUMN_1_MAP_NUM].toString();
                }
                Object[][] newData = updateRows(tickers);
                if (newData == null)
                    return;
                p = 0;
                for (int i = visibleCells[0]; i <= visibleCells[1]; i++) {
                    datas.set(i, newData[p++]);//FIXME indexOutOfBoundsException
                }
                tableView.getSelectionModel().select(selectedIndex);
            }catch (Throwable e){
                e.printStackTrace();
            }
        };

        ScheduledExecutorService exs = Executors.newScheduledThreadPool(1);
        exs.scheduleAtFixedRate(refreshTV, 10L, 10, SECONDS);
    }

    /**
     * Shows the user a message that details the error that it takes in
     *
     * @param e The error that was thrown and needs to be shown to the user
     * */
    private void error(Throwable e) {
        Platform.runLater(() -> {
            e.printStackTrace();
            final JDialog dialog = new JDialog();
            dialog.setAlwaysOnTop(true);
            JOptionPane.showMessageDialog(dialog, e.getCause().toString(), e.getMessage(), JOptionPane.ERROR_MESSAGE);
            dialog.dispose();
            e.printStackTrace();
        });
    }

    /**
     * Return a reference to the lineChart on the UI Thread for plotting on other classes
     * */
    LineChartWithMarkers<LocalDateTime, Number> getLineChart() {
        return lineChart;
    }

    /**
     * Return the interval that the UI Thread is currently using for plotting on classes
     * */
    Interval getInterval(){
        return interval;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*User Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Statically set the id of the user who logged in so that later
     * calls to the database won't have a null id
     * @param ID The id of the logged in user
     * */
    static void setID(int ID) {
        id = ID;
    }

    /**
     * Get the number of shares given the symbol as determined by the database
     * Set the label netShares to be the number of shares
     * @param selectedSymbol the symbol we are querying about for the number of positions
     *                       The number of shares that user has for the given symbol
     * */
    private void updateShares(String selectedSymbol) {
        if (userPositions.length != 0) {
            ArrayList<Integer> indices = new ArrayList<>();
            for (int i = 0; i < userPositions.length; i++){
                if (selectedSymbol.equals(userPositions[i][0])) {
                    indices.add(i);
                }
            }
            long num = 0L;
            for (Integer i : indices){
                num += (Long) userPositions[i][2];
            }
            netShares.setText(String.valueOf(num));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*GUI Manipulation Methods*/

    //////////////////////////////////////////////////////////////////////////////////////
    /*Movement Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Find the old and new mouse positions, whether alt or control is down,
     * and then send that data to the correct method
     *
     * @param e The new mouse position in the form of the MouseEvent
     * */
    private void handleMouse(MouseEvent e) {
        //Do x position calculations
        double mouseOldX = mousePosX;
        mousePosX = e.getX();

        //do y position calculations
        double mouseOldY = mousePosY;
        mousePosY = e.getY();

        //Calculate the modifier
        double modifier = 1.0;
        if (e.isControlDown()) {
            modifier = controlModifier;
        }
        else if (e.isAltDown()) {
            modifier = altModifier;
        }

        if (e.isPrimaryButtonDown()) //Left click: pan the charts
            pan(modifier, mousePosX - mouseOldX);

        else
            zoom(mousePosY - mouseOldY, modifier);


        alterVolumeBCData(false);
        volumeBarChart.autosize();

        Color color1 = cp3.getValue();
        String rgb = String.format("%d, %d, %d",
            (int) (color1.getRed() * 255),
            (int) (color1.getGreen() * 255),
            (int) (color1.getBlue() * 255));
        for(Node n:volumeBarChart.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: rgba(" + rgb + ", 1.0);");
        }
    }

    /**
     * Whenever the user scrolls, take the event and convert to deltas and then send it to either zoom or zoom
     *
     * @param e The scroll event
     * */
    private void handleScroll(ScrollEvent e) {
        //Calculate the modifier
        double modifier = 1.0;
        if (e.isControlDown()) {
            modifier = controlModifier;
        }
        else if (e.isAltDown()) {
            modifier = altModifier;
        }

        zoom(e.getDeltaY(), modifier);
        alterVolumeBCData(false);

        //Fill the volumeBarChart with the right colored bars
        Color color1 = cp3.getValue();
        String rgb = String.format("%d, %d, %d",
            (int) (color1.getRed() * 255),
            (int) (color1.getGreen() * 255),
            (int) (color1.getBlue() * 255));
        for(Node n : volumeBarChart.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: rgba(" + rgb + ", 1.0);");
        }
        volumeBarChart.autosize();
    }

    /**
     * Change the lowerBoundDate and the upperBoundDate to be older (Move backwards in time) while maintaing that the
     * lowerBoundDate never is below the firstDate of the data
     *
     * @param modifier The amount that the lower/upperBoundDate should be changed
     * */
    private void pan(double modifier, double delta) {
        long panDebouncer = 5;
        if (!debouncer(timeDifferencePan, panDebouncer)) return;
        timeDifferencePan = System.currentTimeMillis();
        //Change the LowerBound & UpperBound/*(lowerBoundDate.getTime().getTime() +*/
        LocalDateTime LBCandidate = lowerBoundDate.plus((int) (delta * modifier), ChronoUnit.DAYS);
        LocalDateTime UBCandidate = upperBoundDate.plus((int) (delta * modifier), ChronoUnit.DAYS);
        Duration duration = Duration.between(lowerBoundDate, upperBoundDate);

        //Check to see if the LowerBound date is before the firstDate of the data, else, set it to the firstDate of the data
        if (LBCandidate.isBefore(firstDate)) {
            //The new LowerBoundDate is before the firstDate possible, with data, so we set the lowerBoundDate to firstDate
            lowerBoundDate = firstDate;
            upperBoundDate = firstDate.plus(duration);
        } else if (UBCandidate.isAfter(lastDate)) {
            //The new upperBoundDate is after the lastDate possible, with data, so we set the upperBoundDate to lastDate
            upperBoundDate = lastDate;
            lowerBoundDate = lastDate.minus(duration);
        } else{
            lowerBoundDate = LBCandidate;
            upperBoundDate = UBCandidate;
        }
        ((DateAxis) lineChart.getXAxis()).setLowerBound(lowerBoundDate);
        ((DateAxis) lineChart.getXAxis()).setUpperBound(upperBoundDate);

    }

    /**
     * Change both the lower and upperBoundDate to be closer to each other (Contracting time) while maintaining that
     * they are never switching places [lowerBoundDate < upperBoundDate] and never occurring at the same date
     * [lowerBoundDate != upperBoundDate]
     * NOTE: Called on by HandleScroll()
     *
     * @param modifier The multiplier that the amount change should be adjusted by
     * */
    private void zoom(double delta, double modifier) {
        long zoomDebouncer = 5;
        if (!debouncer(timeDifferenceZoom, zoomDebouncer)) return;
        timeDifferenceZoom = System.currentTimeMillis();

        LocalDateTime CLB = lowerBoundDate.plus((int) (delta * modifier), ChronoUnit.DAYS);
        LocalDateTime CUB = upperBoundDate.plus(-((int) (delta * modifier)), ChronoUnit.DAYS);
        if(CLB.isBefore(firstDate))
            CLB = firstDate;
        if(CUB.isAfter(lastDate))
            CUB = lastDate;
        if(CUB.isBefore(CLB)) {
            if(CLB.isEqual(firstDate))
                CUB = CLB.plus(10, ChronoUnit.DAYS);
            else if(CUB.isEqual(lastDate))
                CLB = CUB.minus(10, ChronoUnit.DAYS);
            else if(CLB.isAfter(lastDate)){
                CLB = lastDate.minus(10, ChronoUnit.DAYS);
                CUB = lastDate;
            } else if(CUB.isBefore(firstDate)){
                CUB = firstDate.plus(10, ChronoUnit.DAYS);
                CLB = lastDate;
            } else{
                CLB = firstDate;
                CUB = firstDate.plus(10, ChronoUnit.DAYS);
            }
        }
        if(!lowerBoundDate.isEqual(CLB)) {
            lowerBoundDate = CLB;
            ((DateAxis) lineChart.getXAxis()).setLowerBound(lowerBoundDate);

        }
        if(!upperBoundDate.isEqual(CUB)) {
            upperBoundDate = CUB;
            ((DateAxis) lineChart.getXAxis()).setUpperBound(upperBoundDate);
        }

    }

    private boolean debouncer(long difference, long allowance) {
        return System.currentTimeMillis() - difference > allowance;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Children Manipulation Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Change the shown bars on the volumeBarChart to contain only the bars within the
     * interval I [lowerBoundDate, upperBoundDate]
     * SPECIAL CASES:
     *          When [lowerBoundDate == firstDate] don't redraw
     *          When [upperBoundDate == lastDate] don't redraw
     *
     * @param changeSymbol The symbol that we are going to be gathering data for and
     *                     then projecting that data to the volumeBarChart
     * */
    private void alterVolumeBCData(boolean changeSymbol) {
        //To not excessively redraw the BarChart when both bounds are shown

        DateAxis xAxis = (DateAxis) lineChart.getXAxis();
        if (xAxis.getDisplayPosition(firstDate) == xAxis.getDisplayPosition(lowerBoundDate)
            || xAxis.getDisplayPosition(lastDate) == xAxis.getDisplayPosition(upperBoundDate)) return;

        ObservableList<Data<String, Number>> displayedVolumeData = FXCollections.observableArrayList();
        upperIndex = allVolumeData.size() - 1;
        lowerIndex = 0;

        //Find the lower and upper index that have the date value of lower and upperBoundDate
        //This will be the displayedVolumeData
        LocalDateTime date;
        for (int i = 0; i < allVolumeData.size(); i++) {
            date = allVolumeData.get(i).getXValue().atStartOfDay();
            if (date.isAfter(lowerBoundDate)) {
                lowerIndex = i;
                break;
            }
        }

        for (int i = lowerIndex; i <= upperIndex; i++) {
            date = allVolumeData.get(i).getXValue().atStartOfDay();
            if (date.isAfter(upperBoundDate)) {
                upperIndex = i;
                break;
            }
        }

        //Add all of the data in between lowerBoundDate and upperBoundDate
        //Prime for SIMD
        for (int i = lowerIndex; i < upperIndex; i++) {
            displayedVolumeData.add(new Data<>(allVolumeData.get(i).getXValue().toString(), allVolumeData.get(i).getYValue()));
        }

        //Change the series
        //Prime for SIMD
        ObservableList<Series<String, Number>> volumeSeries = FXCollections.observableArrayList();
        volumeSeries.add(new Series<>(selectedSymbol + " Volume Series", displayedVolumeData));

        try {
            //noinspection unchecked
            volumeBarChart.setData(volumeSeries);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @FXML
    private void horizontalLine() {
        flush();
        lineChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            double ff = (double) lineChart.getYAxis().getValueForDisplay(translatedMousePos(e, lineChart).getY());
            //draw the marker
            Data<LocalDateTime, Number> horizontalMarker = new Data<>(null, ff);//XValue doesn't matter for HorizontalLines
            Color color = cPicker.getValue();
            lineChart.addHorizontalValueMarker(horizontalMarker, color);
        });
        //So that the user can make multiple lines in one episode, instead of having to re-click the toolBar
        lineChart.setCursor(Cursor.CROSSHAIR);

        //Now the volumeBarChart
        volumeBarChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            Double ff = (Double) volumeBarChart.getYAxis().getValueForDisplay(translatedMousePos(e, volumeBarChart).getY());
            //draw the marker
            Data<String, Number> horizontalMarker = new Data<>(null, ff);//XValue doesn't matter for HorizontalLines
            Color color = cPicker.getValue();
            volumeBarChart.addHorizontalValueMarker(horizontalMarker, color);
        });
        //So that the user can make multiple lines in one episode, instead of having to re-click the toolBar
        volumeBarChart.setCursor(Cursor.CROSSHAIR);
    }

    @FXML
    private void verticalLine() {
        flush();
        lineChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            LocalDateTime ff = ((DateAxis) lineChart.getXAxis()).getValueForDisplay(translatedMousePos(e, lineChart).getX());
            //draw the marker
            Data<LocalDateTime, Number> verticalMarker = new Data<>(ff, null);//YValue doesn't matter for VerticalLines
            Color color = cPicker.getValue();
            lineChart.addVerticalValueMarker(verticalMarker, color);
        });
        //So that the user can make multiple lines in one episode, instead of having to reclick the toolBar
        lineChart.setCursor(Cursor.CROSSHAIR);

        volumeBarChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            String ff = (String) volumeBarChart.getXAxis().getValueForDisplay(translatedMousePos(e, volumeBarChart).getX());
            //draw the marker
            Data<String, Number> verticalMarker = new Data<>(ff, null);//YValue doesn't matter for VerticalLines
            Color color = cPicker.getValue();
            volumeBarChart.addVerticalValueMarker(verticalMarker, color);
        });
        //So that the user can make multiple lines in one episode, instead of having to reclick the toolBar
        volumeBarChart.setCursor(Cursor.CROSSHAIR);
    }

    @FXML
    private void freeFormLine() {
        flush();
        Color color = cPicker.getValue();
        lineChart.setCursor(Cursor.CROSSHAIR);
        //Clear points on mousePressed otherwise the polyLines will be extensions of past polyLines
        lineChart.setOnMouseReleased(e -> {
            Point p = translatedMousePos(e, lineChart);
            Data<LocalDateTime, Number> data = new Data<>((LocalDateTime) lineChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) lineChart.getYAxis().getValueForDisplay(p.getY()));
            lineChart.addFreeFormLine(data, Color.TRANSPARENT);});
        lineChart.setOnMouseDragged(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY)) {
                return;
            }
            lineChart.setCursor(Cursor.CROSSHAIR);
            Point p = translatedMousePos(e, lineChart);
            Data<LocalDateTime, Number> data = new Data<>((LocalDateTime) lineChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) lineChart.getYAxis().getValueForDisplay(p.getY()));
            lineChart.addFreeFormLine(data, color);
        });

        volumeBarChart.setCursor(Cursor.CROSSHAIR);
        //Clear points on mousePressed otherwise the polyLines will be extensions of past polyLines
        volumeBarChart.setOnMouseReleased(e -> {
            Point p = translatedMousePos(e, volumeBarChart);
            Data<String, Number> data = new Data<>((String) volumeBarChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) volumeBarChart.getYAxis().getValueForDisplay(p.getY()));
            volumeBarChart.addFreeFormLine(data, Color.TRANSPARENT);
        });
        volumeBarChart.setOnMouseDragged(e -> {

            Point p = translatedMousePos(e, volumeBarChart);
            Data<String, Number> data = new Data<>((String) volumeBarChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) volumeBarChart.getYAxis().getValueForDisplay(p.getY()));
            volumeBarChart.addFreeFormLine(data, color);
        });
    }

    @FXML
    private void addText() {
        flush();
        lineChart.setCursor(Cursor.TEXT);
        lineChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            Point p = translatedMousePos(e, lineChart);
            Color color = cPicker.getValue();
            String message = JOptionPane.showInputDialog("Input your text");
            Data<LocalDateTime, Number> data = new Data<>((LocalDateTime) lineChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) lineChart.getYAxis().getValueForDisplay(p.getY()));
            lineChart.addText(data, message, color);
        });

        volumeBarChart.setCursor(Cursor.TEXT);
        volumeBarChart.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.SECONDARY))
                return;
            Point p = translatedMousePos(e, volumeBarChart);
            Color color = cPicker.getValue();
            String message = JOptionPane.showInputDialog("Input your text");
            Data<String, Number> data = new Data<>((String) volumeBarChart.getXAxis().getValueForDisplay(p.getX()),
                (Number) volumeBarChart.getYAxis().getValueForDisplay(p.getY()));
            volumeBarChart.addText(data, message, color);
        });
    }

    @FXML
    private void pan() {
        flush();
        lineChart.setOnMousePressed(e -> {
            lineChart.setCursor(Cursor.CLOSED_HAND);
            mousePosX = e.getX();
            mousePosY = e.getY();
        });

        lineChart.setOnMouseReleased(e -> lineChart.setCursor(Cursor.OPEN_HAND));

        lineChart.setOnScroll(this::handleScroll);

        lineChart.setOnMouseDragged(this::handleMouse);

        lineChart.setCursor(Cursor.OPEN_HAND);

        volumeBarChart.setOnMousePressed(e -> {
            volumeBarChart.setCursor(Cursor.CLOSED_HAND);
            mousePosX = e.getX();
            mousePosY = e.getY();
        });

        volumeBarChart.setOnMouseReleased(e -> volumeBarChart.setCursor(Cursor.OPEN_HAND));

        volumeBarChart.setOnScroll(this::handleScroll);

        volumeBarChart.setOnMouseDragged(this::handleMouse);

        volumeBarChart.setCursor(Cursor.OPEN_HAND);
    }

    @FXML
    private void erase() {
        flush();
        lineChart.setCursor(Cursor.TEXT);
        ArrayList<Double> points = new ArrayList<>();
        lineChart.setOnMouseDragged(event -> {
            Point p = translatedMousePos(event, lineChart);
            points.add(p.getX());
            points.add(p.getY());
        });
        lineChart.setOnMouseReleased(event -> {
            ArrayList<List<ObservableList<Data<LocalDateTime, Number>>>> children = lineChart.getDrawnChildren();
            int index = pastCalls();
            Polyline polyline = new Polyline();
            polyline.getPoints().addAll(points);
            children.get(index).forEach(p -> p.removeIf(d -> {
                if (d.getNode().intersects(polyline.getBoundsInLocal())){
                    lineChart.removeNode(d.getNode());
                    return true;
                }
                return false;
            }));
            lineChart.setTickerCalls(children);
        });
    }

    @FXML
    final void linearRegression() {
        LinearRegression linearRegression;
        //noinspection unchecked
        linearRegression = new LinearRegression(((Series<Date, Number>) lineChart.getData().get(0)).getData());
        double slope = linearRegression.slope();
        double intercept = linearRegression.intercept();
        double prediction = linearRegression.predict(allVolumeData.size());//tomorrow
        double r2 = linearRegression.R2();
        double r = linearRegression.R();
        System.out.println("Intercept: " + intercept);
        System.out.println("Slope: " + slope);
        System.out.println("Prediction for tomorrow: " + prediction);
        System.out.println("RSquared: " + r2);
        System.out.println("R: " + r);
        //plot line because we have intercept and the predicted value for today
        @SuppressWarnings("unchecked") Series<LocalDateTime, Number> series = (Series<LocalDateTime, Number>) lineChart.getData().get(0);
        Data<LocalDateTime, Number> d1 = new Data<>( series.getData().get(0).getXValue(), intercept);
        Data<LocalDateTime, Number> d2 = new Data<>( series.getData().get(series.getData().size() - 1).getXValue(), prediction);
        lineChart.addLinearRegression(d1, d2, cPicker.getValue());
    }

    @FXML
    final void simpleMovingAverage() throws NumberFormatException {
        @SuppressWarnings("unchecked") ObservableList<Data<LocalDateTime, Number>> data = ((Series) lineChart.getData().get(0)).getData();
        int per = Integer.parseInt(JOptionPane.showInputDialog("What would you like the period to be?"));
        List<Data<LocalDateTime, Number>> newdata = data.subList(lowerIndex, upperIndex + 1);
        ArrayList<Data<LocalDateTime, Number>> movingAverages = new ArrayList<>();
        SimpleMovingAverage obj = new SimpleMovingAverage(per);
        for (Data<LocalDateTime, Number> aNewdata : newdata) {
            BigDecimal y = (BigDecimal) aNewdata.getYValue();
            obj.addData(y.doubleValue());
            movingAverages.add(new Data<>(aNewdata.getXValue(), obj.getMean()));
        }
        List<Data<LocalDateTime, Number>> SMA = movingAverages.subList(per, movingAverages.size());
        lineChart.addSMA(SMA, cPicker.getValue());
    }

    @FXML
    final void exponentialMovingAverage() throws NumberFormatException {
        ExponentialMovingAverage EMA = new ExponentialMovingAverage(Double.parseDouble(JOptionPane.showInputDialog("What is the Alpha?")));
        @SuppressWarnings("unchecked") ObservableList<Data<LocalDateTime, Number>> data = ((Series) lineChart.getData().get(0)).getData();
        List<Data<LocalDateTime, Number>> newdata = data.subList(lowerIndex, upperIndex + 1);
        ArrayList<Data<LocalDateTime, Number>> movingAverages = new ArrayList<>();
        for (Data<LocalDateTime, Number> x : newdata) {
            BigDecimal y = (BigDecimal) x.getYValue();
            movingAverages.add(new Data<>(x.getXValue(), EMA.average(y.doubleValue())));
        }
        lineChart.addEMA(movingAverages, cPicker.getValue());
    }

    @FXML
    final void vwap() throws NumberFormatException {
        //Get the window and the period
        @SuppressWarnings("unchecked") ObservableList<Data<LocalDateTime, Number>> data = ((Series) lineChart.getData().get(0)).getData();
        int per = Integer.parseInt(JOptionPane.showInputDialog("What would you like the period to be?"));
        List<Data<LocalDateTime, Number>> newdata = data.subList(lowerIndex, upperIndex + 1);

        ArrayList<Data<LocalDateTime, Number>> VWAPS = new ArrayList<>();
        VolumeWeightedAveragePrice obj = new VolumeWeightedAveragePrice(per);

        int i = lowerIndex;
        for (Data<LocalDateTime, Number> x : newdata) {
            BigDecimal y = (BigDecimal) x.getYValue();
            obj.addData(y.doubleValue(), ((long) allVolumeData.get(i).getYValue()));
            VWAPS.add(new Data<>(x.getXValue(), obj.calculateVWAP()));
            i++;
        }
        lineChart.addVWAP(VWAPS, cPicker.getValue());
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Children Manipulation Util Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    private Point translatedMousePos(MouseEvent e, LineChartWithMarkers<LocalDateTime, Number> chart) {
        double mouseY = e.getSceneY();//Gets the XPos relative to the scene
        //Need to find lineChart's YPosition relative to the scene and minus that from mouseY
        double sceneInset = Math.floor(chart.localToScene(chart.getBoundsInLocal()).getMinY());//This finds the top of the chart relative to the scene
        mouseY -= sceneInset;
        //The value should also take into account the paddings, for if the MouseEvent is not on the Charts visible node
        //area, but the X & Y would be positive, it will go onto the visible node at the X & Y
        double topPadding = Math.floor(chart.getPadding().getTop());
        mouseY -= topPadding;
        Bounds txRectBounds = chart.boundsInParentProperty().getValue();
        mouseY -= txRectBounds.getMinY();
        double topInset = Math.floor(chart.getInsets().getTop());
        mouseY -= topInset;

        double mouseX = e.getSceneX();//Gets the XPos relative to the scene
        //Need to find lineChart's XPosition relative to the scene and minus that from mouseX
        double sceneInsetX = Math.floor(chart.localToScene(chart.getBoundsInLocal()).getMinX());
        mouseX -= sceneInsetX;
        double leftInset = Math.floor(chart.getInsets().getLeft());
        mouseX -= leftInset;
        mouseX -= Math.floor(chart.getBaselineOffset());

        //Round and then integerize
        return new Point((int) Math.floor(mouseX),(int) Math.floor(mouseY) );
    }

    private Point translatedMousePos(MouseEvent e, BarChartWithMarkers<String, Number> chart) {
        double mouseY = e.getSceneY();//Gets the XPos relative to the scene
        //Need to find lineChart's YPosition relative to the scene and minus that from mouseY
        double sceneInset = Math.floor(chart.localToScene(chart.getBoundsInLocal()).getMinY());//This finds the top of the chart relative to the scene
        mouseY -= sceneInset;
        //The value should also take into account the paddings, for if the MouseEvent is not on the Charts visible node
        //area, but the X & Y would be positive, it will go onto the visible node at the X & Y
        //double topPadding = Math.floor(chart.getPadding().getTop());
        //mouseY += topPadding;
        double topInset = Math.floor(chart.getInsets().getTop());
        mouseY += topInset;
        mouseY -= Math.floor(chart.getBaselineOffset());

        double mouseX = e.getSceneX();//Gets the XPos relative to the scene
        //Need to find lineChart's XPosition relative to the scene and minus that from mouseX
        double sceneInsetX = Math.floor(chart.localToScene(chart.getBoundsInLocal()).getMinX());
        mouseX -= sceneInsetX;
        double leftInset = Math.floor(chart.getInsets().getLeft());
        mouseX -= leftInset;
        double leftPadding = Math.floor(chart.getPadding().getLeft());
        mouseX -=  leftPadding*2;

        //Round and then integerize
        return new Point((int) Math.floor(mouseX),(int) Math.floor(mouseY) );
    }

    private void flush() {
        lineChart.setOnMousePressed(Event::consume);
        lineChart.setOnMouseReleased(Event::consume);
        lineChart.setOnMouseClicked(Event::consume);
        lineChart.setOnMouseDragged(Event::consume);
        lineChart.setOnMouseDragEntered(Event::consume);
        lineChart.setOnMouseDragExited(Event::consume);
        lineChart.setOnMouseDragReleased(Event::consume);
        lineChart.setOnMouseEntered(Event::consume);
        lineChart.setOnMouseExited(Event::consume);
        lineChart.setOnMouseMoved(Event::consume);
        lineChart.setOnMouseDragOver(Event::consume);
        lineChart.setCursor(Cursor.DEFAULT);
        volumeBarChart.setOnMousePressed(Event::consume);
        volumeBarChart.setOnMouseReleased(Event::consume);
        volumeBarChart.setOnMouseClicked(Event::consume);
        volumeBarChart.setOnMouseDragged(Event::consume);
        volumeBarChart.setOnMouseDragEntered(Event::consume);
        volumeBarChart.setOnMouseDragExited(Event::consume);
        volumeBarChart.setOnMouseDragReleased(Event::consume);
        volumeBarChart.setOnMouseEntered(Event::consume);
        volumeBarChart.setOnMouseExited(Event::consume);
        volumeBarChart.setOnMouseMoved(Event::consume);
        volumeBarChart.setOnMouseDragOver(Event::consume);
        volumeBarChart.setCursor(Cursor.DEFAULT);
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Graph Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    //FIXME
    final void plotStockOnBothCharts(String symbol, LineChartWithMarkers<LocalDateTime, Number> LineChart, Interval interval) {
        try {
            /*
             * Get the historicalQuotes data for the selected symbol, symbol and create the containers that will hold
             * each of the pertinent values for the symbol:
             * price
             * date
             * volume
             */
            if (symbol == null) return;
            List<HistoricalQuote> historicalQuotes = YahooFinance.get(symbol).getHistory(interval);
            firstDate = ZonedDateTime.ofInstant(historicalQuotes.get(0).getDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDateTime();
            lastDate = ZonedDateTime.ofInstant(historicalQuotes.get(historicalQuotes.size() - 1).getDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDateTime();
            BigDecimal[] price = new BigDecimal[historicalQuotes.size()];
            LocalDateTime[] date = new LocalDateTime[historicalQuotes.size()];
            long[] volume = new long[historicalQuotes.size()];

            /*
            Assign each value from the list of the HistoricalQuotes to the corresponding ArrayList.
            Update the lowest and highest price values for the range when creating the graph.
            */
            for (int i = 0; i < historicalQuotes.size(); i++) {
                HistoricalQuote hq = historicalQuotes.get(i);
                if (hq != null) {
                    price[i] = hq.getClose() != null ? hq.getClose() : new BigDecimal(1);
                    date[i] = ZonedDateTime.ofInstant(hq.getDate().getTime().toInstant(), ZoneId.systemDefault()).toLocalDateTime();
                    volume[i] = hq.getVolume() != null ? hq.getVolume() : 1;
                }
            }

            /*
            Create the series that will be the data for the charts
            First the price and then the volume series
            */
            ObservableList<Data<LocalDateTime, Number>> volumeData = FXCollections.observableArrayList();
            ObservableList<Data<LocalDateTime, Number>> priceData = FXCollections.observableArrayList();

            //Prime SIMD
            for (int i = 0; i < price.length; i++) {
                priceData.add(new Data<>(date[i], price[i]));
            }

            //Prime SIMD
            for (int i = 0; i < volume.length; i++) {
                volumeData.add(new Data<>(date[i], volume[i]));
            }

            //noinspection unchecked
            LineChart.getData().setAll(new Series<>(priceData));

            //Make allVolumeData take LocalDate instead of LocalDateTime for better looking tickMarkers
            Object[] f = volumeData.toArray();
            if (f.length != 0) {
                transfer(f);
            }
            alterVolumeBCData(true);

            format(LineChart, symbol);

            lineChart.changeNodes(pastCalls());
            volumeBarChart.changeNodes(pastCalls());

            changeColors(cp3.getValue());

        } catch (IOException | NullPointerException e) {
            error(e);
        }
    }

    //FIXME
    private void format(LineChartWithMarkers<LocalDateTime, Number> LineChart, String symbol) {
        LineChart.setTitle(symbol);
        LineChart.getYAxis().setLabel("Price (USD)");

        lowerBoundDate = lowerBoundDate.minus(1, ChronoUnit.YEARS);

        ((DateAxis) LineChart.getXAxis()).setLowerBound(lowerBoundDate);
        ((DateAxis) LineChart.getXAxis()).setUpperBound(lastDate);
        LineChart.getXAxis().setAutoRanging(false);
    }

    //FIXME
    private void transfer(Object[] f){
        //allVolumeData must be cleared or it will have past symbols data in it, which will lead
        //to a IllegalOperationError: Duplicate Children Added
        allVolumeData.clear();
        for (Object h : f) {
            Number y = ((Data<LocalDateTime, Number>) h).getYValue();
            LocalDate ld = ((Data<LocalDateTime, Number>) h).getXValue().toLocalDate();
            allVolumeData.add(new Data<>(ld, y));
        }
    }

    @FXML
    final void changeTheme() {
        Parent root = stackPane.getScene().getRoot();
        root.getStylesheets().clear();
        String selectedToggle = cssThemes.getSelectedToggle().getUserData().toString();

        switch (selectedToggle) {
            case "/cssFiles/DarkTheme.bss":
                if (!root.getStylesheets().contains(darkTheme)) {
                    root.getStylesheets().add(darkTheme);
                }
                break;
            case "aquaThemeCssRMI":
                break;
            case "/cssFiles/modena.bss":
                if (!root.getStylesheets().contains(lightTheme)) {
                    root.getStylesheets().add(lightTheme);
                }
                break;
            default:

        }
    }

    @FXML
    private void gradientBackground() {
        BorderPane borderPane = new BorderPane();
        Button b = new Button("Click to finalize");
        cp1.setTooltip(new Tooltip("Top of the chart gradient color"));
        cp2.setTooltip(new Tooltip("Bottom of the chart gradient color"));

        borderPane.setLeft(cp1);
        borderPane.setRight(cp2);

        borderPane.setCenter(b);
        Scene scene = new Scene(borderPane, 500, 75);
        String selectedToggle = cssThemes.getSelectedToggle().getUserData().toString();
        scene.getStylesheets().add(selectedToggle);
        Stage stage = new Stage();
        stage.setScene(scene);
        b.setOnMouseClicked(e -> {
            changeGradient();
            stage.close();
        });
        stage.show();
    }

    @FXML
    private void changeGradient() {
        lineChart.setColors(cp1.getValue(), cp2.getValue());
        volumeBarChart.setColors(cp1.getValue(), cp2.getValue());
    }

    @FXML
    private void changeChartColor(){
        BorderPane borderPane = new BorderPane();
        Button b = new Button("Click to finalize");
        cp3.setTooltip(new Tooltip("Chart data color"));
        borderPane.setLeft(cp3);
        borderPane.setRight(b);
        Scene scene = new Scene(borderPane, 500, 75);
        String selectedToggle = cssThemes.getSelectedToggle().getUserData().toString();
        scene.getStylesheets().add(selectedToggle);
        Stage stage = new Stage();
        stage.setScene(scene);
        b.setOnMouseClicked(e -> {
            changeColors(cp3.getValue());
            stage.close();
        });
        stage.show();
    }

    private void changeColors(Color color){
        Node line = ((Series)lineChart.getData().get(0)).getNode().lookup(".chart-series-line");
        String rgb = String.format("%d, %d, %d",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
        line.setStyle("-fx-stroke: rgba(" + rgb + ", 1.0);");
        for(Node n:volumeBarChart.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: rgba(" + rgb + ", 1.0);");
        }
    }

    @FXML
    private void changeInterval(){
        String identifier = (String) timeFrame.getSelectedToggle().getUserData();
        switch (identifier){
            case "Minute":
                break;
            case "Fifteen":
                break;
            case "Hourly":
                break;
            case "Daily":
                interval = intervals[0];
                plotStockOnBothCharts(selectedSymbol, lineChart, interval);
                break;
            case "Weekly":
                interval = intervals[1];
                plotStockOnBothCharts(selectedSymbol, lineChart, interval);
                break;
            case "Monthly":
                interval = intervals[2];
                plotStockOnBothCharts(selectedSymbol, lineChart, interval);
                break;
            case "Quarterly":
                break;
            case "Yearly":
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Table Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get 25 Stocks from each letter to populate the grid
     *
     * @return An array of stocks that will show up on the grid
     */
    private String[] populateDataForTableView() {
        ArrayList<String> stockSymbols = new ArrayList<>();
        String[][] data = SearchController.getTables();
        for (int i = 0; i < 26; i++) {
            int start = new Random().nextInt(data[i].length/2);
            int end = start+25 >= data[i].length ? data[i].length : start+25;
            stockSymbols.addAll(Arrays.asList(data[i]).subList(start, end));
        }

        //Convert the ArrayList to an array for space efficiency
        return stockSymbols.toArray(new String[0]);
    }

    private void populateTableView() {
        setValueFactories();

        //This will be the starting amount of items in the Table
        Object[][] data = generateDataInArray(arrayOfData);

        //Give the tableView the data
        tableView.getItems().setAll(data);

        //Set the columns
        //noinspection unchecked
        tableView.getColumns().setAll(symbolTC, priceTC, percentChangeTC, currentVolumeTC, bidTC, askTC, symbolName);
    }

    private void setValueFactories(){
        //Set the CellValueFactory values for each TableColumn--it will be the Column's Key
        symbolTC.setCellValueFactory(p -> new SimpleStringProperty((p.getValue()[COLUMN_1_MAP_NUM]).toString()));
        priceTC.setCellValueFactory(p -> new SimpleObjectProperty<>((BigDecimal) p.getValue()[COLUMN_2_MAP_NUM]));
        percentChangeTC.setCellValueFactory(p -> new SimpleObjectProperty<>((BigDecimal) p.getValue()[COLUMN_3_MAP_NUM]));
        percentChangeTC.setCellFactory(column -> new TableCell<Object[], BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty); //This is mandatory
                if (!isEmpty()) {
                    // Get fancy and change color based on data
                    if (item.doubleValue() < 0.0)
                        this.setTextFill(Color.RED);
                    else if (item.doubleValue() > 0.0)
                        this.setTextFill(Color.GREEN);
                    else
                        this.setTextFill(Color.LIGHTGRAY);
                    setText(item.toString());
                }
            }
        });

        currentVolumeTC.setCellValueFactory(p -> new SimpleObjectProperty<>((Long)p.getValue()[COLUMN_4_MAP_NUM]));
        bidTC.setCellValueFactory(p -> new SimpleObjectProperty<>((BigDecimal)p.getValue()[COLUMN_5_MAP_NUM]));
        askTC.setCellValueFactory(p -> new SimpleObjectProperty<>((BigDecimal) p.getValue()[COLUMN_6_MAP_NUM]));
        symbolName.setCellValueFactory(p -> {
            if (p == null || p.getValue() == null || p.getValue()[COLUMN_7_MAP_NUM] == null)
                return new SimpleStringProperty("");
            else
                return new SimpleStringProperty((p.getValue()[COLUMN_7_MAP_NUM]).toString());
        });//Only ever get errors on this one. . .
    }

    private Object[][] generateDataInArray(String[] ArrayOfStockSymbols) {
        Object[][] allData;

        try {
            Map<String, Stock> stockSymbolMap = YahooFinance.get(ArrayOfStockSymbols, false);//a LIMIT of 100 is too large and throws a 431 response
            String[] keySet = stockSymbolMap.keySet().toArray(new String[0]);

            for (int i = stockSymbolMap.size() - 1; i >= 0; i--) {
                try {
                    StockQuote sq = stockSymbolMap.get(keySet[i]).getQuote();

                    if (threwException) {
                        threwException = false;
                        stockSymbolMap.remove(keySet[i]);
                    }

                    //Remove the exception symbol
                    //If it does pass all three, it gets removed
                    //Sometimes an exception is thrown in the last parameter
                    else if (sq.getAsk() == null || sq.getBid() == null || stockSymbolMap.get(keySet[i]).getName().isEmpty()) {
                        stockSymbolMap.remove(keySet[i]);
                    }
                } catch (Exception e) {
                    threwException = true;
                    i++;
                }
            }

            //Update keySet, because we removed values
            keySet = stockSymbolMap.keySet().toArray(new String[0]);

            //Assign the Values that will go into each TableColumn
            Stock s;

            allData = new Object[keySet.length][7];

            for (int i = 0; i < stockSymbolMap.size(); i++) {
                //Assign the Stock s to the Stock object from the keySet[i]: go through each Stock symbol in keySet
                s = stockSymbolMap.get(keySet[i]);

                /*Get the data from the stock for each value of the TableColumns*/
                //Get the Symbol of the selected stock
                allData[i][COLUMN_1_MAP_NUM] = keySet[i];
                //Get the price of the selected stock
                allData[i][COLUMN_2_MAP_NUM] = s.getQuote().getPrice();
                //Get the Change in Percent (relative to last close) of the selected stock
                allData[i][COLUMN_3_MAP_NUM] = s.getQuote().getChangeInPercent();
                //Get the volume of the selected stock
                allData[i][COLUMN_4_MAP_NUM] = s.getQuote().getVolume();
                //Get the Bid of the selected stock
                allData[i][COLUMN_5_MAP_NUM] = s.getQuote().getBid();
                //Get the Ask of the selected stock
                allData[i][COLUMN_6_MAP_NUM] = s.getQuote().getAsk();
                //Get the company name of the stock
                allData[i][COLUMN_7_MAP_NUM] = s.getName();
            }
        } catch (IOException e) {
            error(e);
            return new Object[0][0];
        }
        return allData;
    }

    @FXML
    private void newStockPicked() {
        long TVDebouncer = 1000;
        if (debouncer(timeDifferenceTV, TVDebouncer)) {
            try {
                if (tableView.getSelectionModel() == null) return;
                int tableIndex  = tableView.getSelectionModel().getSelectedIndex();
                if (tableIndex < 0) return;
                selectedSymbol = symbolTC.getCellData(tableIndex);
                if (selectedSymbol == null) return;
                //Check to see if the symbol of the selected cell is different than the symbol of what is already charted
                if (!selectedSymbol.equals(lineChart.getTitle())) {
                    format();
                    plotStockOnBothCharts(selectedSymbol, lineChart, interval);
                    Platform.runLater(() -> {
                        try {
                            updateShares(selectedSymbol);
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                error(e);
            }
            lineChart.setCursor(Cursor.OPEN_HAND);
            timeDifferenceTV = System.currentTimeMillis();
        }
        else {
            tableView.getSelectionModel().clearSelection();
        }
    }

    private int pastCalls(){
        //The newly selected symbol is not displayed, check to see if has been previously displayed
        Integer index = orderedSymbolCalls.get(lineChart.getTitle());
        if (index == null) {
            //This symbol hasn't been displayed, add it to orderedSymbolCalls
            orderedSymbolCalls.put(lineChart.getTitle(), orderedSymbolCalls.size());
            return orderedSymbolCalls.size()-1;
        }
        return index;
    }

    private void format(){
        volumeBarChart.getData().clear();
        lowerBoundDate = LocalDateTime.now();
        upperBoundDate = LocalDateTime.now();
        lineChart.getData().clear();
        lineChart.setTitle(selectedSymbol);
    }

    private Object[][] updateRows(String[] rows) {
        try {
            Map<String, Stock> stockSymbolMap = YahooFinance.get(rows, false);
            String[] keySet = stockSymbolMap.keySet().toArray(new String[0]);
            Stock s;
            Object[][] newData = new Object[keySet.length][7];
            for (int i = 0; i < stockSymbolMap.size(); i++) {
                //Assign the Stock s to the Stock object from the keySet[i]: go through each Stock symbol in keySet
                s = stockSymbolMap.get(keySet[i]);
                newData[i][COLUMN_1_MAP_NUM] = keySet[i];
                newData[i][COLUMN_2_MAP_NUM] = s.getQuote().getPrice();
                newData[i][COLUMN_2_MAP_NUM] = s.getQuote().getPrice();
                newData[i][COLUMN_3_MAP_NUM] = s.getQuote().getChangeInPercent();
                newData[i][COLUMN_4_MAP_NUM] = s.getQuote().getVolume();
                newData[i][COLUMN_5_MAP_NUM] = s.getQuote().getBid();
                newData[i][COLUMN_6_MAP_NUM] = s.getQuote().getAsk();
                newData[i][COLUMN_7_MAP_NUM] = s.getName();
            }
            //sort it so that the tableView does not change
            return sortRows(rows, newData);
        } catch (Exception e) {
            return null;
        }
    }

    private Object[][] sortRows(String[] discriminator, Object[][] unsorted){
        Object[][] sortedData = new Object[unsorted.length][7];
        for (int i = 0; i < discriminator.length; i++){
            //noinspection ForLoopReplaceableByForEach
            for (int u = 0; u < unsorted.length; u++){
                if (unsorted[u][0].toString().equals(discriminator[i])){
                    sortedData[i] = unsorted[u];
                }
            }
        }
        return sortedData;
    }

    private int[] getVisibleRows() {
        VirtualFlow<?> vf = (VirtualFlow<?>) ((TableViewSkin<?>) tableView.getSkin()).getChildren().get(1);
        return new int[]{vf.getFirstVisibleCell() != null ? vf.getFirstVisibleCell().getIndex() : 0,
            vf.getLastVisibleCell() != null ? vf.getLastVisibleCell().getIndex() : 0};
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Controller Hand-Off Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    private void showX(String resource, String title, int w, int h, Modality modality, double opacity, boolean resize){
        try {
            String selectedToggle = cssThemes.getSelectedToggle().getUserData().toString();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(resource));
            Parent root = fxmlLoader.load();
            Stage primaryStage = new Stage();
            primaryStage.setTitle(title);
            root.getStylesheets().add(selectedToggle);
            primaryStage.setScene(new Scene(root, w, h));
            primaryStage.setResizable(resize);
            primaryStage.setOnCloseRequest(e -> primaryStage.close());
            primaryStage.initModality(modality);
            primaryStage.setOpacity(opacity);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showGroups() {
        GroupController.getUserData(primaryUser);
        String selectedToggle = cssThemes.getSelectedToggle().getUserData().toString();
        GroupController.setupTheme(selectedToggle);
        showX("/fxmlFiles/GroupInterface.fxml", "Groups", 380, 500, Modality.NONE, 1.0, true);
    }

    private void searchPanel(String s) {
        SearchController.setup(s, this, 0);
        showX("/fxmlFiles/SearchPopUp.fxml", "Equities Trading Simulator", 430, 315, Modality.APPLICATION_MODAL, .90, true);
    }

    @FXML
    private void buyStock() {
        String symbol = lineChart.getTitle();
        Function<String, BigDecimal> func = aVoid -> {
            try {
                BigDecimal askPrice = YahooFinance.get(aVoid).getQuote().getAsk();
                boolean useAsk = askPrice.doubleValue() != 0;
                if (useAsk) {
                    return askPrice;
                }
                return YahooFinance.get(aVoid).getQuote().getPrice();
            } catch (IOException e){
                e.printStackTrace();
                return new BigDecimal(1000000000);
            }
        };
        BuySideController.setup(symbol, primaryUser, func);
        showX("/fxmlFiles/BuySecurity.fxml",
            "Buy",
            320,
            185,
            Modality.APPLICATION_MODAL,
            1.0,
            false);
    }

    @FXML
    private void sellStock() {//Uses the bid
        String symbol = lineChart.getTitle();
        Function<String, BigDecimal> func = aVoid -> {
            try {
                BigDecimal askPrice = YahooFinance.get(aVoid).getQuote().getBid();
                boolean useAsk = askPrice.doubleValue() != 0;
                if (useAsk) {
                    return askPrice;
                }
                return YahooFinance.get(aVoid).getQuote().getPrice();
            } catch (IOException e){
                e.printStackTrace();
                return new BigDecimal(1000000000);
            }
        };
        SellSideController.setup(symbol, primaryUser, func);
        showX("/fxmlFiles/SellSecurity.fxml",
            "Buy",
            320,
            185,
            Modality.APPLICATION_MODAL,
            1.0,
            false);
    }

    @FXML
    private void createWatchlist(){
        WatchListController.setStaticWLE(watchListEditor);
        showX("/fxmlFiles/WatchList.fxml", "Watchlist", 435, 553, Modality.NONE, 1.0, true);
    }

    @FXML
    private void showEditWatchlist(){
        showX("/fxmlFiles/WatchlistEditor.fxml", "Watchlist Editor", 435, 553, Modality.NONE, 1.0, true);
    }

    @FXML
    private void showScanner(){
        Scan.getController(this);
        showX("/fxmlFiles/Scanner.fxml", "Scanner", 230, 291, Modality.APPLICATION_MODAL, 1.0, true);
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /*Update Methods*/
    //////////////////////////////////////////////////////////////////////////////////////

    public void updateProgressBar(double progress){
        Platform.runLater( () -> {
            progressBar.setProgress(progress);
            //only update the tooltip every X*1000 milliseconds
            if (debouncer(timeDifferenceTT, progressBarDebouncer)) {
                progressBar.getTooltip().setText(progressBar.getId() + "; Current Progress: " +
                    Math.round(progress * 100) + "%");
                timeDifferenceTT = System.currentTimeMillis();
            }
        });
    }

    public void showProgressBar(boolean isVisible, String taskDescription){
        Platform.runLater( () -> {
            progressBar.setVisible(isVisible);
            progressBar.setId(taskDescription);
            progressBar.setTooltip(new Tooltip("Current task: "+ taskDescription + "; Current Progress: 0%"));
        });
    }
}
