package com.example.roadchecker;

public class Data {
    private double latitude;
    private double longitude;
    private double accReadX;
    private double accReadY;
    private double accReadZ;
    private double maxAccRead;

    public Data() {
        // Default constructor required for Firebase
    }



    public Data(double latitude, double longitude, double accReadX, double accReadY, double accReadZ, double maxAccRead) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accReadX = accReadX;
        this.accReadY = accReadY;
        this.accReadZ = accReadZ;
        this.maxAccRead = maxAccRead;
    }
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setAccReadX(double accReadX) {
        this.accReadX = accReadX;
    }

    public double getAccReadX() {
        return accReadX;
    }

    public double getAccReadY() {
        return accReadY;
    }

    public void setAccReadY(double accReadY) {
        this.accReadY = accReadY;
    }

    public double getAccReadZ() {
        return accReadZ;
    }

    public void setAccReadZ(double accReadZ) {
        this.accReadZ = accReadZ;
    }

    public double getMaxAccRead() {
        return maxAccRead;
    }

    public void setMaxAccRead(double maxAccRead) {
        this.maxAccRead = maxAccRead;
    }
}

