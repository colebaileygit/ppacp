package edu.kit.privateadhocpeering;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class TimingReceiver extends BroadcastReceiver {

    public static final String RESET_INTENT = "edu.kit.bleadvertising.idchange";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, DiscoveryService.class);
            context.startService(i);
        } else if (RESET_INTENT.equals(intent.getAction())) {
            long delay = secureRandomRange(5000, 10000);
            Log.i("TIMING", "Sleeping with delay " + delay);
            Intent i = new Intent(context, DiscoveryService.class);
            i.setAction(DiscoveryService.RESET_ADVERTISING);
            i.putExtra(DiscoveryService.SILENT_DURATION, delay);
            context.startService(i);
        }

        setNextAlarm(context);
    }

    static void setNextAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TimingReceiver.class);
        intent.setAction(RESET_INTENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        long timeMillis = System.currentTimeMillis() + 5000;
        long tickNumber =  timeMillis / (1000 * Key.TICK_LENGTH);
        long nextTickChangeMillis = (tickNumber + 1) * Key.TICK_LENGTH * 1000;
        long offset = secureRandomRange(2500, 5000);

        am.setExact(AlarmManager.RTC_WAKEUP, nextTickChangeMillis - offset, alarmIntent);
    }

    private static long secureRandomRange(long min, long max) {
        long num;
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
            num = sr.nextLong();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e("RNG", "SHA1PRNG not available.");
            Random r = new Random();
            num = r.nextLong();
        }

        // scale random number to given range
        long factor = Long.MAX_VALUE / (max - min);
        long scaled = Math.abs(num) / factor;

        return scaled + min;
    }

}
