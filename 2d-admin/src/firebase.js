import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getDatabase } from 'firebase/database';

const firebaseConfig = {
  apiKey: "AIzaSyCkI--YT7t8nRA3A_24CpZczSYC3uCgXCg",
  authDomain: "dledger-1e687.firebaseapp.com",
  databaseURL: "https://dledger-1e687-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "dledger-1e687",
  storageBucket: "dledger-1e687.firebasestorage.app",
  messagingSenderId: "1096955089627",
  appId: "1:1096955089627:android:2a8f6410386d470296f2fe"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getDatabase(app);
