import React, { useEffect, useState } from 'react';
import {
  getAuth,
  onAuthStateChanged,
  initializeAuth,
  indexedDBLocalPersistence,
} from 'firebase/auth';
import { app } from '../config/config';
import { useNavigate } from 'react-router-dom';

export interface IAuthRouteProps {
  children: React.ReactNode;
}

const AuthRoute: React.FunctionComponent<IAuthRouteProps> = props => {
  const { children } = props;
  // const auth = getAuth();
  const auth = initializeAuth(app, {
    persistence: indexedDBLocalPersistence,
  });
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  console.log('auth', auth);

  useEffect(() => {
    console.log('inside useEffect');
    const AuthCheck = onAuthStateChanged(auth, user => {
      console.log('inside on state changed');
      if (user) {
        setLoading(false);
      } else {
        console.log('unauthorized');
        navigate('/login');
      }
    });

    return () => AuthCheck();
  }, [auth]);

  if (loading) return <p>loading ...</p>;

  return <>{children}</>;
};

export default AuthRoute;
