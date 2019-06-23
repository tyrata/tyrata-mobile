package com.tyrata.tyrata.data.model;

public class Reading {

    // Public for Firebase
    public long time; // Time elapsed from the time of sensor power on
    public double voltage; // Battery Voltage remaining
    public double temp; // Temperature of the device
    public float value; // value of reading
    public int type; // 1 for freq. - 0 for cap.

    public Reading(long time, int voltage, int temperature, long reading, int type) {
        this.convertTemp(temperature);
        this.convertVolt(voltage);
        this.time = time;
        this.type = type;
        convertValue(reading);
    }

    protected void convertValue(long valueReading) {
        if(this.type == 1) {
            this.value = valueReading;
        } else if(this.type == 0) {
            this.value = (float) (valueReading * (8.192 / (2^24)));
        } else {
            this.value = 0;
        }
    }

    protected void convertTemp(int tempReading) {
        this.temp = tempReading * .0625;
    }

    protected void convertVolt(double voltReading) {
    this.voltage = voltReading / 10;
    }
}
