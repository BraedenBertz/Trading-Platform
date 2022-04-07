package technicalAnalysis.overlays;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.util.Date;

public class LinearRegression {
    private final double intercept, slope;
    private final double r2;
    private final double svar0, svar1;

    /**
     * Performs a linear regression on the data points {@code (y[i], x[i])}.
     * */
    public LinearRegression(ObservableList<XYChart.Data<Date, Number>> data) {
        int n = data.size();

        // first pass
        double sumx = 0.0, sumy = 0.0;
        for (int i = 0; i < n; i++) {
            double yi = data.get(i).getYValue().doubleValue();
            sumx  += i;
            sumy  += yi;
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            double yi = data.get(i).getYValue().doubleValue();
            xxbar += ((long) i - xbar) * ((long) i - xbar);
            yybar += (yi - ybar) * (yi - ybar);
            xybar += ((long) i - xbar) * (yi - ybar);
        }
        slope  = xybar / xxbar;
        intercept = ybar - slope * xbar;

        // more statistical analysis
        double rss = 0.0;      // residual sum of squares
        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < n; i++) {
            double yi = data.get(i).getYValue().doubleValue();
            double fit = slope* (long) i + intercept;
            rss += (fit - yi) * (fit - yi);
            ssr += (fit - ybar) * (fit - ybar);
        }

        int degreesOfFreedom = n-2;
        r2    = ssr / yybar;
        double svar  = rss / degreesOfFreedom;
        svar1 = svar / xxbar;
        svar0 = svar/n + xbar*xbar*svar1;
    }

    /**
     * Returns the <em>y</em>-intercept &alpha; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
     *
     * @return the <em>y</em>-intercept &alpha; of the best-fit line <em>y = &alpha; + &beta; x</em>
     */
    public double intercept() {
        return intercept;
    }

    /**
     * Returns the slope &beta; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
     *
     * @return the slope &beta; of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>
     */
    public double slope() {
        return slope;
    }

    /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>,
     *         which is a real number between 0 and 1
     */
    public double R2() {
        return r2;
    }

    /**
     * Returns the coefficient of the measures the strength and direction of a
     * linear relationship between two variables <em>R</em>.
     *
     * @return the coefficient of determination <em>R</em>,
     *         which is a real number between 0 and 1
     */
    public double R(){ return Math.sqrt(r2);}

    /**
     * Returns the standard error of the estimate for the intercept.
     *
     * @return the standard error of the estimate for the intercept
     */
    public double interceptStdErr() {
        return Math.sqrt(svar0);
    }

    /**
     * Returns the standard error of the estimate for the slope.
     *
     * @return the standard error of the estimate for the slope
     */
    public double slopeStdErr() {
        return Math.sqrt(svar1);
    }

    /**
     * Returns the expected response {@code y} given the value of the predictor
     * variable {@code x}.
     *
     * @param  x the value of the predictor variable
     * @return the expected response {@code y} given the value of the predictor
     *         variable {@code x}
     */
    public double predict(double x) {
        return slope*x + intercept;
    }

    /**
     * Returns a string representation of the simple linear regression model.
     *
     * @return a string representation of the simple linear regression model,
     *         including the best-fit line and the coefficient of determination
     *         <em>R</em><sup>2</sup>
     */
    public String toString() {
        return String.format("%.2f n + %.2f", slope(), intercept()) +
                "  (R^2 = " + String.format("%.3f", R2()) + ")";
    }

}
