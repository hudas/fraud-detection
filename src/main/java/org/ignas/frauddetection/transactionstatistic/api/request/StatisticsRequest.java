package org.ignas.frauddetection.transactionstatistic.api.request;

import org.ignas.frauddetection.shared.Location;

import java.time.LocalDateTime;

public class StatisticsRequest {

    private String requestByDebtorId;

    private String requestByCreditorId;

    private Location requestByLocation;

    private LocalDateTime requestByTime;

    public StatisticsRequest() {
    }

    public StatisticsRequest(
        String requestByDebtorId,
        String requestByCreditorId,
        Location requestByLocation,
        LocalDateTime requestByTime) {
        this.requestByDebtorId = requestByDebtorId;
        this.requestByCreditorId = requestByCreditorId;
        this.requestByLocation = requestByLocation;
        this.requestByTime = requestByTime;
    }

    public String getRequestByDebtorId() {
        return requestByDebtorId;
    }

    public String getRequestByCreditorId() {
        return requestByCreditorId;
    }

    public Location getRequestByLocation() {
        return requestByLocation;
    }

    public LocalDateTime getRequestByTime() {
        return requestByTime;
    }
}
