import React, { useState } from 'react';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithEmailAndPassword,
} from 'firebase/auth';
import { FormGroup, Input, Button } from 'reactstrap';
import { useNavigate } from 'react-router-dom';
import AuthContainer from '../components/AuthContainer';

export interface ILoginPageProps {}

const LoginPage: React.FunctionComponent<ILoginPageProps> = props => {
  const auth = getAuth();
  const navigate = useNavigate();
  const [authing, setAuthing] = useState(false);
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [error, setError] = useState<string>('');

  // const history = useHistory();

  const signInWithEmailAndPass = async () => {
    if (error !== '') setError('');

    setAuthing(true);

    signInWithEmailAndPassword(auth, email, password)
      .then(response => {
        console.log(response.user.uid);
        navigate('/');
      })
      .catch(error => {
        console.log(error);
        setAuthing(false);
      });
  };

  const signInWithGoogle = async () => {
    setAuthing(true);

    signInWithPopup(auth, new GoogleAuthProvider())
      .then(response => {
        console.log(response.user.uid);
        navigate('/');
      })
      .catch(error => {
        console.log(error);
        setAuthing(false);
      });
  };

  return (
    <AuthContainer header="Login">
      <FormGroup>
        <Input
          type="email"
          name="email"
          id="email"
          placeholder="Email Address"
          onChange={event => setEmail(event.target.value)}
          value={email}
        />
      </FormGroup>
      <FormGroup>
        <Input
          autoComplete="new-password"
          type="password"
          name="password"
          id="password"
          placeholder="Enter Password"
          onChange={event => setPassword(event.target.value)}
          value={password}
        />
      </FormGroup>

      <Button onClick={() => signInWithEmailAndPass()} disabled={authing}>
        Login
      </Button>
    </AuthContainer>
  );
};

export default LoginPage;
