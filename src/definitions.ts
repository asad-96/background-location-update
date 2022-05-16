import type { PluginListenerHandle } from '@capacitor/core';

export interface BackgroundLocationPlugin {
  addWatcher(
    options: WatcherOptions,
    callback: (position?: Location, error?: CallbackError) => void,
  ): Promise<string>;
  removeWatcher(options: { id: string }): Promise<void>;
  openSettings(): Promise<void>;

  /**
   * Called when onlineNotificationAction set to true in addWatcher() and result received
   *
   * Provides onlineNotificationAction result.
   *
   * @since 2.0.2
   */
  addListener(
    eventName: 'onlineNotificationAction',
    listenerFunc: (data: { isOnline: boolean }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}

export interface WatcherOptions {
  backgroundMessage?: string;
  backgroundTitle?: string;
  requestPermissions?: boolean;
  stale?: boolean;
  distanceFilter?: number;
  onlineNotificationAction?: boolean;
}

export interface Location {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  altitudeAccuracy: number | null;
  simulated: boolean;
  bearing: number | null;
  speed: number | null;
  time: number | null;
}

export interface CallbackError extends Error {
  code?: string;
}
