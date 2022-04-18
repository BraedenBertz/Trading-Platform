package statisticalTestSuite;

import java.util.*;

public class SimpleMovingAverage {

    // queue used to store list so that we get the average
    private final Queue<Double> Dataset = new LinkedList<>();
    private int period;
    private double sum;

    // constructor to initialize period
    public SimpleMovingAverage(int period)
    {
        this.period = period;
    }

    // function to add new data in the
    // list and update the sum so that
    // we get the new mean
    public void addData(double num)
    {
        sum += num;
        Dataset.add(num);

        // Updating size so that length
        // of data set should be equal
        // to period as a normal mean has
        if (Dataset.size() > period)
        {
            sum -= Dataset.remove();
        }
    }

    // function to calculate mean
    public double getMean()
    {
        return sum / period;
    }

    public double lastSMA(int timePeriod){
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
