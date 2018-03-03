package org.ignas.frauddetection.shared;

public class Location {

    private float latitude;

    private float longtitude;

    public Location() {
    }

    public Location(float latitude, float longtitude) {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    /**
     * Calculates euler distance in degrees
     * @param to
     * @return
     */
    public double distanceTo(Location to) {
        float latitudeDiff = this.latitude - to.latitude;
        float longtitudeDiff = this.longtitude - to.longtitude;

        return Math.sqrt(latitudeDiff * latitudeDiff + longtitudeDiff * longtitudeDiff);
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(float longtitude) {
        this.longtitude = longtitude;
    }
}
