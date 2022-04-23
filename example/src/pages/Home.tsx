import React, { useState, useCallback } from 'react';
import { getAuth, signOut } from 'firebase/auth';
import { collection, doc, getFirestore, setDoc } from 'firebase/firestore';
import { Button } from 'reactstrap';
import './index.css';

import { BackgroundLocation, Location } from 'background-location-update';

export interface IHomePageProps {}

const HomePage: React.FunctionComponent<IHomePageProps> = props => {
  // const auth = getAuth();
  const db = getFirestore();

  // const [loc, setLoc] = useState();
  // const [BGDisabled, setBGDisabled] = useState();
  const [forgroundList, setForgroundList]: any = useState([]);
  const [forgroundID, setForgroundID]: any = useState();
  const [backgroundList, setBackgroundList]: any = useState([]);
  const [backgroundID, setBackgroundID]: any = useState();
  //   const guess = useCallback(async () => {
  //     let coordinates = null;
  //     BackgroundLocation.addWatcher(
  //       {
  //         requestPermissions: false,
  //         stale: true,
  //       },
  //       location => {
  //         if (location) {
  //             coordinates = location;
  //         //   setLoc(location);
  //         }
  //         setLoc(coordinates)
  //       },
  //     );
  //   }, []);

  const log_for_watcher = (text: string, time: number | null) => {
    console.log(text + ',' + time);
    // const li = document.createElement('li');
    // li.style.color = colour;
    // li.innerText = String(Math.floor((time - started) / 1000)) + ':' + text;
    // const container = document.getElementById('log');
    // return container.insertBefore(li, container.firstChild);
  };

  const make_guess = (timeout: number): Promise<Location | undefined> => {
    return new Promise(function (resolve) {
      let last_location: Location | undefined;
      let id: string;
      BackgroundLocation.addWatcher(
        {
          requestPermissions: true,
          stale: true,
        },
        async function callback(location) {
          last_location = location;
          if (location !== undefined) {
            try {
              await setDoc(
                doc(
                  db,
                  'locations_fg/',
                  Math.floor(Date.now() / 1000).toString(),
                ),
                location,
              );
            } catch (error) {
              console.error(error);
            }
          }
        },
      ).then(function retain_callback_id(the_id) {
        // const col = collection(db, "Locations")
        id = the_id;
      });

      setTimeout(function () {
        resolve(last_location);
        BackgroundLocation.removeWatcher({ id });
      }, timeout);
    });
  };

  const guess = async (timeout: number) => {
    return make_guess(timeout).then(function (location: Location | undefined) {
      return location === undefined
        ? log_for_watcher('null', Date.now())
        : log_for_watcher(
            [location.latitude, location.longitude].map(String).join(':'),
            location.time,
          );
    });
  };

  const log_error = (error: Error) => {
    console.error(error);
    return log_for_watcher(error.name + ': ' + error.message, Date.now());
  };

  const log_location = (location: Location, watcher_ID: string) => {
    return log_for_watcher(
      location.latitude + ':' + location.longitude,
      location.time,
    );
  };
  const watcher_callback = async (location: any, error: any, type: boolean) => {
    console.log('inside watcher callback');
    if (error) {
      console.log('inside watcher callback error');
      if (
        error.code === 'NOT_AUTHORIZED' &&
        window.confirm(
          'This app needs your location, ' +
            'but does not have permission.\n\n' +
            'Open settings now?',
        )
      ) {
        BackgroundLocation.openSettings();
      }
      return log_error(error);
    }
    if (location !== undefined) {
      if (type) {
        // let newList = backgroundList;
        // newList.push(location);
        setBackgroundList((state: any) => [...state, location]);

        await setDoc(
          doc(db, 'locations_bg/', Math.floor(Date.now() / 1000).toString()),
          location,
        );
      } else {
        // let newList = forgroundList;
        // newList.push(location);
        setForgroundList((state: any) => [...state, location]);

        await setDoc(
          doc(db, 'locations_fg/', Math.floor(Date.now() / 1000).toString()),
          location,
        );
      }
      return location;
    } else {
      console.log('inside watcher callback location undefined');
    }
  };
  const add_watcher = async (type: boolean) => {
    const res = await BackgroundLocation.addWatcher(
      Object.assign(
        {
          stale: true,
        },
        type
          ? {
              backgroundTitle: 'Tracking your location, senõr.',
              backgroundMessage: 'Cancel to prevent battery drain.',
            }
          : {
              distanceFilter: 10,
            },
      ),
      (location, error) => watcher_callback(location, error, type),
    );
    // .then(the_id => {

    console.log(type);
    if (type) {
      console.log('inside res:', res);
      setBackgroundID(res);
      console.log('inside: ', backgroundID);
    } else {
      setForgroundID(res);
    }
  };
  const remove_watcher = async (watcherId: string, type: boolean) => {
    console.log('watcherId: ' + watcherId);
    let response = await BackgroundLocation.removeWatcher({ id: watcherId });
    console.log('response remove watcher: ' + response);
    if (type) {
      setBackgroundID('');
      setBackgroundList([]);
    } else {
      setForgroundID('');
      setForgroundList([]);
    }
  };

  const returnNull = () => {
    return null;
  };
  return (
    <div>
      {/* <p>Home Page (Protected by Firebase!)</p> */}
      <h1>Geolocation</h1>
      <p>Your location is:</p>
      <Button onClick={() => guess(100)}>Guess</Button>
      <Button
        onClick={() => {
          backgroundList.length > 0 ? returnNull() : add_watcher(true);
        }}
        diabled={backgroundList.length > 0 ? true : false}
      >
        BG
      </Button>
      <Button
        onClick={() => {
          forgroundList.length > 0 ? returnNull() : add_watcher(false);
        }}
        diabled={forgroundList.length > 0 ? true : false}
      >
        FG
      </Button>
      <div className="flex-container">
        {backgroundList.length > 0 ? (
          <div>
            <Button onClick={() => remove_watcher(backgroundID, true)}>
              Remove BG
            </Button>
            <ul>
              {backgroundList.map((itm: any) => {
                return (
                  <li style={{ color: 'red' }}>
                    {itm.latitude} , {itm.longitude}
                  </li>
                );
              })}
            </ul>
          </div>
        ) : (
          ''
        )}
        {forgroundList.length > 0 ? (
          <div>
            <Button onClick={() => remove_watcher(forgroundID, false)}>
              Remove FG
            </Button>
            <ul>
              {forgroundList.map((itm: any) => {
                return (
                  <li style={{ color: 'blue' }}>
                    {itm.latitude} , {itm.longitude}
                  </li>
                );
              })}
            </ul>
          </div>
        ) : (
          ''
        )}
      </div>
    </div>
  );
};

export default HomePage;
