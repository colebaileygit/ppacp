package edu.kit.bletest.dummy;

import android.graphics.Color;

/**
 * Created by Me on 5/7/2016.
 */
public class Beacon {
    public String identifier;
    public BeaconStatus status;

    public Beacon(String id, BeaconStatus status) {
        identifier = id;
        this.status = status;
    }

    public enum BeaconStatus {
        ACTIVE(Color.GREEN),
        INACTIVE(Color.RED);

        private final int color;

        BeaconStatus(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}
