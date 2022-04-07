/**
 * @author gerdreiss
 * Original Link: @link{https://github.com/gerdreiss/com.jscriptive.moneyfx/blob/master/src/main/java/com/jscriptive/moneyfx/ui/chart/LocalDateAxis.java}
 */
package utilities;

import com.sun.javafx.charts.ChartLayoutAnimator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.*;
import javafx.scene.chart.Axis;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.*;
import java.util.*;

/**
 * An axis that displays date and time values.
 * <p/>
 * Tick labels are usually automatically set and calculated depending on the range unless you explicitly {@linkplain #setTickLabelFormatter(javafx.util.StringConverter) set an formatter}.
 * <p/>
 * You also have the chance to specify fix lower and upper bounds, otherwise they are calculated by your data.
 * Displaying date values, ranging over several months:</p>
 * <p>
 * Displaying date values, ranging only over a few hours:</p>
 * <p/>
 * <p/>
 * <pre>
 * {@code
 * ObservableList<XYChart.Series<Date, Number>> series = FXCollections.observableArrayList();
 *
 * ObservableList<XYChart.Data<Date, Number>> series1Data = FXCollections.observableArrayList();
 * series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2012, 11, 15).getTime(), 2));
 * series1Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 5, 3).getTime(), 4));
 *
 * ObservableList<XYChart.Data<Date, Number>> series2Data = FXCollections.observableArrayList();
 * series2Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 0, 13).getTime(), 8));
 * series2Data.add(new XYChart.Data<Date, Number>(new GregorianCalendar(2014, 7, 27).getTime(), 4));
 *
 * series.add(new XYChart.Series<>("Series1", series1Data));
 * series.add(new XYChart.Series<>("Series2", series2Data));
 *
 * NumberAxis numberAxis = new NumberAxis();
 * Start.DateAxis dateAxis = new Start.DateAxis();
 * LineChart<Date, Number> lineChart = new LineChart<>(dateAxis, numberAxis, series);
 * }
 */
public class DateAxis extends Axis<LocalDateTime> implements TemporalUnit{

    /**
     * These property are used for animation.
     */
    private final LongProperty currentLowerBound = new SimpleLongProperty(this, "currentLowerBound");

    private final LongProperty currentUpperBound = new SimpleLongProperty(this, "currentUpperBound");

    private final ObjectProperty<StringConverter<LocalDateTime>> tickLabelFormatter = new ObjectPropertyBase<StringConverter<LocalDateTime>>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return DateAxis.this;
        }

        @Override
        public String getName() {
            return "tickLabelFormatter";
        }
    };

    /**
     * Stores the min and max date of the list of dates which is used.
     * If autoranging is true, these values are used as lower and upper bounds.
     */
    private LocalDateTime minDate, maxDate;

    private final ObjectProperty<LocalDateTime> lowerBound = new ObjectPropertyBase<LocalDateTime>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return DateAxis.this;
        }

        @Override
        public String getName() {
            return "lowerBound";
        }
    };

    private final ObjectProperty<LocalDateTime> upperBound = new ObjectPropertyBase<LocalDateTime>() {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public Object getBean() {
            return DateAxis.this;
        }

        @Override
        public String getName() {
            return "upperBound";
        }
    };

    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);

    private Object currentAnimationID;

    private TemporalUnit actualInterval = TemporalUnit.Days;

    /**
     * Default constructor. By default the lower and upper bound are calculated by the data.
     */
    public DateAxis() {
    }

    /**
     * Constructs a date axis with fix lower and upper bounds.
     *
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     */
    public DateAxis(LocalDateTime lowerBound, LocalDateTime upperBound) {
        this();
        setAutoRanging(false);
        setUpperBound(upperBound);
        setLowerBound(lowerBound);
    }

    /**
     * Constructs a date axis with a label and fix lower and upper bounds.
     *
     * @param axisLabel  The label for the axis.
     * @param lowerBound The lower bound.
     * @param upperBound The upper bound.
     */
    public DateAxis(String axisLabel, LocalDateTime lowerBound, LocalDateTime upperBound) {
        this(lowerBound, upperBound);
        setLabel(axisLabel);
    }

    /**
     * Called when data has changed and the range may not be valid any more. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param list The current set of all data that needs to be plotted on this axis
     */
    @Override
    public void invalidateRange(List<LocalDateTime> list) {
        super.invalidateRange(list);

        Collections.sort(list);
        if (list.isEmpty()) {
            minDate = maxDate = LocalDateTime.now();
        } else if (list.size() == 1) {
            minDate = maxDate = list.get(0);
        } else {
            minDate = list.get(0);
            maxDate = list.get(list.size() - 1);
        }
    }

    /**
     * This calculates the upper and lower bound based on the data provided to invalidateRange() method. This must not
     * effect the state of the axis, changing any properties of the axis. Any results of the auto-ranging should be
     * returned in the range object. This will we passed to setRange() if it has been decided to adopt this range for
     * this axis.
     *
     * @param length The length of the axis in screen coordinates
     * @return Range information, this is implementation dependent
     */
    @Override
    protected Object autoRange(double length) {
        if (isAutoRanging()) {
            return new Object[]{minDate, maxDate};
        } else {
            if (getLowerBound() == null || getUpperBound() == null) {
                throw new IllegalArgumentException("If autoRanging is false, a lower and upper bound must be set.");
            }
            return getRange();
        }
    }

    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this method should
     * animate the range to the new range.
     *
     * @param range A range object returned from autoRange()
     * @param animating If true animate the change in range
     */
    @Override
    protected void setRange(Object range, boolean animating) {
        Object[] r = (Object[]) range;
        LocalDateTime oldLowerBound = getLowerBound();
        LocalDateTime oldUpperBound = getUpperBound();
        LocalDateTime lower = LocalDateTime.parse(r[0].toString());
        LocalDateTime upper = LocalDateTime.parse(r[1].toString());
        setLowerBound(lower);
        setUpperBound(upper);

        if (animating) {

            animator.stop(currentAnimationID);
            currentAnimationID = animator.animate(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, toNumericValue(oldLowerBound)),
                            new KeyValue(currentUpperBound, toNumericValue(oldUpperBound))
                    ),
                    new KeyFrame(Duration.millis(700),
                            new KeyValue(currentLowerBound, toNumericValue(lower)),
                            new KeyValue(currentUpperBound, toNumericValue(upper))
                    )
            );

        } else {
            currentLowerBound.set((long) toNumericValue(getLowerBound()));
            currentUpperBound.set((long) toNumericValue(getUpperBound()));
        }
    }

    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    @Override
    protected Object getRange() {
        return new Object[]{getLowerBound(), getUpperBound()};
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        return 0;
    }

    /**
     * Get the display position along this axis for a given value.
     * If the value is not in the current range, the returned value will be an extrapolation of the display
     * position.
     *
     * If the value is not valid for this Axis and the axis cannot display such value in any range,
     * Double.NaN is returned
     *
     * @param date The data value to work out display position for
     * @return display position or Double.NaN if value not valid
     */
    public double getDisplayPosition(LocalDateTime date) {
        final double length = getSide().isHorizontal() ? getWidth() : getHeight();

        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        // Get the actual range of the visible area.
        // The minimal date should start at the zero position, that's why we subtract it.
        double range = length - getZeroPosition();

        // Then get the difference from the actual date to the min date and divide it by the total difference.
        // We get a value between 0 and 1, if the date is within the min and max date.
        double d = (toNumericValue(date) - currentLowerBound.get()) / diff;

        // Multiply this percent value with the range and add the zero offset.
        if (getSide().isVertical()) {
            return getHeight() - d * range + getZeroPosition();
        } else {
            return d * range + getZeroPosition();
        }
    }

    /**
     * Get the data value for the given display position on this axis. If the axis
     * is a CategoryAxis this will be the nearest value.
     *
     * @param  displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or
     *         null if not on axis;
     */
    @Override
    public LocalDateTime getValueForDisplay(double displayPosition) {
        final double length = getSide().isHorizontal() ? getWidth() : getHeight();

        // Get the difference between the max and min date.
        double diff = currentUpperBound.get() - currentLowerBound.get();

        if (getSide().isVertical()) {
            // displayPosition = getHeight() - ((date - lowerBound) / diff) * range + getZero
            // date = displayPosition - getZero - getHeight())/range * diff + lowerBound
            return Instant.ofEpochMilli((long) ((displayPosition - getHeight()) / -length * diff + currentLowerBound.get()))
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else {
            // displayPosition = ((date - lowerBound) / diff) * range + getZero
            // date = displayPosition - getZero)/range * diff + lowerBound
            return Instant.ofEpochMilli((long) (displayPosition / length * diff + currentLowerBound.get()))
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param date The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    @Override
    public boolean isValueOnAxis(LocalDateTime date) {
        return toNumericValue(date) > currentLowerBound.get() && toNumericValue(date) < currentUpperBound.get();
    }

    /**
     * All axis values must be representable by some numeric value. This gets the numeric value for a given data value.
     *
     * @param date The data value to convert
     * @return Numeric value for the given data value
     */
    @Override
    public double toNumericValue(LocalDateTime date) {
        return ZonedDateTime.of(date, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * All axis values must be representable by some numeric value. This gets the data value for a given numeric value.
     *
     * @param v The numeric value to convert
     * @return Data value for given numeric value
     */
    @Override
    public LocalDateTime toRealValue(double v) {
        return Instant.ofEpochMilli((long)v).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param v The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override
    protected List<LocalDateTime> calculateTickValues(double v, Object range) {
        Object[] r = (Object[]) range;
        LocalDateTime lower = (LocalDateTime) r[0];
        LocalDateTime upper = (LocalDateTime) r[1];

        List<LocalDateTime> dateList = new ArrayList<>();
        ZonedDateTime calendar;

        // The preferred gap which should be between two tick marks.
        double averageTickGap = 100;
        double averageTicks = v / averageTickGap;

        List<LocalDateTime> previousDateList = new ArrayList<>();

        TemporalUnit previousInterval = TemporalUnit.values()[0];

        // Starting with the greatest interval, add one of each calendar unit.
        for (TemporalUnit interval : TemporalUnit.values()) {
            // Reset the calendar.
            calendar = ZonedDateTime.ofLocal(lower, ZoneId.systemDefault(), ZoneOffset.MIN);
            // Clear the list.
            dateList.clear();
            previousDateList.clear();
            actualInterval = interval;

            // Loop as long we exceeded the upper bound.calendar.toLocalDateTime
            while (toNumericValue(calendar.toLocalDateTime()) <= toNumericValue(upper)) {
                dateList.add(calendar.toLocalDateTime());
                Object ffff =  actualInterval.interval;
                calendar = calendar.plus(interval.amount, actualInterval.interval);
            }
            // Then check the size of the list. If it is greater than the amount of ticks, take that list.
            if (dateList.size() > averageTicks) {
                calendar = ZonedDateTime.ofLocal(lower, ZoneId.systemDefault(), ZoneOffset.MIN);
                // Recheck if the previous interval is better suited.
                while (toNumericValue(calendar.toLocalDateTime()) <= toNumericValue(upper)) {
                    previousDateList.add(calendar.toLocalDateTime());
                    calendar = calendar.plus(previousInterval.amount, actualInterval.interval);
                }
                break;
            }

            previousInterval = interval;
        }
        if (previousDateList.size() - averageTicks > averageTicks - dateList.size()) {
            dateList = previousDateList;
            actualInterval = previousInterval;
        }

        // At last add the upper bound.
        dateList.add(upper);

        List<LocalDateTime> evenDateList = makeDatesEven(dateList);
        // If there are at least three dates, check if the gap between the lower date and the second date is at least half the gap of the second and third date.
        // Do the same for the upper bound.
        // If gaps between dates are to small, remove one of them.
        // This can occur, e.g. if the lower bound is 25.12.2013 and years are shown. Then the next year shown would be 2014 (01.01.2014) which would be too narrow to 25.12.2013.
        if (evenDateList.size() > 2) {

            LocalDateTime secondDate = evenDateList.get(1);
            LocalDateTime thirdDate = evenDateList.get(2);
            LocalDateTime lastDate = evenDateList.get(dateList.size() - 2);
            LocalDateTime previousLastDate = evenDateList.get(dateList.size() - 3);

            // If the second date is too near by the lower bound, remove it.
            if (toNumericValue(secondDate) - toNumericValue(lower) < (toNumericValue(thirdDate) - toNumericValue(secondDate)) / 2) {
                evenDateList.remove(secondDate);
            }

            // If difference from the upper bound to the last date is less than the half of the difference of the previous two dates,
            // we better remove the last date, as it comes to close to the upper bound.
            if (toNumericValue(upper) - toNumericValue(lastDate) < (toNumericValue(lastDate) - toNumericValue(previousLastDate)) / 2) {
                evenDateList.remove(lastDate);
            }
        }

        return evenDateList;
    }

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     */
    @Override
    protected void layoutChildren() {
        if (!isAutoRanging()) {
            currentLowerBound.set((long)toNumericValue(getLowerBound()));
            currentUpperBound.set((long)toNumericValue(getUpperBound()));
        }
        super.layoutChildren();
    }

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param date The value to format into a tick label string
     * @return A formatted string for the given value
     */
    @Override
    protected String getTickMarkLabel(LocalDateTime date) {

        StringConverter<LocalDateTime> converter = getTickLabelFormatter();
        if (converter != null) {
            return converter.toString(date);
        }

        DateTimeFormatter dateFormat;
        ZonedDateTime calendar = ZonedDateTime.ofLocal(date, ZoneId.systemDefault(), ZoneOffset.MIN);

        if (actualInterval.amount == TemporalUnit.Years.amount && calendar.getMonth().equals(Month.JANUARY) && calendar.getDayOfMonth() == 1) {
            dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        } else if (actualInterval.amount == TemporalUnit.Months.amount && calendar.getDayOfMonth() == 1) {
            dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
        } else {
            switch (actualInterval.interval) {
                case WEEKS:
                default:
                    dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                    break;
                case HOURS:
                case MINUTES:
                    dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                    break;
                case SECONDS:
                    dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                    break;
                case MILLIS:
                    dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
                    break;
            }
        }
        return dateFormat.format(date);
    }

    private final ReadOnlyDoubleWrapper scale = new ReadOnlyDoubleWrapper(this, "scale", 0) {
        @Override
        protected void invalidated() {
            requestAxisLayout();
        }
    };
    public double getScale(){

        return scale.get();
    }

    /**
     * Makes dates even, in the sense of that years always begin in January, months always begin on the 1st and days always at midnight.
     *
     * @param dates The list of dates.
     * @return The new list of dates.
     */
    private List<LocalDateTime> makeDatesEven(List<LocalDateTime> dates) {
        // If the dates contain more dates than just the lower and upper bounds, make the dates in between even.
        if (dates.size() > 2) {
            List<LocalDateTime> evenDates = new ArrayList<>();

            // For each interval, modify the date slightly by a few millis, to make sure they are different days.
            // This is because Axis stores each value and won't update the tick labels, if the value is already known.
            // This happens if you display days and then add a date many years in the future the tick label will still be displayed as day.
            for (int i = 0; i < dates.size(); i++) {
                ZonedDateTime calendar = ZonedDateTime.ofLocal(dates.get(i), ZoneId.systemDefault(), ZoneOffset.UTC);
                switch (actualInterval.interval) {
                    case YEARS:
                        // If its not the first or last date (lower and upper bound), make the year begin with first month and let the months begin with first day.
                        if (i != 0 && i != dates.size() - 1) {
                            calendar.with(ChronoField.YEAR, 0);
                            calendar.with(ChronoField.DAY_OF_MONTH, 1);
                        }
                        calendar.with(ChronoField.HOUR_OF_DAY, 0);
                        calendar.with(ChronoField.MINUTE_OF_DAY, 0);
                        calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        calendar.with(ChronoField.MILLI_OF_DAY, 6);
                        break;
                    case MONTHS:
                        // If its not the first or last date (lower and upper bound), make the months begin with first day.
                        if (i != 0 && i != dates.size() - 1) {
                            calendar.with(ChronoField.DAY_OF_MONTH, 1);
                        }
                        calendar.with(ChronoField.HOUR_OF_DAY, 0);
                        calendar.with(ChronoField.MINUTE_OF_DAY, 0);
                        calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        calendar.with(ChronoField.MILLI_OF_DAY, 5);
                        break;
                    case WEEKS:
                        // Make weeks begin with first day of week?
                        calendar.with(ChronoField.HOUR_OF_DAY, 0);
                        calendar.with(ChronoField.MINUTE_OF_DAY, 0);
                        calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        calendar.with(ChronoField.MILLI_OF_DAY, 4);
                        break;
                    case DAYS:
                        calendar.with(ChronoField.HOUR_OF_DAY, 0);
                        calendar.with(ChronoField.MINUTE_OF_DAY, 0);
                        calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        calendar.with(ChronoField.MILLI_OF_DAY, 3);
                        break;
                    case HOURS:
                        if (i != 0 && i != dates.size() - 1) {
                            calendar.with(ChronoField.MINUTE_OF_DAY, 0);
                            calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        }
                        calendar.with(ChronoField.MILLI_OF_DAY, 2);
                        break;
                    case MINUTES:
                        if (i != 0 && i != dates.size() - 1) {
                            calendar.with(ChronoField.SECOND_OF_DAY, 0);
                        }
                        calendar.with(ChronoField.MILLI_OF_DAY, 1);
                        break;
                    case SECONDS:
                        calendar.with(ChronoField.MILLI_OF_DAY, 0);
                        break;

                }
                evenDates.add(calendar.toLocalDateTime());
            }

            return evenDates;
        } else {
            return dates;
        }
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The property.
     * @see #getLowerBound()
     * @see #setLowerBound(java.time.LocalDateTime)
     */
    private ObjectProperty<LocalDateTime> lowerBoundProperty() {
        return lowerBound;
    }

    /**
     * Gets the lower bound of the axis.
     *
     * @return The lower bound.
     * @see #lowerBoundProperty()
     */
    private LocalDateTime getLowerBound() {
        return lowerBound.get();
    }

    /**
     * Sets the lower bound of the axis.
     *
     * @param date The lower bound date.
     * @see #lowerBoundProperty()
     */
    public final void setLowerBound(LocalDateTime date) {
        lowerBound.set(date);
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The property.
     * @see #getUpperBound() ()
     * @see #setUpperBound(java.time.LocalDateTime)
     */
    public final ObjectProperty<LocalDateTime> upperBoundProperty() {
        return upperBound;
    }

    /**
     * Gets the upper bound of the axis.
     *
     * @return The upper bound.
     * @see #upperBoundProperty()
     */
    private LocalDateTime getUpperBound() {
        return upperBound.get();
    }

    /**
     * Sets the upper bound of the axis.
     *
     * @param date The upper bound date.
     * @see #upperBoundProperty() ()
     */
    public final void setUpperBound(LocalDateTime date) {
        upperBound.set(date);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The converter.
     */
    private StringConverter<LocalDateTime> getTickLabelFormatter() {
        return tickLabelFormatter.getValue();
    }

    /**
     * Sets the tick label formatter for the ticks.
     *
     * @param value The converter.
     */
    public final void setTickLabelFormatter(StringConverter<LocalDateTime> value) {
        tickLabelFormatter.setValue(value);
    }

    /**
     * Gets the tick label formatter for the ticks.
     *
     * @return The property.
     */
    public final ObjectProperty<StringConverter<LocalDateTime>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    /**
     * Checks if the duration of the unit is an estimate.
     * <p>
     * All units have a duration, however the duration is not always accurate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * This method returns true if the duration is an estimate and false if it is
     * accurate. Note that accurate/estimated ignores leap seconds.
     *
     * @return true if the duration is estimated, false if accurate
     */
    @Override
    public boolean isDurationEstimated() {
        return false;
    }

    /**
     * Returns a copy of the specified temporal object with the specified period added.
     * <p>
     * The period added is a multiple of this unit. For example, this method
     * could be used to add "3 days" to a date by calling this method on the
     * instance representing "days", passing the date and the period "3".
     * The period to be added may be negative, which is equivalent to subtraction.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use {@link Temporal#plus(long, java.time.temporal.TemporalUnit)}:
     * <pre>
     *   // these two lines are equivalent, but the second approach is recommended
     *   temporal = thisUnit.addTo(temporal);
     *   temporal = temporal.plus(thisUnit);
     * </pre>
     * It is recommended to use the second approach, {@code plus(TemporalUnit)},
     * as it is a lot clearer to read in code.
     * <p>
     * Implementations should perform any queries or calculations using the units
     * available in {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the unit is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     * <p>
     * Implementations must not alter the specified temporal object.
     * Instead, an adjusted copy of the original must be returned.
     * This provides equivalent, safe behavior for immutable and mutable implementations.
     *
     * @param <R>  the type of the Temporal object
     * @param temporal  the temporal object to adjust, not null
     * @param amount  the amount of this unit to add, positive or negative
     * @return the adjusted temporal object, not null
     * @throws DateTimeException if the amount cannot be added
     * @throws UnsupportedTemporalTypeException if the unit is not supported by the temporal
     */
    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return null;
    }

    /**
     * Checks if this unit represents a component of a time.
     * <p>
     * A unit is time-based if it can be used to imply meaning from a time.
     * It must have a {@linkplain #getDuration() duration} that divides into
     * the length of a standard day without remainder.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a unit like 36 hours.
     *
     * @return true if this unit is a component of a time
     */
    @Override
    public boolean isTimeBased() {
        return false;
    }

    /**
     * Gets the duration of this unit, which may be an estimate.
     * <p>
     * All units return a duration measured in standard nanoseconds from this method.
     * The duration will be positive and non-zero.
     * For example, an hour has a duration of {@code 60 * 60 * 1,000,000,000ns}.
     * <p>
     * Some units may return an accurate duration while others return an estimate.
     * For example, days have an estimated duration due to the possibility of
     * daylight saving time changes.
     * To determine if the duration is an estimate, use {@link #isDurationEstimated()}.
     *
     * @return the duration of this unit, which may be an estimate, not null
     */
    @Override
    public java.time.Duration getDuration() {
        return null;
    }

    /**
     * Checks if this unit represents a component of a date.
     * <p>
     * A date is time-based if it can be used to imply meaning from a date.
     * It must have a {@linkplain #getDuration() duration} that is an integral
     * multiple of the length of a standard day.
     * Note that it is valid for both {@code isDateBased()} and {@code isTimeBased()}
     * to return false, such as when representing a unit like 36 hours.
     *
     * @return true if this unit is a component of a date
     */
    @Override
    public boolean isDateBased() {
        return false;
    }

    /**
     * Calculates the amount of time between two temporal objects.
     * <p>
     * This calculates the amount in terms of this unit. The start and end
     * points are supplied as temporal objects and must be of compatible types.
     * The implementation will convert the second type to be an instance of the
     * first type before the calculating the amount.
     * The result will be negative if the end is before the start.
     * For example, the amount in hours between two temporal objects can be
     * calculated using {@code HOURS.between(startTime, endTime)}.
     * <p>
     * The calculation returns a whole number, representing the number of
     * complete units between the two temporals.
     * For example, the amount in hours between the times 11:30 and 13:29
     * will only be one hour as it is one minute short of two hours.
     * <p>
     * There are two equivalent ways of using this method.
     * The first is to invoke this method directly.
     * The second is to use :
     * <pre>
     *   // these two lines are equivalent
     *   between = thisUnit.between(start, end);
     *   between = start.until(end, thisUnit);
     * </pre>
     * The choice should be made based on which makes the code more readable.
     * <p>
     * For example, this method allows the number of days between two dates to
     * be calculated:
     * <pre>
     *  long daysBetween = DAYS.between(start, end);
     *  // or alternatively
     *  long daysBetween = start.until(end, DAYS);
     * </pre>
     * <p>
     * Implementations should perform any queries or calculations using the units
     * available in {@link ChronoUnit} or the fields available in {@link ChronoField}.
     * If the unit is not supported an {@code UnsupportedTemporalTypeException} must be thrown.
     * Implementations must not alter the specified temporal objects.
     *
     * @implSpec
     * Implementations must begin by checking to if the two temporals have the
     * same type using {@code getClass()}. If they do not, then the result must be
     * obtained by calling {@code temporal1Inclusive.until(temporal2Exclusive, this)}.
     *
     * @param temporal1Inclusive  the base temporal object, not null
     * @param temporal2Exclusive  the utilities temporal object, exclusive, not null
     * @return the amount of time between temporal1Inclusive and temporal2Exclusive
     *  in terms of this unit; positive if temporal2Exclusive is later than
     *  temporal1Inclusive, negative if earlier
     * @throws DateTimeException if the amount cannot be calculated, or the end
     *  temporal cannot be converted to the same type as the start temporal
     * @throws UnsupportedTemporalTypeException if the unit is not supported by the temporal
     * @throws ArithmeticException if numeric overflow occurs
     */
    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return 0;
    }

    /**
     * The intervals, which are used for the tick labels. Beginning with the largest interval, the axis tries to calculate the tick values for this interval.
     * If a smaller interval is better suited for, that one is taken.
     */
    private enum TemporalUnit {
        Decades(ChronoUnit.DECADES, 10),
        Years(ChronoUnit.YEARS, 1),
        Months(ChronoUnit.MONTHS, 1),
        Weeks(ChronoUnit.WEEKS, 1),
        Days(ChronoUnit.DAYS, 1),
        Hours(ChronoUnit.HOURS, 1),
        Minutes(ChronoUnit.MINUTES, 1),
        Seconds(ChronoUnit.SECONDS, 1),
        Millis(ChronoUnit.MILLIS, 1);

        private final int amount;

        private final ChronoUnit interval;

        TemporalUnit(ChronoUnit interval, int amount) {
            this.interval = interval;
            this.amount = amount;
        }
    }
}
