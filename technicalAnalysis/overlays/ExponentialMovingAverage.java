package technicalAnalysis.overlays;

public class ExponentialMovingAverage {
    private final double alpha;
    private Double oldValue;
    public ExponentialMovingAverage(double alpha) {
        this.alpha = alpha;
    }

    public double average(double value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        double newValue = oldValue + alpha * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }

    public double lastEMA(int timePeriod){
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
        return timePeriod*alpha;
    }
}
