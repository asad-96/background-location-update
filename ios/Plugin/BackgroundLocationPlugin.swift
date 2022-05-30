import Foundation
import Capacitor
import CoreLocation
import UserNotifications


/**
 * Notificaton types for NSNotificationCenter
 */
@objc public enum AppInBackground: Int {
  case ShowLocationNotification
  
  public func name() -> String {
    switch self {
    case .ShowLocationNotification: return "CAPShowLocationNotification"
    }
  }
}
/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */

// Avoids a bewildering type warning.
let null = Optional<Double>.none as Any

func formatLocation(_ location: CLLocation) -> PluginCallResultData {
    var simulated = false;
    if #available(iOS 15, *) {
        // Prior to iOS 15, it was not possible to detect simulated locations.
        // But in general, it is very difficult to simulate locations on iOS in
        // production.
        if location.sourceInformation != nil {
            simulated = location.sourceInformation!.isSimulatedBySoftware;
        }
    }
    return [
        "latitude": location.coordinate.latitude,
        "longitude": location.coordinate.longitude,
        "accuracy": location.horizontalAccuracy,
        "altitude": location.altitude,
        "altitudeAccuracy": location.verticalAccuracy,
        "simulated": simulated,
        "speed": location.speed < 0 ? null : location.speed,
        "bearing": location.course < 0 ? null : location.course,
        "time": NSNumber(
            value: Int(
                location.timestamp.timeIntervalSince1970 * 1000
            )
        ),
    ]
}

class Watcher {
    let callbackId: String
    let locationManager: CLLocationManager = CLLocationManager()
    private let created = Date()
    private let allowStale: Bool
    private var isUpdatingLocation: Bool = false
    init(_ id: String, stale: Bool) {
        callbackId = id
        allowStale = stale
    }
    func start() {
        // Avoid unnecessary calls to startUpdatingLocation, which can
        // result in extraneous invocations of didFailWithError.
        if !isUpdatingLocation {
            locationManager.startUpdatingLocation()
            isUpdatingLocation = true
        }
    }
    func stop() {
        if isUpdatingLocation {
            locationManager.stopUpdatingLocation()
            isUpdatingLocation = false
        }
    }
    func isLocationValid(_ location: CLLocation) -> Bool {
        return (
            allowStale ||
            location.timestamp >= created
        )
    }
}



@objc(BackgroundLocationPlugin)
public class BackgroundLocationPlugin: CAPPlugin, CLLocationManagerDelegate {
    private var watchers = [Watcher]()
    private let eventName = "BACKGROUNDLOCATIONNOTIFY"
    private var isBackground = false
    private var isActive = false
    private var notificationTitle = "Notification Title"
    private var notificationSubtitle = "Notification Subtitle"
    private var stringActionOnline = "Mark Online"
    private var stringActionOffline = "Mark Offline"
    
    @objc public override func load() {
        UIDevice.current.isBatteryMonitoringEnabled = true
        NotificationCenter.default.addObserver(self, selector: #selector(showBackgroundLocationNotification), name: NSNotification.Name(AppInBackground.ShowLocationNotification.name()), object: nil)
    }
    
    @objc func addWatcher(_ call: CAPPluginCall) {
        call.keepAlive = true

        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            let background = call.getString("backgroundMessage") != nil
            let watcher = Watcher(
                call.callbackId,
                stale: call.getBool("stale") ?? false
            )
            let manager = watcher.locationManager
            manager.delegate = self
            let externalPower = [
                .full,
                .charging
            ].contains(UIDevice.current.batteryState)
            manager.desiredAccuracy = (
                externalPower
                ? kCLLocationAccuracyBestForNavigation
                : kCLLocationAccuracyBest
            )
            manager.distanceFilter = call.getDouble(
                "distanceFilter"
            ) ?? kCLDistanceFilterNone;
            manager.allowsBackgroundLocationUpdates = background
            self.watchers.append(watcher)
            self.isBackground = background
            self.isActive = call.getBool("isActive", false)
//            if (call.getBool("isActive", false)) {
                self.notificationTitle = call.getString("backgroundTitle", "App is tracking your location in background")
                self.notificationSubtitle = call.getString("backgroundMessage", "Mark offline to stop location tracking")
                self.stringActionOnline = call.getString("actionOnline", "Mark Online")
                self.stringActionOffline = call.getString("actionOffline", "Mark Offline")
//            }
            if call.getBool("requestPermissions") != false {
                let status = CLLocationManager.authorizationStatus()
                if [
                    .notDetermined,
                    .denied,
                    .restricted,
                ].contains(status) {
                    return (
                        background
                        ? manager.requestAlwaysAuthorization()
                        : manager.requestWhenInUseAuthorization()
                    )
                }
                if (
                    background && status == .authorizedWhenInUse
                ) {
                    // Attempt to escalate.
                    manager.requestAlwaysAuthorization()
                }
            }
            return watcher.start()
        }
    }

    @objc func showBackgroundLocationNotification() {
        if (!isActive && isBackground) {
            isActive = true
            showNotification(notificationTitle, notificationSubtitle, stringActionOffline)
        }
        else if (isActive && isBackground) {
            isActive = false
            showNotification(notificationTitle, notificationSubtitle, stringActionOnline)
        }
    }
    
    @objc func removeWatcher(_ call: CAPPluginCall) {
        // CLLocationManager requires main thread
        DispatchQueue.main.async {
            if let callbackId = call.getString("id") {
                if let index = self.watchers.firstIndex(
                    where: { $0.callbackId == callbackId }
                ) {
                    self.watchers[index].locationManager.stopUpdatingLocation()
                    self.watchers.remove(at: index)
                }
                if let savedCall = self.bridge?.savedCall(withID: callbackId) {
                    self.bridge?.releaseCall(savedCall)
                }
                return call.resolve()
            }
            return call.reject("No callback ID")
        }
    }
    
    @objc func openSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let settingsUrl = URL(
                string: UIApplication.openSettingsURLString
            ) else {
                return call.reject("No link to settings available")
            }

            if UIApplication.shared.canOpenURL(settingsUrl) {
                UIApplication.shared.open(settingsUrl, completionHandler: {
                    (success) in
                    if (success) {
                        return call.resolve()
                    } else {
                        return call.reject("Failed to open settings")
                    }
                })
            } else {
                return call.reject("Cannot open settings")
            }
        }
    }
    
    public func showNotification(_ title: String,_ subTitle: String,_ actionString: String) {
        let center = UNUserNotificationCenter.current()
        
        center.requestAuthorization(options: [.alert, .sound], completionHandler: { granted, error in
        })
        
        let content = UNMutableNotificationContent()
        content.title = title
//        content.body = body
        content.subtitle = subTitle
        content.categoryIdentifier = "activeCate"

        let action = UNNotificationAction(identifier: "active_action", title: actionString, options: UNNotificationActionOptions.init(rawValue: 0))

        let actionCate = UNNotificationCategory(identifier: "activeCate", actions: [action], intentIdentifiers: [])

        center.setNotificationCategories([actionCate])
        center.delegate = self
        
        // Step 3: Create notification trigger
        let date = Date().addingTimeInterval(2)
        let dateComponents = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        // Step 4: Create the request
        
        let uuidString = "backgroundlocationupdateplugin"
        let request = UNNotificationRequest(identifier: uuidString, content: content, trigger: trigger)
        
        // Step 5: Register the request
        
        center.add(request, withCompletionHandler: {(error) in
            NSLog("Error")
        })
    }
    
    public func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        if let watcher = self.watchers.first(
            where: { $0.locationManager == manager }
        ) {
            if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
                if let clErr = error as? CLError {
                    if clErr.code == .locationUnknown {
                        // This error is sometimes sent by the manager if
                        // it cannot get a fix immediately.
                        return
                    } else if (clErr.code == .denied) {
                        watcher.stop()
                        return call.reject(
                            "Permission denied.",
                            "NOT_AUTHORIZED"
                        )
                    }
                }
                return call.reject(error.localizedDescription, nil, error)
            }
        }
    }
    
    public func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        if let location = locations.last {
            if let watcher = self.watchers.first(
                where: { $0.locationManager == manager }
            ) {
                if watcher.isLocationValid(location) {
                    if let call = self.bridge?.savedCall(withID: watcher.callbackId) {
                        return call.resolve(formatLocation(location))
                    }
                }
            }
        }
    }
    
    public func locationManager(
        _ manager: CLLocationManager,
        didChangeAuthorization status: CLAuthorizationStatus
    ) {
        // If this method is called before the user decides on a permission, as
        // it is on iOS 14 when the permissions dialog is presented, we ignore
        // it.
        if status != .notDetermined {
            if let watcher = self.watchers.first(
                where: { $0.locationManager == manager }
            ) {
                return watcher.start()
            }
        }
    }
    
}

extension BackgroundLocationPlugin: UNUserNotificationCenterDelegate {

        public func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
            completionHandler([.alert])
        }
        
        public func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
            switch response.actionIdentifier {
            case "active_action":
                self.showBackgroundLocationNotification()
                self.notifyListeners("onlineNotificationAction", data: ["isOnline": self.isActive])
            default:
                print("Other Action")
            }

            completionHandler()
        }
}
