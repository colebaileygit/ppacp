package edu.kit.privateadhocpeering;

import org.spongycastle.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ByteArrayHelper {

    public static List<Byte> toList(byte[] array) {
        List<Byte> list = new ArrayList<>();
        if (array == null) return list;
        for (byte anArray : array) {
            list.add(anArray);
        }

        return list;
    }

    public static byte[] toArray(UUID uuid) {
        String data = uuid.toString().replace("-", "");

        int length = data.length() / 2;

        byte[] result = new byte[length];

        for (int i = 0; i < length; i++) {
            int value = Integer.valueOf(data.substring(2 * i, 2 * i + 2), 16);
            result[i] = (byte) value;
        }

        return result;
    }

    public static byte[] toArray(List<Byte> list) {
        if (list == null) return new byte[0];
        byte[] array = new byte[list.size()];

        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }

    public static byte[] fromLong(long value) {
        byte[] returnData = new byte[8];
        for (int i = 0; i < 8; i++) {
            returnData[i] = (byte) (value & 0xFF);
            value = value >> 8;
            if (value == 0) break;
        }
        return returnData;
    }

    public static long toLong(List<Byte> list) {
        return toLong(toArray(list));
    }
    public static long toLong(byte[] array) {
        if (array.length < 8) throw new IllegalArgumentException("Byte array requires 4 bytes.");
        long nonce = array[7];
        for (int i = 6; i >= 0; i--) {
            nonce = nonce << 8 | array[i] & 0xFF;
        }

        return nonce;
    }

    public static String toString(List<Byte> list) {
        return toString(toArray(list));
    }

    public static String toString(byte[] array) {
        String s = "";
        if (array == null) return s;
        for (byte b : array) {
            String str = Long.toHexString(b);
            String trimmed = (str.length() > 2) ? str.substring(str.length() - 2, str.length()) : str;
            if (trimmed.length() == 1) trimmed = "0" + trimmed;
            s += trimmed + " ";
        }
        return s;
    }

}
