package com.tyrata.tyrata.util;

import android.util.Pair;

import com.jjoe64.graphview.series.DataPoint;
import com.tyrata.tyrata.data.model.v2.Reading;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility Class for shared/common data (for all the classes)
 * Read about these variables at their usages in other classes
 */
public class CommonUtil {
    public static final Object lock = new Object();

    public static final int REQUEST_ENABLE_BT = 1;

    // Beta Tester ListView headings
    public static final String[] SENSOR_KEYS = {"Sensor Name:", "MAC Address:", "Battery"};

    // Real-time data
    public static int[] tireValues;
    // @Todo Might have to change values. Ask hardware people.
    // Tread thickness range variables
    private static final double[] THICKNESS_RED = {1, 1000};
    private static final double[] THICKNESS_YELLOW = {1000, 2000};
    private static final double[] THICKNESS_GREEN = {2000, 60000};
    // @Todo Remove hardcoded graph plot data after Demo
    private static final double[] THICKNESS_INCH = {10.00,9.86,9.73,9.55,9.48,9.32,9.26,9.19,9.13,9.07,8.91,8.80,8.75,8.68,8.60,8.52,8.44,8.37,8.23,8.20,8.20,8.20,8.13,8.05,8.00,7.92,7.88,7.65,7.42,7.38,7.24,7.19,7.14,7.09,7.02,6.99,6.87,6.87,6.87,6.82,6.81,6.73,6.64,6.53,6.49,6.39,6.32,6.29,6.21,6.21,6.18,6.18,6.18,6.12,6.09,6.02,6.00,5.94,5.85,5.77,5.72,5.61,5.48,5.26,5.17,5.12,5.04,4.96,4.91,4.84,4.79,4.72,4.65,4.63,4.51,4.47,4.44,4.40,4.33,4.28,4.28,4.28,4.24,4.19,4.14,4.10,4.10,4.06,4.00,3.87,3.75,3.69,3.61,3.55,3.49,3.42,3.37,3.29,3.22,3.10,3.03,2.93,2.80,2.80,2.80,2.80,2.74,2.68,2.61,2.52,2.45,2.34,2.32,2.27,2.22,2.15,2.12,2.09,2.06,2.02,2.00};
    // @Todo Change Hardcoded sensor information displayed in Tester View (BetaTesterActivity.java)
    public static String[] sensorValues = new String[3];

    public static Map<String, Pair<Long, com.tyrata.tyrata.data.model.v2.Reading>> activeDevices = new HashMap<>();
    // public static Map<String, Pair<Long, Reading>> activeDevices = new HashMap<>();
    public static Set<String> inactiveDevices = new HashSet<>();
    private static final double[] THICKNESS_MM = {7.9375,7.826375,7.7231875,7.5803125,7.52475,7.39775,7.350125,7.2945625,7.2469375,7.1993125,7.0723125,6.985,6.9453125,6.88975,6.82625,6.76275,6.69925,6.6436875,6.5325625,6.50875,6.50875,6.50875,6.4531875,6.3896875,6.35,6.2865,6.25475,6.0721875,5.889625,5.857875,5.74675,5.7070625,5.667375,5.6276875,5.572125,5.5483125,5.4530625,5.4530625,5.4530625,5.413375,5.4054375,5.3419375,5.2705,5.1831875,5.1514375,5.0720625,5.0165,4.9926875,4.9291875,4.9291875,4.905375,4.905375,4.905375,4.85775,4.8339375,4.778375,4.7625,4.714875,4.6434375,4.5799375,4.54025,4.4529375,4.34975,4.175125,4.1036875,4.064,4.0005,3.937,3.8973125,3.84175,3.8020625,3.7465,3.6909375,3.6750625,3.5798125,3.5480625,3.52425,3.4925,3.4369375,3.39725,3.39725,3.39725,3.3655,3.3258125,3.286125,3.254375,3.254375,3.222625,3.175,3.0718125,2.9765625,2.9289375,2.8654375,2.8178125,2.7701875,2.714625,2.6749375,2.6114375,2.555875,2.460625,2.4050625,2.3256875,2.2225,2.2225,2.2225,2.2225,2.174875,2.12725,2.0716875,2.00025,1.9446875,1.857375,1.8415,1.8018125,1.762125,1.7065625,1.68275,1.6589375,1.635125,1.603375,1.5875};
    private static final double[] TIME_MONTHS = {0,0.25,0.5,0.75,1,1.25,1.5,1.75,2,2.25,2.5,2.75,3,3.25,3.5,3.75,4,4.25,4.5,4.75,5,5.25,5.5,5.75,6,6.25,6.5,6.75,7,7.25,7.5,7.75,8,8.25,8.5,8.75,9,9.25,9.5,9.75,10,10.25,10.5,10.75,11,11.25,11.5,11.75,12,12.25,12.5,12.75,13,13.25,13.5,13.75,14,14.25,14.5,14.75,15,15.25,15.5,15.75,16,16.25,16.5,16.75,17,17.25,17.5,17.75,18,18.25,18.5,18.75,19,19.25,19.5,19.75,20,20.25,20.5,20.75,21,21.25,21.5,21.75,22,22.25,22.5,22.75,23,23.25,23.5,23.75,24,24.25,24.5,24.75,25,25.25,25.5,25.75,26,26.25,26.5,26.75,27,27.25,27.5,27.75,28,28.25,28.5,28.75,29,29.25,29.5,29.75,30};
    private static final double[] MILEAGE = {0,588,1134,1890,2184,2856,3108,3402,3654,3906,4578,5040,5250,5544,5880,6216,6552,6846,7434,7560,7560,7560,7854,8190,8400,8736,8904,9870,10836,11004,11592,11802,12012,12222,12516,12642,13146,13146,13146,13356,13398,13734,14112,14574,14742,15162,15456,15582,15918,15918,16044,16044,16044,16296,16422,16716,16800,17052,17430,17766,17976,18438,18984,19908,20286,20496,20832,21168,21378,21672,21882,22176,22470,22554,23058,23226,23352,23520,23814,24024,24024,24024,24192,24402,24612,24780,24780,24948,25200,25746,26250,26502,26838,27090,27342,27636,27846,28182,28476,28980,29274,29694,30240,30240,30240,30240,30492,30744,31038,31416,31710,32172,32256,32466,32676,32970,33096,33222,33348,33516,33600};

    /* COMMON METHODS */

    // Get the latest reading from a sensor's list of readings
    public static Reading getLastReading(String macAddress) {
        return activeDevices.get(macAddress).second;
    }

    /**
     * Converts thickness to Color code
     * For Demo...
     *
     */
    public static String decodeMessage(String encodedMsg) {
        Double thickness = Double.parseDouble(encodedMsg);
        String decodedMsg;
        if (thickness >= THICKNESS_GREEN[0] && thickness < THICKNESS_GREEN[1])
            decodedMsg = "green";
        else if (thickness >= THICKNESS_YELLOW[0] && thickness < THICKNESS_YELLOW[1])
            decodedMsg = "yellow";
        else if (thickness >= THICKNESS_RED[0] && thickness < THICKNESS_RED[1])
            decodedMsg = "red";
        else
            decodedMsg = "default";
        return decodedMsg;
    }

    // @Todo remove parameters- start and length after Demo AND use data points fetched from cloud
    /**
     * Generate static data-points for Demo
     */
    public static DataPoint[] generateDataPoints(int start, int length, boolean isXinMiles, boolean isYinMm) {
        int i, j;
        if(length == 0) length = 121 - start + 1;
        length = Math.min(length, 121);
        DataPoint[] dataPoints = new DataPoint[length];

        double[] X = isXinMiles ? MILEAGE : TIME_MONTHS;
        double[] Y = isYinMm ? THICKNESS_MM : THICKNESS_INCH;

        for(i = start - 1, j = 0; j < length; i++, j++) {
            dataPoints[j] = new DataPoint(X[i], Y[i]);
        }

        i--;
        if(length == 1)
            tireValues = new int[]{
                    (int) (33000 - MILEAGE[i]), // Change static numbers to dynamic
                    (int) Math.round(30 - TIME_MONTHS[i]),
                    (int) Math.round(THICKNESS_INCH[i])}; // Bad; Put this in getLastDataPoint()

        return dataPoints;
    }

    // @Todo Verify accuracy AND remove starts, lengths
    /**
     * Thickness data analysis and prediction (using Linear Regression)
     */
    public static double getThicknessPredictions(int start, int length, boolean isXinMiles, boolean isYinMm) {
        double[] X = isXinMiles ? MILEAGE : TIME_MONTHS;
        double[] Y = isYinMm ? THICKNESS_MM : THICKNESS_INCH;

        double sumX = 0, sumY = 0;
        double meanX, meanY;
        double xX = 0, xY = 0;
        double beta0, beta1;

        if(length == 0) length = 121 - start + 1;
        length = Math.min(length, 121);

        for(int i = start - 1, j = 0; j < length; i++, j++) {
            sumX  += X[i];
            sumY  += Y[i];
        }

        meanX = sumX / length;
        meanY = sumY / length;

        for(int i = start - 1, j = 0; j < length; i++, j++) {
            xX += (X[i] - meanX) * (X[i] - meanX);
            xY += (X[i] - meanX) * (Y[i] - meanY);
        }

        beta1 = xY / xX;
        beta0 = meanY - beta1 * meanX;

        return (2 - beta0) / beta1;
    }
}