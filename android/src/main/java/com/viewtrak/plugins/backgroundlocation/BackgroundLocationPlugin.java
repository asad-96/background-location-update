package com.viewtrak.plugins.backgroundlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONObject;

@CapacitorPlugin(
        name = "BackgroundLocation",
        permissions = {
                @Permission(alias = "location", strings = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION })
        }
)
public class BackgroundLocationPlugin extends Plugin {

    private PluginCall callPendingPermissions = null;
    private Boolean stoppedWithoutPermissions = false;

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void addWatcher(final PluginCall call) {
        if (service == null) {
            call.reject("Service not running.");
            return;
        }
        call.setKeepAlive(true);
        if (getPermissionState("location") != PermissionState.GRANTED) {
            if (call.getBoolean("requestPermissions", true)) {
                callPendingPermissions = call;
                requestAllPermissions(call, "locationPermsCallback");
            } else {
                call.reject("Permission denied.", "NOT_AUTHORIZED");
            }
        } else {
            if (!isLocationEnabled(getContext())) {
                call.reject("Location services disabled.", "NOT_AUTHORIZED");
            }
        }
        if (call.getBoolean("stale", false)) {
            if (
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
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
            LocationServices
                    .getFusedLocationProviderClient(getContext())
                    .getLastLocation()
                    .addOnSuccessListener(
                            getActivity(),
                            new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        call.resolve(formatLocation(location));
                                    }
                                }
                            }
                    );
        }
        Notification backgroundNotification = null;
        Notification onlineNotification = null;
        Notification offlineNotification = null;
        String backgroundMessage = call.getString("backgroundMessage");
        if (backgroundMessage != null) {
            backgroundNotification = makeNotification(call, backgroundMessage).build();

            // Online Notification
            // Get the layouts to use in the custom notification
//            RemoteViews notificationLayout = new RemoteViews(getContext().getPackageName(), R.layout.notification_user_status);


            Intent onlineIntent = new Intent(getContext(), BackgroundLoctionService.class);
            onlineIntent.putExtra("STATUS","online");
            onlineIntent.putExtra("ID",call.getCallbackId());

            PendingIntent pendingIntent1 = PendingIntent.getService(getContext(), 7, onlineIntent, 0);
            onlineNotification = makeNotification(call, backgroundMessage)
                    .addAction(android.R.drawable.presence_offline,"Mark Online", pendingIntent1)
                    .build();

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                onlineNotification = makeNotification(call, backgroundMessage)
//                        .setCustomContentView(notificationLayout)
//                        .build();
//            }

            // Offline Notification
            Intent offlineIntent = new Intent(getContext(), BackgroundLoctionService.class);
            offlineIntent.putExtra("STATUS", "offline");
            offlineIntent.putExtra("ID", call.getCallbackId());

            offlineNotification =
                    makeNotification(call, backgroundMessage)
                            .addAction(android.R.drawable.presence_online, "Mark Offline", PendingIntent.getService(getContext(), 7, offlineIntent, 0))
                            .build();
            //            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getContext(), 7, onlineIntent, 0);
            //
            //            builder.setActions(new Notification.Action(R.drawable.ic_transparent,"Mark Online", pendingIntent1));

        }
        service.addWatcher(
                call.getCallbackId(),
                backgroundNotification,
                onlineNotification,
                offlineNotification,
                call.getFloat("distanceFilter", 0f)
        );
    }

    // Sends messages to the service.
    private BackgroundLoctionService.LocalBinder service = null;

    @PermissionCallback
    private void locationPermsCallback(PluginCall call) {
        //        for(int result : grantResults) {
        if (getPermissionState("location") == PermissionState.DENIED) {
            callPendingPermissions.reject("User denied location permission", "NOT_AUTHORIZED");
            return;
        }
        //        }
        callPendingPermissions = null;
        if (service != null) {
            service.onPermissionsGranted();
        }
    }

    @PluginMethod
    public void removeWatcher(PluginCall call) {
        String callbackId = call.getString("id");
        if (callbackId == null) {
            call.reject("Missing id.");
            return;
        }
        if (service != null) service.removeWatcher(callbackId);
        PluginCall savedCall = bridge.getSavedCall(callbackId);
        if (savedCall != null) {
            savedCall.release(bridge);
        }
        call.resolve();
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        getContext().startActivity(intent);
        call.resolve();
    }

    // Checks if device-wide location services are disabled
    private static Boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm != null && lm.isLocationEnabled();
        } else {
            return (
                    Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) !=
                            Settings.Secure.LOCATION_MODE_OFF
            );
        }
    }

    // Receives ONLINE OFFLINE status from the service.
    private class StatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            String status = intent.getStringExtra("STATUS");
            PluginCall call = bridge.getSavedCall(id);
        }
    }

    // Receives messages from the service.
    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            PluginCall call = bridge.getSavedCall(id);
            if (call == null) {
                return;
            }
            Location location = intent.getParcelableExtra("location");
            if (location == null) {
                if (BuildConfig.DEBUG) {
                    call.reject("No locations received");
                }
                return;
            }
            call.resolve(formatLocation(location));
        }
    }

    private static JSObject formatLocation(Location location) {
        JSObject obj = new JSObject();
        obj.put("latitude", location.getLatitude());
        obj.put("longitude", location.getLongitude());
        // The docs state that all Location objects have an accuracy, but then why is there a
        // hasAccuracy method? Better safe than sorry.
        obj.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL);
        obj.put("altitude", location.hasAltitude() ? location.getAltitude() : JSONObject.NULL);
        if (Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) {
            obj.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
        } else {
            obj.put("altitudeAccuracy", JSONObject.NULL);
        }
        // In addition to mocking locations in development, Android allows the
        // installation of apps which have the power to simulate location
        // readings in other apps.
        obj.put("simulated", location.isFromMockProvider());
        obj.put("speed", location.hasSpeed() ? location.getSpeed() : JSONObject.NULL);
        obj.put("bearing", location.hasBearing() ? location.getBearing() : JSONObject.NULL);
        obj.put("time", location.getTime());
        return obj;
    }

    // Gets the identifier of the app's resource by name, returning 0 if not found.
    private int getAppResourceIdentifier(String name, String defType) {
        return getContext().getResources().getIdentifier(name, defType, getContext().getPackageName());
    }

    // Gets a string from the app's strings.xml file, resorting to a fallback if it is not defined.
    private String getAppString(String name, String fallback) {
        int id = getAppResourceIdentifier(name, "string");
        return id == 0 ? fallback : getContext().getString(id);
    }

    @Override
    public void load() {
        super.load();

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    BackgroundLoctionService.class.getPackage().getName(),
                    getAppString("capacitor_background_location_notification_channel_name", "Background Tracking"),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }

        this.getContext()
                .bindService(
                        new Intent(this.getContext(), BackgroundLoctionService.class),
                        new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder binder) {
                                BackgroundLocationPlugin.this.service = (BackgroundLoctionService.LocalBinder) binder;
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                Log.e("disconnected", name.getPackageName());
                            }
                        },
                        Context.BIND_AUTO_CREATE
                );

        LocalBroadcastManager
                .getInstance(this.getContext())
                .registerReceiver(new ServiceReceiver(), new IntentFilter(BackgroundLoctionService.ACTION_BROADCAST));

        LocalBroadcastManager
                .getInstance(this.getContext())
                .registerReceiver(new StatusReceiver(), new IntentFilter(BackgroundLoctionService.STATUS_BROADCAST));
    }

    @Override
    protected void handleOnStart() {
        if (service != null) {
            service.onActivityStarted();
            if (stoppedWithoutPermissions && hasRequiredPermissions()) {
                service.onPermissionsGranted();
            }
        }
        super.handleOnStart();
    }

    @Override
    protected void handleOnStop() {
        if (service != null) {
            service.onActivityStopped();
        }
        stoppedWithoutPermissions = !hasRequiredPermissions();
        super.handleOnStop();
    }

    @Override
    protected void handleOnDestroy() {
        if (service != null) {
            service.stopService();
        }
        super.handleOnDestroy();
    }

    private Notification.Builder makeNotification(final PluginCall call, String backgroundMessage) {
        Notification.Builder builder = new Notification.Builder(getContext())
                .setContentTitle(call.getString("backgroundTitle", "Using your location"))
                .setContentText(backgroundMessage)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis());

        try {
            String name = getAppString("capacitor_background_location_notification_icon", "mipmap/ic_launcher");
            String[] parts = name.split("/");
            // It is actually necessary to set a valid icon for the notification to behave
            // correctly when tapped. If there is no icon specified, tapping it will open the
            // app's settings, rather than bringing the application to the foreground.
            builder.setSmallIcon(getAppResourceIdentifier(parts[1], parts[0]));
        } catch (Exception e) {
            Logger.error("Could not set notification icon", e);
        }

        Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(getContext().getPackageName());
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            builder.setContentIntent(
                    PendingIntent.getActivity(getContext(), 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE)
            );
        }

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(BackgroundLoctionService.class.getPackage().getName());
        }

        return builder;
    }
}