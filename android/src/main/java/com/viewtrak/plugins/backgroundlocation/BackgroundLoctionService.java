package com.viewtrak.plugins.backgroundlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.getcapacitor.Logger;
import com.getcapacitor.PluginCall;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.HashSet;

// A bound and started service that is promoted to a foreground service when
// location updates have been requested and the main activity is stopped.
//
// When an activity is bound to this service, frequent location updates are
// permitted. When the activity is removed from the foreground, the service
// promotes itself to a foreground service, and location updates continue. When
// the activity comes back to the foreground, the foreground service stops, and
// the notification associated with that service is removed.
public class BackgroundLoctionService extends Service {

    static final String ACTION_BROADCAST = (BackgroundLoctionService.class.getPackage().getName() + ".broadcast");

    static final String STATUS_BROADCAST = BackgroundLoctionService.class.getPackage().getName() + ".STATUS_BROADCAST";

    private final IBinder binder = new LocalBinder();

    public Notification onlineNotification;
    public Notification offlineNotification;


    // Must be unique for this application.
    private static final int NOTIFICATION_ID = 28351;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String status = intent.getStringExtra("STATUS");

        if (status != null) {
            Intent i = new Intent(STATUS_BROADCAST);
            i.putExtra("STATUS", status);
            i.putExtra("id", intent.getStringExtra("ID"));
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

            isOnline = !isOnline;

            if (isOnline) {
                if (onlineNotification != null) ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
                        NOTIFICATION_ID,
                        onlineNotification
                );
            } else if (offlineNotification != null) ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
                    NOTIFICATION_ID,
                    offlineNotification
            );
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class Watcher {

        public String id;
        public FusedLocationProviderClient client;
        public LocationRequest locationRequest;
        public LocationCallback locationCallback;
        public Notification backgroundNotification;
        public Notification onlineNotification;
        public Notification offlineNotification;
    }

    private Boolean isOnline = false;

    private HashSet<Watcher> watchers = new HashSet<Watcher>();

    Notification getNotification() {
        for (Watcher watcher : watchers) {
            if (watcher.backgroundNotification != null) {
                return watcher.backgroundNotification;
            }
        }
        return null;
    }

    // Handles requests from the activity.
    public class LocalBinder extends Binder {

        void addWatcher(
                final String id,
                Notification backgroundNotification,
                Notification onlineNotification,
                Notification offlineNotification,
                float distanceFilter
        ) {
            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(BackgroundLoctionService.this);
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setMaxWaitTime(1000);
            locationRequest.setInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setSmallestDisplacement(distanceFilter);

            BackgroundLoctionService.this.onlineNotification = onlineNotification;
            BackgroundLoctionService.this.offlineNotification = offlineNotification;

            Watcher watcher = new Watcher();

            LocationCallback callback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    Intent intent = new Intent(ACTION_BROADCAST);
                    intent.putExtra("location", location);
                    intent.putExtra("id", id);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    if (isOnline) {
                        if (watcher.onlineNotification != null) ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
                                NOTIFICATION_ID,
                                watcher.onlineNotification
                        );
                    } else if (watcher.offlineNotification != null) ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(
                            NOTIFICATION_ID,
                            watcher.offlineNotification
                    );
                }

                @Override
                public void onLocationAvailability(LocationAvailability availability) {
                    if (!availability.isLocationAvailable() && BuildConfig.DEBUG) {
                        Logger.debug("Location not available");
                    }
                }
            };

            watcher.id = id;
            watcher.client = client;
            watcher.locationRequest = locationRequest;
            watcher.locationCallback = callback;
            watcher.backgroundNotification = backgroundNotification;
            watcher.onlineNotification = onlineNotification;
            watcher.offlineNotification = offlineNotification;
            watchers.add(watcher);

            if (
                    ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            watcher.client.requestLocationUpdates(watcher.locationRequest, watcher.locationCallback, null);
        }

        void removeWatcher(String id) {
            for (Watcher watcher : watchers) {
                if (watcher.id.equals(id)) {
                    watcher.client.removeLocationUpdates(watcher.locationCallback);
                    watchers.remove(watcher);
                    if(watcher.backgroundNotification != null)
                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, watcher.backgroundNotification);
                    if (getNotification() == null) {
                        stopForeground(true);
                    }
                    if(watcher.backgroundNotification != null)
                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
                    return;
                }
            }
        }

        void onPermissionsGranted() {
            // If permissions were granted while the app was in the background, for example in
            // the Settings app, the watchers need restarting.
            for (Watcher watcher : watchers) {
                watcher.client.removeLocationUpdates(watcher.locationCallback);
                if (
                        ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                watcher.client.requestLocationUpdates(watcher.locationRequest, watcher.locationCallback, null);
            }
        }

        void onActivityStarted() {
            stopForeground(true);
        }

        void onActivityStopped() {
            Notification notification = getNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
            }
        }

        void stopService() {
            BackgroundLoctionService.this.stopSelf();
        }
    }
}