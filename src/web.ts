import { WebPlugin } from '@capacitor/core';

import type {
  BackgroundLocationPlugin,
  CallbackError,
  Location,
  WatcherOptions,
} from './definitions';

export class BackgroundLocationWeb
  extends WebPlugin
  implements BackgroundLocationPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async addWatcher(
    options: WatcherOptions,
    callback: (position?: Location, error?: CallbackError) => void,
  ): Promise<string> {
    return 'Not Implemented in Web' + options + ',' + callback.name;
  }

  async removeWatcher(options: { id: string }): Promise<void> {
    console.log(options);
    return;
  }

  async openSettings(): Promise<void> {
    return;
  }
}
