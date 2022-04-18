package statisticalTestSuite;

import java.util.*;

/**
 * In finance, volume-weighted average price (VWAP) is the ratio of the value traded to total volume traded over a
 * particular time horizon (usually one day). It is a measure of the average price at which a stock is traded over the trading horizon.
 * VWAP is calculated using the following formula:
 * VWAP = sum( P[j] * Q[j])/sum(Q[j])
 * where P[j] is price of trade j;
 * Q[j] is quantity of trade j;
 * and j is each individual trade that takes place over the defined period of time, excluding cross trades and basket cross trades.
 * */
public class VolumeWeightedAveragePrice {
    // queue used to store list so that we get the average
    private final Queue<Double> priceDataset = new LinkedList<>();
    private final Queue<Long> volumeDataset = new LinkedList<>();
    private int period;
    private double PriceSum;
    private double volumeSum;

    // constructor to initialize period
    public VolumeWeightedAveragePrice(int period)
    {
        this.period = period;
    }

    public Double calculateVWAP(){
        return PriceSum/volumeSum;
    }

    // function to add new data in the
    // list and update the sum so that
    // we get the new mean
    public void addData(Double datum, long Volume)
    {
        PriceSum += datum * Volume;
        volumeSum+= Volume;
        priceDataset.add(datum);
        volumeDataset.add(Volume);

        // Updating size so that length
        // of data set should be equal
        // to period as a normal mean has
        if (priceDataset.size() > period)
        {
            PriceSum -= priceDataset.remove()*volumeDataset.element();
            volumeSum -= volumeDataset.remove();

        }
    }

    public double lastVWAP(int timePeriod){
        switch (timePeriod){
            case 0:
                //Daily
                break;
            case 1:
                //Weekly
                break;
            case 2:
                //Monthly
                break;
        }
        return 2;
    }

    public void setPeriod(int period){
        this.period = period;
    }
}
