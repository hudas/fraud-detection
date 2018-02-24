package org.ignas.frauddetection.api.evaluation;

public class TransactionLocationDTO {

    private String latitude;

    private String longtitude;

    public TransactionLocationDTO() {
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(String longtitude) {
        this.longtitude = longtitude;
    }
}
