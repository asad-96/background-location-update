# background-location-update

A Capacitor plugin which lets you receive location updates even while the app is backgrounded.

## Install

```bash
npm install background-location-update
npx cap sync
```

## Usage

```typescript
import { BackgroundLocation, Location } from 'background-location-update';
```

### iOS

Add the following keys to `Info.plist.`:

```xml
<dict>
  ...
  <key>NSLocationWhenInUseUsageDescription</key>
  <string>We need to track your location</string>
  <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>We need to track your location while your device is locked.</string>
  <key>UIBackgroundModes</key>
  <array>
    <string>location</string>
  </array>
  ...
</dict>
```

### Android

Configure `AndroidManifest.xml`:

```xml
<manifest>
    <application>
        <service
            android:name="com.viewtrak.plugins.backgroundlocation.BackgroundLoctionService"
            android:enabled="true"
            android:exported="true"
            android:foregroundServiceType="location" />
    </application>

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-feature android:name="android.hardware.location.gps" />
</manifest>
```

Configration specific to Android can be made in `strings.xml`:

```xml
<resources>
    <!--
        The channel name for the background notification. This will be visible
        when the user presses & holds the notification. It defaults to
        "Background Tracking".
    -->
    <string name="capacitor_background_location_notification_channel_name">
        Background Tracking
    </string>

    <!--
        The icon to use for the background notification. Note the absence of a
        leading "@". It defaults to "mipmap/ic_launcher", the app's launch icon.

        If a raster image is used to generate the icon (as opposed to a vector
        image), it must have a transparent background. To make sure your image
        is compatible, select "Notification Icons" as the Icon Type when
        creating the image asset in Android Studio.
    -->
    <string name="capacitor_background_location_notification_icon">
        drawable/ic_tracking
    </string>
</resources>

```

## API

<docgen-index>

* [`addWatcher(...)`](#addwatcher)
* [`removeWatcher(...)`](#removewatcher)
* [`openSettings()`](#opensettings)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### addWatcher(...)

```typescript
addWatcher(options: WatcherOptions, callback: (position?: Location | undefined, error?: CallbackError | undefined) => void) => Promise<string>
```

| Param          | Type                                                                                                                      |
| -------------- | ------------------------------------------------------------------------------------------------------------------------- |
| **`options`**  | <code><a href="#watcheroptions">WatcherOptions</a></code>                                                                 |
| **`callback`** | <code>(position?: <a href="#location">Location</a>, error?: <a href="#callbackerror">CallbackError</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### removeWatcher(...)

```typescript
removeWatcher(options: { id: string; }) => Promise<void>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

--------------------


### openSettings()

```typescript
openSettings() => Promise<void>
```

--------------------


### Interfaces


#### WatcherOptions

| Prop                     | Type                 |
| ------------------------ | -------------------- |
| **`backgroundMessage`**  | <code>string</code>  |
| **`backgroundTitle`**    | <code>string</code>  |
| **`requestPermissions`** | <code>boolean</code> |
| **`stale`**              | <code>boolean</code> |
| **`distanceFilter`**     | <code>number</code>  |


#### Location

| Prop                   | Type                        |
| ---------------------- | --------------------------- |
| **`latitude`**         | <code>number</code>         |
| **`longitude`**        | <code>number</code>         |
| **`accuracy`**         | <code>number</code>         |
| **`altitude`**         | <code>number \| null</code> |
| **`altitudeAccuracy`** | <code>number \| null</code> |
| **`simulated`**        | <code>boolean</code>        |
| **`bearing`**          | <code>number \| null</code> |
| **`speed`**            | <code>number \| null</code> |
| **`time`**             | <code>number \| null</code> |


#### CallbackError

| Prop       | Type                |
| ---------- | ------------------- |
| **`code`** | <code>string</code> |

</docgen-api>
