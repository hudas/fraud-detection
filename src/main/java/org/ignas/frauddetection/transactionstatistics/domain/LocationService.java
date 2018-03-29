package org.ignas.frauddetection.transactionstatistics.domain;

import org.ignas.frauddetection.shared.Location;

public class LocationService {

    public static Location toNearestArea(Location location) {

        return new Location(
            towardsCoordinateCenter(location.getLatitude()),
            towardsCoordinateCenter(location.getLongtitude())
        );
    }

    private static float towardsCoordinateCenter(float coordinate) {
        return (float) (coordinate > 0 ? Math.floor(coordinate) : Math.ceil(coordinate));
    }
}
