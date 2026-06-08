package io.nativeplanet.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver to start NativePlanet Controller Service on device boot.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "NativePlanetBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received boot intent: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            Log.i(TAG, "Starting NativePlanet Controller Service");

            Intent serviceIntent = new Intent(context, NativePlanetControllerService.class);
            context.startService(serviceIntent);
        }
    }
}
