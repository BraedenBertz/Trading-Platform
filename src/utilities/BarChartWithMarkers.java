package utilities;

import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * extends the JAVAFX BarChart to allow the addition of user designated overlays, technical indicators
 * plotted onto the graph or user defined drawings.
 * @param <X> The xAxis data type (Must be a categoryAxis
 * @param <Y> They yAxis data type
 */
public class BarChartWithMarkers<X, Y> extends BarChart{
    //This will keep track of the symbols
    private final ArrayList<List<ObservableList<Data<X, Y>>>> tickerCalls = new ArrayList<>();

    private int symbol = 0;
    /*
     * 0 = horizontal
     * 1 = vertical
     * 2 = ffnodes
     * 3 = textMarkers
     * */

    private final ObservableList<Polygon> polygons;
    private Color g1 = Color.color(1, 1, 1, .3), g2 = Color.color(0, 0, 0, .3);

    /**
     * Constructor for creating a BarChart that can add drawings/lines and utilities
     * miscellanea: needs @NamedArg for user in FXML code
     */
    public BarChartWithMarkers(@NamedArg("xAxis") Axis<X> xAxis,
                                @NamedArg("yAxis") Axis<Y> yAxis) {
        super(xAxis, yAxis);
        //To make them able to add/subtract/manipulate the nodes later in the code
        setup();
        polygons = FXCollections.observableArrayList(data -> new Observable[]{data.getProperties()});
        polygons.addListener((InvalidationListener) observable -> layoutPlotChildren());
    }

    /**
     * Setup all of the data containers for the nodes that the user will eventually input into them
     *
     */
    private void setup(){
        List<ObservableList<Data<X, Y>>> obsLists = new LinkedList<>();
        //To make them able to add/subtract/manipulate the nodes later in the code
        //Horizontal
        obsLists.add(FXCollections.observableArrayList(data -> new Observable[]{data.YValueProperty()}));
        obsLists.get(0).addListener((InvalidationListener) observable -> layoutPlotChildren());
        //Vertical
        obsLists.add(FXCollections.observableArrayList(data -> new Observable[]{data.XValueProperty()}));
        obsLists.get(1).addListener((InvalidationListener) observable -> layoutPlotChildren());
        //FFNodes
        obsLists.add(FXCollections.observableArrayList(data -> new Observable[]{data.YValueProperty()}));
        obsLists.get(2).addListener((InvalidationListener) observable -> layoutPlotChildren());
        //textMarkers
        obsLists.add(FXCollections.observableArrayList(data -> new Observable[]{data.YValueProperty()}));
        obsLists.get(3).addListener((InvalidationListener) observable -> layoutPlotChildren());
        tickerCalls.add(obsLists);
    }

    /**
     * Add a line to the the horizontalMarkers arrayList and then add the line to the plot's children
     *
     * @param marker A node point that give the X value and the Y value for where the node resides
     * @param color The color that this horizontal line will have on the plot
     */
    public void addHorizontalValueMarker(Data<X, Y> marker, Color color) {

        Objects.requireNonNull(marker, "the marker must not be null");

        //Check to see if the marker is already in the set of drawn objects, if yes, drawing it would be a redundancy
        if (tickerCalls.get(symbol).get(0).contains(marker)) return;
        Line line = new Line();//This is what we will add to the chart

        marker.setNode(line);
        line.setStroke(color);
        line.setOnMouseClicked(mouseEvent -> {
            //removes the text if the user rightClicks on it
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                removeHorizontalValueMarker(marker);
            }
        });
        getPlotChildren().add(line);
        tickerCalls.get(symbol).get(0).add(marker);
    }

    /**
     * Remove a line from the horizontalMarkers arraylist and remove the marker from the plot's children
     *
     * @param marker The marker that we will remove from the specific ticker's plot
     */
    private void removeHorizontalValueMarker(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        tickerCalls.get(symbol).get(0).remove(marker);
    }

    /**
     * Add a line to the the verticalMarkers arrayList and then add the line to the plot's children
     *
     * @param marker A node point that give the X value and the Y value for where the node resides
     * @param color The color that this vertical line will have on the plot
     */
    public void addVerticalValueMarker(Data<X, Y> marker, Color color) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (tickerCalls.get(symbol).get(1).contains(marker)) return;
        Line line = new Line();
        marker.setNode(line);
        line.setStroke(color);
        line.setOnMouseClicked(mouseEvent -> {
            //removes the text if the user rightClicks on it
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                removeVerticalValueMarker(marker);
            }
        });
        getPlotChildren().add(line);
        tickerCalls.get(symbol).get(1).add(marker);
    }

    /**
     * Remove a line from the verticalMarkers arraylist and remove the marker from the plot's children
     *
     * @param marker The specific marker on the ticker's plot that we will remove
     */
    private void removeVerticalValueMarker(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        tickerCalls.get(symbol).get(1).remove(marker);
    }

    /**
     * Add a node to the pointMarker's arrayList and see if you can draw from
     * point 1 to point 2, point 2 to point 3, and so on
     */
    public void addFreeFormLine(Data<X, Y> marker, Color color) {
        Objects.requireNonNull(marker, "the marker must not be null");
        Line line = new Line();
        line.setStroke(color);
        line.setEndY(getYAxis().getDisplayPosition(marker.getYValue()));
        line.setEndX(getXAxis().getDisplayPosition(marker.getXValue()));
        line.setStartY(getYAxis().getDisplayPosition(marker.getYValue()));
        line.setStartX(getXAxis().getDisplayPosition(marker.getXValue()));
        marker.setNode(line);
        //Set the start of each line to be where marker is, so that we can connect them later
        getPlotChildren().add(line);
        tickerCalls.get(symbol).get(2).add(marker);
    }

    /**
     * Add text on the user's cursor position on the plot
     *
     * @param marker  The dataPointValue that will be where the text goes on the graph
     * @param color   The color of the text
     * @param message The content of the text
     */
    public void addText(Data<X, Y> marker, String message, Color color) {
        Objects.requireNonNull(marker, "the marker must not be null");
        Objects.requireNonNull(message, "the message must not be null");
        if (tickerCalls.get(symbol).get(3).contains(marker)) return;
        Text text = new Text(message);
        text.setFill(color);
        marker.setNode(text);
        text.setOnMouseClicked(mouseEvent -> {
            //removes the text if the user rightClicks on it
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                removeText(marker);
            }
        });
        getPlotChildren().add(text);
        tickerCalls.get(symbol).get(3).add(marker);
    }

    /**
     * Remove text from the textMarkers arraylist and remove the marker from the plot's children
     *
     * @param marker The text that we will remove from the current ticker's plot
     */
    private void removeText(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        tickerCalls.get(symbol).get(6).remove(marker);
    }

    /**
     * @TODO
     * Experimental
     * */
    public void setColors(Color gr1, Color gr2){
        g1 = Color.color(gr1.getRed(), gr1.getGreen(), gr1.getBlue(), .3);
        g2 = Color.color(gr2.getRed(), gr2.getGreen(), gr2.getBlue(), .3);
        clearCurrentGradient();
    }

    /**
     * @TODO
     * Experimental
     * */
    private void clearCurrentGradient(){
        if (polygons.size() != 0) {
            for (Polygon polygon : polygons) {
                getPlotChildren().remove(polygon);
                polygons.remove(polygon);
            }
        }
    }

    /**
     * Change the charts shown children to the children inherited from a tickerSymbol state
     *
     * @param callNumber The tickerSymbols access index
     * */
    public void changeNodes(int callNumber) {
        if (callNumber > tickerCalls.size() || callNumber < 0)
            return;
        //Check to see if we already have the callNumber
        if (callNumber == tickerCalls.size())
            setup();
        for (ObservableList<Data<X,Y>> observableList : tickerCalls.get(symbol)){
            //purge the old symbols visible nodes from the chart

            for (Data<X, Y> anObservableList : observableList) {
                getPlotChildren().remove(anObservableList.getNode());
            }
        }
        System.out.println("last Symbol number: "+ symbol + "; newestCallNumber: "+ callNumber);
        symbol = callNumber;
    }

    /**
     * Add the children to the plot
     */
    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        int width = 2;
        for (Data<X, Y> horizontalMarker : tickerCalls.get(symbol).get(0)) {
            Line line = (Line) horizontalMarker.getNode();
            line.setStartX(0);
            line.setEndX(getBoundsInLocal().getWidth());
            line.setStartY(getYAxis().getDisplayPosition(horizontalMarker.getYValue()));
            line.setEndY(line.getStartY());
            line.setStrokeWidth(width);
            if (!getPlotChildren().contains(line)){
                getPlotChildren().add(line);
            }
            line.toFront();
        }
        for (Data<X, Y> verticalMarker : tickerCalls.get(symbol).get(1)) {
            Line line = (Line) verticalMarker.getNode();
            line.setStartX(getXAxis().getDisplayPosition(verticalMarker.getXValue()));
            line.setEndX(line.getStartX());
            line.setStartY(0d);
            line.setEndY(getBoundsInLocal().getHeight());
            line.setStrokeWidth(width);
            if (!getPlotChildren().contains(line)){
                getPlotChildren().add(line);
            }
            line.toFront();
        }
        for (int i = 0; i < tickerCalls.get(symbol).get(2).size() - 1; i++) {
            Line x1 = (Line) tickerCalls.get(symbol).get(2).get(i).getNode();
            x1.setStartX(getXAxis().getDisplayPosition(tickerCalls.get(symbol).get(2).get(i).getXValue()));
            x1.setStartY(getYAxis().getDisplayPosition(tickerCalls.get(symbol).get(2).get(i).getYValue()));
            x1.setEndY(getYAxis().getDisplayPosition(tickerCalls.get(symbol).get(2).get(i + 1).getYValue()));
            x1.setEndX(getXAxis().getDisplayPosition(tickerCalls.get(symbol).get(2).get(i + 1).getXValue()));
            x1.setStrokeWidth(width);
            if (!getPlotChildren().contains(x1)){
                getPlotChildren().add(x1);
            }
            x1.toFront();
        }
        for (Data<X, Y> textMarker : tickerCalls.get(symbol).get(3)) {
            Text text = (Text) textMarker.getNode();
            text.setX(getXAxis().getDisplayPosition(textMarker.getXValue()));
            text.setY(getYAxis().getDisplayPosition(textMarker.getYValue()));
            text.setFont(Font.font("Times New Roman", 20));
            text.setWrappingWidth(100);
            if (!getPlotChildren().contains(text)){
                getPlotChildren().add(text);
            }
            text.toFront();
        }
        if (polygons.size() == 0) {
            Polygon polygon = new Polygon();
            LinearGradient linearGrad = new LinearGradient(0, 0, 0, 1,
                    true, // proportional
                    CycleMethod.NO_CYCLE, // cycle colors
                    new Stop(0.0f, g1),
                    new Stop(1.0f, g2));
            polygon.getPoints().addAll(getBoundsInLocal().getMinX()-80, getBoundsInLocal().getMinY()-80,
                    getBoundsInLocal().getMaxX()+80, getBoundsInLocal().getMinY()-80,
                    getBoundsInLocal().getMaxX()+80, getBoundsInLocal().getMaxY()+80,
                    getBoundsInLocal().getMinX()-80, getBoundsInLocal().getMaxY()+80);
            getPlotChildren().add(polygon);
            polygon.toBack();
            polygons.add(polygon);
            polygon.setFill(linearGrad);
        }
    }
}
