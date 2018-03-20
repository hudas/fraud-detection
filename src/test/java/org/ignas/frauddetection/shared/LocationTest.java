package org.ignas.frauddetection.shared;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationTest {

    @Test
    void distanceTo() {
        Location first = new Location(54.4f, 25.3f);
        Location second = new Location(54.0f, 25.0f);

        double distance = first.distanceTo(second);

        Assertions.assertEquals(0.5f, distance, 0.001f);
    }

    @Test
    void distanceToOpposedSingleCoord() {
        Location first = new Location(53.5f, 25.3f);
        Location second = new Location(54.0f, 25.0f);

        double distance = first.distanceTo(second);

        Assertions.assertEquals(0.583f, distance, 0.0001f);
    }

    @Test
    void distanceToSame() {
        Location first = new Location(54.0f, 25.0f);
        Location second = new Location(54.0f, 25.0f);

        double distance = first.distanceTo(second);

        Assertions.assertEquals(0f, distance, 0.0001f);
    }
}
