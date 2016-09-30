package edu.kit.privateadhocpeering;

import android.util.Log;

/**
 * Created by Me on 5/26/2016.
 */
public class ByteCounter {

    private byte[] counter = { 0, 0 };

    byte[] getNextCount() {
        byte[] result = counter;

        if (counter[1] != (byte)0xFF) {
            counter[1]++;
        } else if (counter[0] != (byte)0xFF) {
            counter[0]++;
        } else {
            Log.e("COUNTER", "Maximum reached.");
        }

        return result;
    }

}
