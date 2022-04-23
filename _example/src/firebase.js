// Import the functions you need from the SDKs you need
import { initializeApp } from 'firebase/app';
import { getAuth, onAuthStateChanged } from 'firebase/auth';

const firebaseConfig = {
  apiKey: 'AIzaSyBBCFZGcYIE_crg4gFEo8IttTzV6n9sUU8',
  authDomain: 'fbapp-c9bdd.firebaseapp.com',
  databaseURL: 'https://fbapp-c9bdd.firebaseio.com',
  projectId: 'fbapp-c9bdd',
  storageBucket: 'fbapp-c9bdd.appspot.com',
  messagingSenderId: '991919161758',
  appId: '1:991919161758:web:b3c008543ae84b406f568f',
};

// // Initialize Firebase
const firebaseApp = initializeApp(firebaseConfig);

const auth = getAuth(firebaseApp);

onAuthStateChanged(auth, user => {
  if (user != null) {
    console.log(user);
  } else {
    console.log('no user found');
  }
});
