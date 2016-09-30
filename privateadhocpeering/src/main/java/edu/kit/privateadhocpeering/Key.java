package edu.kit.privateadhocpeering;

import android.util.Log;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Key {

    static int TICK_LENGTH = 512;

    private UUID uuid;

    private static final String TAG = "KEY_ENCRYPT";

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    Key() { this(generateKey()); }

    Key(UUID uuid) {
        this.uuid = uuid;
    }

    private static UUID generateKey() {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            kgen.init(128, sr);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            return UUID.nameUUIDFromBytes(raw);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "Basic algorithms not supported.");
            return null;
        }
    }

    // TODO: make not public
   public byte[] getAdvertisingData() {
       byte[] data = encryptUniqueData(getCurrentTimeBase(TICK_LENGTH));
       if (data == null) return new byte[0];
       byte[] result = new byte[8];
       System.arraycopy(data, 0, result, 0, 8);
       return result;
    }

    UUID getServiceUUID() {
        byte[] data = encryptUniqueData(getCurrentTimeBase(TICK_LENGTH));
        byte[] result = new byte[8];
        System.arraycopy(data, 8, result, 0, 8);
        return compileServiceUuid(result);
    }

    // Create UUID with the form "9f4a76ec-adbb-42c0-ac30-615c43ff98db"
    // from first 8 encrypted key bytes and final 8 base uuid bytes
    private UUID compileServiceUuid(byte[] data) {
        String prefix = ByteArrayHelper.toString(data);

        String[] split = prefix.split(" ");

        if (split.length < 8) throw new IllegalStateException("Parsed key data incorrect.");

        String uuid = split[0] + split[1] + split[2] + split[3]
                + "-" + split[4] + split[5] + "-" + split[6] + split[7]
                + "-" + AuthenticationGATT.base_uuid.toString().substring(19);

        return UUID.fromString(uuid);
    }

    byte[] encryptUniqueData(byte[] data) {
        byte[] keyData = ByteArrayHelper.toArray(uuid);

        Cipher c;
        try {
            c = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "ECB Algorithm not available");
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "PKCS5Padding not available");
            return null;
        }

        SecretKeySpec skeySpec = new SecretKeySpec(keyData, "AES");
        try {
            c.init(Cipher.ENCRYPT_MODE, skeySpec);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Key invalid.");
            e.printStackTrace();
            return null;
        }

        byte[] encrypted = new byte[0];
        try {
            encrypted = c.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }

        return encrypted;
    }

    byte[] decryptUniqueData(byte[] data) {
        byte[] keyData = ByteArrayHelper.toArray(uuid);

        Cipher c;
        try {
            c = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "ECB Algorithm not available");
            return null;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "PKCS5Padding not available");
            return null;
        }

        SecretKeySpec skeySpec = new SecretKeySpec(keyData, "AES");
        try {
            c.init(Cipher.DECRYPT_MODE, skeySpec);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Key invalid.");
            e.printStackTrace();
            return null;
        }

        byte[] decrypted = new byte[0];
        try {
            decrypted = c.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }

        return decrypted;
    }

    private byte[] getCurrentTimeBase(int secondsOffset) {
        long time = System.currentTimeMillis();
        long seconds = time / 1000;
        byte[] bytes = ByteArrayHelper.fromLong(seconds / secondsOffset);
        byte[] response = new byte[16];
        System.arraycopy(bytes, 0, response, 0, bytes.length);
        return response;
    }

    byte[] encrypt(byte[] msgCount, byte[] sessionIV, byte[] data) {
        byte[] keyData = ByteArrayHelper.toArray(uuid);

        byte[] iv = generateIV(msgCount, sessionIV);

        // encrypt
        AEADParameters parameters = new AEADParameters(
                new KeyParameter(keyData), 96, iv, new byte[0]);
        GCMBlockCipher gcmEngine = new GCMBlockCipher(new AESFastEngine());
        gcmEngine.init(true, parameters);


        byte[] encrypted = new byte[data.length + 14];

        int encLen = gcmEngine.processBytes(data, 0, data.length, encrypted,
                2);
        try {
            encLen += gcmEngine.doFinal(encrypted, encLen + 2);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            return null;
        }

        System.arraycopy(iv, 0, encrypted, 0, 2);

        return encrypted;
    }

    byte[] decrypt(byte[] sessionIV, byte[] rawData) {
        byte[] keyData = ByteArrayHelper.toArray(uuid);

        byte[] nonce = new byte[2];
        System.arraycopy(rawData, 0, nonce, 0, 2);
        byte[] iv = generateIV(nonce, sessionIV);

        // encrypt
        AEADParameters parameters = new AEADParameters(
                new KeyParameter(keyData), 96, iv, new byte[0]);
        GCMBlockCipher gcmEngine = new GCMBlockCipher(new AESFastEngine());
        gcmEngine.init(false, parameters);


        byte[] decrypted = new byte[rawData.length - 14];

        int decLen = gcmEngine.processBytes(rawData, 2, rawData.length - 2, decrypted,
                0);
        try {
            decLen += gcmEngine.doFinal(decrypted, decLen);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            return null;
        }

        return decrypted;
    }

    private byte[] generateIV(byte[] sessionIV) {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "SHA1PRNG not available.");
            return null;
        }

        byte[] nonce = new byte[2];
        sr.nextBytes(nonce);

        return generateIV(nonce, sessionIV);
    }

    private byte[] generateIV(byte[] counter, byte[] sessionIV) {
        byte[] timeBase = getCurrentTimeBase(TICK_LENGTH);

        Log.d("KEYING_IV_BASE", Arrays.toString(timeBase));

        byte[] iv = new byte[10];

        System.arraycopy(counter, 0, iv, 0, 2);
        System.arraycopy(sessionIV, 0, iv, 2, 4);
        System.arraycopy(timeBase, 0, iv, 6, 4);

        return iv;
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == this.getClass() && ((Key) o).uuid.equals(this.uuid);
    }
}
