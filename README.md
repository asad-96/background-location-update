# background-location-update

A Capacitor plugin which lets you receive location updates even while the app is backgrounded.

## Install

```bash
npm install background-location-update
npx cap sync
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
