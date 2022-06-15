import React from 'react';
// Redux imports
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
// Store imports
import { store } from './app/store';
// Components import
import ThemeProvider from 'react-bootstrap/ThemeProvider'
import App from './App';
// Stylesheet imports
import 'bootstrap/dist/css/bootstrap.min.css';
import 'font-awesome/css/font-awesome.min.css';
// Additional imports
import reportWebVitals from './reportWebVitals';


// Gets the element root from the DOM.
const roolElement = document.getElementById('root');
const root = createRoot(roolElement);
// Parse the LtiLaunchData JSON object from the root attribute.
const ltiLaunchData = JSON.parse(roolElement.getAttribute('lti-launch-data'));
// Use the backend attributes, it's recommended to store them in the Redux store.
const idToken = ltiLaunchData.id_token;
const target = ltiLaunchData.target;
// Just for debugging purposes.
console.log(`The idToken is ${idToken}`);
console.log(`The target is ${target}`);

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <ThemeProvider breakpoints={['xs', 'xxs','sm']}>
        <App />
      </ThemeProvider>
    </Provider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
