package com.tyrata.tyrata.data.model;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.tyrata.tyrata.util.CommonUtil.generateDataPoints;
import static com.tyrata.tyrata.util.CommonUtil.getThicknessPredictions;

/**
 * Plots Graph
 */
public class Graph {
    private GraphView mGraphView;

    private String mTireColor;
    private boolean mIsXinMiles; // Indicates if X-axis units are in miles
    private boolean mIsYinMm; // Indicates if Y-axis units are in mm

    private LineGraphSeries<DataPoint> mSeries; // Indicates readings from sensor
    private LineGraphSeries<DataPoint> mPredictions; // Indicates predictions based on readings
    private PointsGraphSeries<DataPoint> mPoints; // Circular shaped dots (that indicates latest reading)

    public Graph(GraphView mGraphView, String xTitle, String yTitle, String mTireColor) {
        this.mGraphView = mGraphView;
        this.mTireColor = mTireColor;

        mIsXinMiles = false;
        mIsYinMm = false;

        // Set bounds manually (not automatic). Set in the activities that call this class
        this.mGraphView.getViewport().setYAxisBoundsManual(true);
        this.mGraphView.getViewport().setXAxisBoundsManual(true);

        // Enable scaling (pinch to zoom) and scrolling
        this.mGraphView.getViewport().setScalable(false);
        this.mGraphView.getViewport().setScalableY(false);
        this.mGraphView.getViewport().setScrollable(false);
        this.mGraphView.getViewport().setScrollableY(false);

        // Set X and Y titles
        setXAxisTitle(xTitle);
        setYAxisTitle(yTitle);
        this.mGraphView.getGridLabelRenderer().setHorizontalAxisTitleTextSize(25f);
        this.mGraphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(25f);

        // custom label formatter to show inches
        this.mGraphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                // Truncates zeroes (otherwise the view is clustered with numbers)
                if (isValueX && value > 1000) return super.formatLabel(value / 1000, true);
                return super.formatLabel(value, isValueX);
            }
        });
    }

    /**
     * @Todo Change to dynamic after Demo
     * Set static graph view (boundaries and background color) for Demo graph mode
     */
    public void setGraphProperties() {
        double y_green = 10, y_yellow = 6, y_red = 2.8, y_gy = 7.2, y_yr = 4, y_off = 0.1;
        double x_max = 33600;

        mGraphView.getGridLabelRenderer().setTextSize(40f);

        if(mIsXinMiles) {
            setGraphViewBounds(0, 33600, -1, -1);
            setXAxisTitle("Readings");
        } else {
            setGraphViewBounds(0, 36, -1, -1);
            setXAxisTitle("time (months)");
            x_max = 36;
        }

        if(mIsYinMm) {
            setGraphViewBounds(-1, -1, 0, 8);
            setYAxisTitle("thickness (mm)");
            y_green = 8; //@Todo Dynamic Inch to mm conversion
            y_yellow = 4.76;
            y_red = 2.22;
            y_gy = 5.71;
            y_yr = 3.175;
            y_off = 0.08;
        } else {
            setGraphViewBounds(-1, -1, 0, 10);
            setYAxisTitle("thickness (32nds of an inch)");
        }

        drawColorZone(x_max, y_green, y_green,160, 219, 142, 0,0,0,y_off); // Green
        drawColorZone(x_max, y_gy,y_yellow,170, 222, 136, 5, 2, -2, y_off); // Green-Yellow Gradience
        drawColorZone(x_max, y_yellow,y_yellow,254, 246, 91, 0, 0, 0, y_off); // Yellow
        drawColorZone(x_max, y_yr,y_red,254, 246, 91,-2, -13, -3, y_off); // Yellow-Red Gradience
        drawColorZone(x_max, y_red,y_red,230, 84, 52,0, 0, 0, y_off); // Red
        drawGrid();
    }

    /**
     * @Todo Change to dynamic after Demo
     * Set static graph view (boundaries and background color) for Demo graph mode
     */
    public void setGraphProperties2() {
        double y_green = 10, y_yellow = 6, y_red = 2.8, y_gy = 7.2, y_yr = 4, y_off = 0.1;
        double x_max = 33600;

        mGraphView.getGridLabelRenderer().setTextSize(40f);

        if(mIsXinMiles) {
            setGraphViewBounds(0, 33600, -1, -1);
            setXAxisTitle("Readings");
        } else {
            setGraphViewBounds(0, 36, -1, -1);
            setXAxisTitle("time (months)");
            x_max = 36;
        }

        if(mIsYinMm) {
            setGraphViewBounds(-1, -1, 0, 8);
            setYAxisTitle("thickness (mm)");
            y_green = 8; //@Todo Dynamic Inch to mm conversion
            y_yellow = 4.76;
            y_red = 2.22;
            y_gy = 5.71;
            y_yr = 3.175;
            y_off = 0.08;
        } else {
            setGraphViewBounds(-1, -1, 0, 10);
            setYAxisTitle("thickness (32nds of an inch)");
        }

        drawColorZone(x_max, y_green, y_green,160, 219, 142, 0,0,0,y_off); // Green
        drawColorZone(x_max, y_gy,y_yellow,170, 222, 136, 5, 2, -2, y_off); // Green-Yellow Gradience
        drawColorZone(x_max, y_yellow,y_yellow,254, 246, 91, 0, 0, 0, y_off); // Yellow
        drawColorZone(x_max, y_yr,y_red,254, 246, 91,-2, -13, -3, y_off); // Yellow-Red Gradience
        drawColorZone(x_max, y_red,y_red,230, 84, 52,0, 0, 0, y_off); // Red
        drawGrid();
    }

    /**
     * @Todo Change to dynamic after Demo
     * Set static information (populate data-points) for Demo graph mode
     */
    public void setGlobalDataVariables() {
        int dataPointsLength;
        switch (mTireColor) {
            case "red":
                dataPointsLength = 110;
                break;
            case "yellow":
                dataPointsLength = 87;
                break;
            default:
                dataPointsLength = 37;
        }

        // Get the latest data point from sensor readings
        DataPoint newestPoint = generateDataPoints(dataPointsLength, 1, mIsXinMiles, mIsYinMm)[0];

        // Generate static data-points based on color
        mSeries = new LineGraphSeries<>(generateDataPoints(1, dataPointsLength, mIsXinMiles, mIsYinMm));
        mSeries.setColor(Color.BLACK);

        // Predict slope of the graph (based on mSeries list)
        mPredictions = new LineGraphSeries<>(new DataPoint[] {
                newestPoint,
                new DataPoint(getThicknessPredictions(1, dataPointsLength, mIsXinMiles, mIsYinMm), 2)
        });
        // Draw a circular point for the latest reading
        mPoints = new PointsGraphSeries<>(new DataPoint[]{newestPoint});
    }

    /**
     * Plot the graph (readings, predictions and points)
     */
    public void plotGraph() {
        plotFromData(mSeries);
        plotPrediction(mPredictions);
        plotPoints(mPoints);
    }

    /**
     * Re-plot the graph (called when data lists get updated)
     */
    public void rePlotGraph() {
        setGraphProperties();
        setGlobalDataVariables();
        plotGraph();
    }

    /**
     * Toggle between X-unit scales (time and mileage)
     */
    public void changeXUnits() {
        mIsXinMiles = !mIsXinMiles;
    }

    /**
     * Toggle between Y-unit scales (thickness in inch and mm)
     */
    public void changeYUnits() {
        mIsYinMm = !mIsYinMm;
    }

    /**
     * Clear the graph (to avoid painting on existing paint)
     */
    public void removeAllSeries() {
        mGraphView.removeAllSeries();
        mGraphView.getSecondScale().removeAllSeries();
    }

    /**
     * Add secondary Y axis to the graph
     */
    public void addSecondaryAxis(String title) {
        mGraphView.getGridLabelRenderer().setGridColor(Color.WHITE);
        mGraphView.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        mGraphView.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.WHITE);
        mGraphView.getGridLabelRenderer().setVerticalLabelsColor(Color.YELLOW);
        mGraphView.getGridLabelRenderer().setVerticalAxisTitleColor(Color.YELLOW);
        mGraphView.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);

        mGraphView.getSecondScale().setVerticalAxisTitle(title);
        mGraphView.getSecondScale().setVerticalAxisTitleColor(Color.RED);

        mGraphView.getSecondScale().setVerticalAxisTitleTextSize(25f);
        // @Todo Add bounds later dynamically

    }

    /**
     * Plot secondary Y-axis data
     */
    public void plotSecondaryData(LineGraphSeries<DataPoint> series) {
        series.setThickness(5);
        mGraphView.getSecondScale().addSeries(series);
    }
    /**
     * Helper function to plot Line from the readings data (mSeries)
     */
    public void plotFromData(LineGraphSeries<DataPoint> series) {
        series.setThickness(5);
        mGraphView.addSeries(series);
    }
    /**
     * Helper method to set X and Y boundaries
     * Positive checks to indicate update (doesn't update if negative value passed)
     */
    public void setGraphViewBounds(int minX, int maxX, int minY, int maxY) {
        if (minX >= 0) mGraphView.getViewport().setMinX(minX);
        if (maxX >= 0) mGraphView.getViewport().setMaxX(maxX);
        if (minY >= 0) mGraphView.getViewport().setMinY(minY);
        if (maxY >= 0) mGraphView.getViewport().setMaxY(maxY);
    }
    public void switchToDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:ss");
        mGraphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX) {
                    sdf.format(new Date((long) value));
                }
                return super.formatLabel(value, isValueX);
            }
        });
        mGraphView.getGridLabelRenderer().setHumanRounding(false);
        mGraphView.getGridLabelRenderer().setNumHorizontalLabels(3);
    }
    public void setSecondaryGraphViewBounds(int minY, int maxY) {
        mGraphView.getSecondScale().setMaxY(maxY >= 0 ? maxY : 0);
        mGraphView.getSecondScale().setMinY(minY >= 0 ? minY : 0);
    }

    /**
     * Sets X-Axis tile
     */
    public void setXAxisTitle(String title) {
        mGraphView.getGridLabelRenderer().setHorizontalAxisTitle(title);
    }

    /**
     * Sets Y-Axis tile
     */
    private void setYAxisTitle(String title) {
        //mGraphView.getGridLabelRenderer().setVerticalAxisTitle(title);
        mGraphView.getGridLabelRenderer().setVerticalAxisTitle("Measurement");
    }

    /**
     * Helper function to plot mPredictions Line
     */
    private void plotPrediction(LineGraphSeries<DataPoint> predictions) {
        // Custom paint to make a dotted line
        predictions.setThickness(8);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setPathEffect(new DashPathEffect(new float[]{9, 9}, 0));
        predictions.setCustomPaint(paint);
        predictions.setDrawAsPath(true);
        mGraphView.addSeries(predictions);
    }

    /**
     * Helper function to plot circular dot (indicating latest reading)
     */
    private void plotPoints(PointsGraphSeries<DataPoint> points) {
        mGraphView.addSeries(points);
    }

    /**
     * Draws consecutive multiple colored lines for graph background to indicate zones (R, G or Y)
     */
    private void drawColorZone(double x_max,
                               double y_max,
                               double y_min,
                               int R,
                               int G,
                               int B,
                               int R_off,
                               int G_off,
                               int B_off,
                               double y_off) {
        double y = y_max;
        // @Todo Check better (automatic) drawing instead of doing it manually
        // Generate new colored band with a RGB value
        while(y >= y_min) {
            // Draw line between the specified coordinates (x1,y1) to (x2,y2)
            LineGraphSeries<DataPoint> mColoredBand = new LineGraphSeries<>(new DataPoint[]{
                    new DataPoint(0, y),
                    new DataPoint(x_max, y)
            });
            int color = Color.rgb(R, G, B);
            mColoredBand.setBackgroundColor(color);
            mColoredBand.setDrawBackground(true);
            mColoredBand.setColor(color);
            mGraphView.addSeries(mColoredBand); // Add line to the graph

            // Update color offsets (for smooth color transitions)
            R += R_off;
            G += G_off;
            B += B_off;
            y -= y_off;
        }
    }

    /**
     * @Todo Change to automatic drawing. Avoid hardcoding
     * Draw background grid
     */
    private void drawGrid() {
        double maxX = mGraphView.getViewport().getMaxX(true);
        double maxY = mGraphView.getViewport().getMaxY(true);

        // Draw horizontal lines with 2 unit separations
        for(double i = 2; i <= maxY; i += 2) drawGridLine(0, i, maxX, i);

        // Draw vertical lines with either 5 (if months) or 5000 (if miles) unit separations
        if(mIsXinMiles)
            for(double i = 5000; i <= maxX; i += 5000) drawGridLine(i, 0, i, maxY);
        else
            for(double i = 5; i <= maxX; i += 5) drawGridLine(i, 0, i, maxY);
    }

    /**
     * Helper method used by drawGrid to draw a line
     */
    private void drawGridLine(double x1, double y1, double x2, double y2) {
        LineGraphSeries<DataPoint> gridLine;
        gridLine = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(x1, y1),
                new DataPoint(x2, y2)
        });
        gridLine.setColor(Color.parseColor("#808080")); // Gray colored
        gridLine.setThickness(1);
        mGraphView.addSeries(gridLine);
    }
}
