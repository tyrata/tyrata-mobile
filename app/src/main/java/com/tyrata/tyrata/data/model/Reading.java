package com.tyrata.tyrata.data.model;

/**
 * Readings format
 */
public class Reading {
    // Public access required for Firebase
    public int value;
    public int difference;
    public long elapsedTime; // Time elapsed from the time of sensor power on
    public int battery;

    public Reading(int value, int difference, long elapsedTime, int battery) {
        this.value = value;
        this.difference = difference;
        this.elapsedTime = elapsedTime;
        this.battery = battery;
    }
}
