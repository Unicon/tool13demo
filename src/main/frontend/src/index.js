import React from 'react';
// Redux imports
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import appSlice from './app/appSlice';
import { fetchCourses } from './app/appSlice';
import { isValidRootOutcomeGuid } from './util/Utils.js';

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
// This defines the initial state of the store, the variables sent from the backend should be in the store.
const initialState = {
  // These variables are related to the LTI APP.
  courseArray: [],
  filteredCourseArray: [],
  metadata: {},
  searchInputText: '',
  selectedCourse: null,
  loading: true,
  isFetchingDeepLinks: false,
  errorFetchingCourses: false,
  errorAssociatingCourse: false,
  selectedModules: [],
  // These variables are related to the LTI Launch data.
  deploymentId: ltiLaunchData.deploymentId,
  clientId: ltiLaunchData.clientId,
  iss: ltiLaunchData.iss,
  context: ltiLaunchData.context,
  id_token: ltiLaunchData.id_token,
  state: ltiLaunchData.state,
  target: ltiLaunchData.target,
  root_outcome_guid: isValidRootOutcomeGuid(ltiLaunchData.root_outcome_guid) ? ltiLaunchData.root_outcome_guid : null,
  platform_family_code: ltiLaunchData.platform_family_code,
  lti_system_error: ltiLaunchData.lti_system_error,
  ltiStorageTarget: ltiLaunchData.ltiStorageTarget,
};

// Creates the store and preloads the initial state of the store.
const store = configureStore({
  reducer: appSlice,
  preloadedState: initialState
});

// Once the store is configured, load the courses from the backend.
store.dispatch(fetchCourses(1));

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
