package org.ignas.frauddetection.shared;

import java.util.Objects;

public class Location {

    private float latitude;

    private float longtitude;

    public Location() {
    }

    public Location(float latitude, float longtitude) {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public static Location fromShortCode(String code) {
        String[] coordinates = code.split(",");

        float latitude = Float.parseFloat(coordinates[0].trim());
        float longtitude = Float.parseFloat(coordinates[1].trim());

        return new Location(latitude, longtitude);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Float.compare(location.latitude, latitude) == 0 &&
            Float.compare(location.longtitude, longtitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longtitude);
    }

    @Override
    public String toString() {
        return String.format("%f, %f", latitude, longtitude);
    }
}
