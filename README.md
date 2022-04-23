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

## API

<docgen-index>

- [`addWatcher(...)`](#addwatcher)
- [`removeWatcher(...)`](#removewatcher)
- [`openSettings()`](#opensettings)
- [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### addWatcher(...)

```typescript
addWatcher(options: WatcherOptions, callback: (position?: Location | undefined, error?: CallbackError | undefined) => void) => Promise<string>

// To start listening for changes in the device's location, add a new watcher.
// You do this by calling 'addWatcher' with an options object and a callback. A
// Promise is returned, which resolves to the callback ID used to remove the
// watcher in the future. The callback will be called every time a new location
// is available. Watchers can not be paused, only removed. Multiple watchers may
// exist simultaneously.
await BackgroundLocation.addWatcher(
    // If the "backgroundMessage" option is defined, the watcher will
    // provide location updates whether the app is in the background or the
    // foreground. If it is not defined, location updates are only
    // guaranteed in the foreground. This is true on both platforms.

    // On Android, a notification must be shown to continue receiving
    // location updates in the background. This option specifies the text of
    // that notification.
    backgroundMessage: "Cancel to prevent battery drain.",

    // The title of the notification mentioned above. Defaults to "Using
    // your location".
    backgroundTitle: "Tracking You.",

    // Whether permissions should be requested from the user automatically,
    // if they are not already granted. Defaults to "true".
    requestPermissions: true,

    // If "true", stale locations may be delivered while the device
    // obtains a GPS fix. You are responsible for checking the "time"
    // property. If "false", locations are guaranteed to be up to date.
    // Defaults to "false".
    stale: false,

    // The minimum number of metres between subsequent locations. Defaults
    // to 0.
    distanceFilter: 50
    },
    (location, error) => watcher_callback(location, error),
);

watcher_callback(location, error) {
    if (error) {
        if (error.code === "NOT_AUTHORIZED") {
            if (window.confirm(
                "This app needs your location, " +
                "but does not have permission.\n\n" +
                "Open settings now?"
            )) {
                // It can be useful to direct the user to their device's
                // settings when location permissions have been denied. The
                // plugin provides the 'openSettings' method to do exactly
                // this.
                BackgroundGeolocation.openSettings();
            }
        }
        return console.error(error);
    }

    return console.log(location);
}
```

| Param          | Type                                                                                                                      |
| -------------- | ------------------------------------------------------------------------------------------------------------------------- |
| **`options`**  | <code><a href="#watcheroptions">WatcherOptions</a></code>                                                                 |
| **`callback`** | <code>(position?: <a href="#location">Location</a>, error?: <a href="#callbackerror">CallbackError</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;string&gt;</code>

---

### removeWatcher(...)

```typescript
removeWatcher(options: { id: string; }) => Promise<void>

// When a watcher is no longer needed, it should be removed by calling
// 'removeWatcher' with an object containing its ID.
BackgroundGeolocation.removeWatcher({
    id: watcher_id
});

```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

---

### openSettings()

```typescript
openSettings() => Promise<void>

// It can be useful to direct the user to their device's
// settings when location permissions have been denied. The
// plugin provides the 'openSettings' method to do exactly
// this.
BackgroundGeolocation.openSettings();
```

---

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

{
// Longitude in degrees.
longitude: 131.723423719132,
// Latitude in degrees.
latitude: -22.40106297456,
// Radius of horizontal uncertainty in metres, with 68% confidence.
accuracy: 11,
// Metres above sea level (or null).
altitude: 65,
// Vertical uncertainty in metres, with 68% confidence (or null).
altitudeAccuracy: 4,
// Deviation from true north in degrees (or null).
bearing: 159.60000610351562,
// True if the location was simulated by software, rather than GPS.
simulated: false,
// Speed in metres per second (or null).
speed: 23.51068878173828,
// Time the location was produced, in milliseconds since the unix epoch.
time: 1562731602000
}

#### CallbackError

| Prop       | Type                |
| ---------- | ------------------- |
| **`code`** | <code>string</code> |

</docgen-api>
