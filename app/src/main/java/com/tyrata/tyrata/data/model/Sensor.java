package com.tyrata.tyrata.data.model;

/**
 * Sensor Model
 */
public class Sensor {
    // Public access required for Firebase
    public String macAddress;
    public String name;
    public String lastReading;
    public boolean isActive;

    public Sensor(String macAddress, String name, String lastReading, boolean isActive) {
        this.macAddress = macAddress;
        this.name = name;
        this.lastReading = lastReading;
        this.isActive = isActive;
    }
}
