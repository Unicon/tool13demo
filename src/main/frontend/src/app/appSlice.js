import { createSlice } from '@reduxjs/toolkit';
import { DUMMY_DATA } from '../util/dummyData';

// This defines the store slice
export const appSlice = createSlice({
  // Name of the slice
  name: 'appSlice',
  // A reducer is a function that takes the state and the action and defines the next state
  // In the example defines what happens when the category selection changes.
  reducers: {
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    setCourseArray: (state, action) => {
      state.courseArray = state.filteredCourseArray = action.payload;
    },
    updateMetadata: (state, action) => {
      state.metadata = action.payload;
    },
    changeSearchInput: (state, action) => {
      const searchInputText = action.payload;
      state.searchInputText = searchInputText;
      // This filters courses depending on the text input value.
      state.filteredCourseArray = state.courseArray.slice().filter((course) => {
        return searchInputText === '' || (course.book_title && course.book_title.toLowerCase().includes(searchInputText.toLowerCase()))
      });
    },
    changeSelectedCourse: (state, action) => {
      state.selectedCourse = action.payload;
      if (action.payload === null) {
        // This is important, when the course selection is being restored in the UI we must start over.
        state.root_outcome_guid = null;
      }
    },
    setErrorFetchingCourses: (state, action) => {
      state.errorFetchingCourses = action.payload;
    },
    setErrorAssociatingCourse: (state, action) => {
      state.errorAssociatingCourse = action.payload;
    },
    changeSelectedModules: (state, action) => {
      // When a user clicks a module, it updates the module selection array.
      const currentSelectedModules = state.selectedModules.slice();
      // The module that the user has clicked.
      const selectedModuleId = action.payload.moduleId;
      // If the user selected or deselected the module.
      const selected = action.payload.selected;
      // Push the selected or deselected module to the module selection, it will be filtered or not.
      currentSelectedModules.push(selectedModuleId);
      // Filter all the modules that are not selected
      state.selectedModules = currentSelectedModules.filter((moduleId) => {
        return (moduleId !== selectedModuleId) || (moduleId === selectedModuleId && selected);
      });
    },
    toggleAllModules: (state, action) => {
      // Clears the module selection or adds all the modules to the array.
      if (!action.payload) {
        state.selectedModules = [];
      } else if (state.selectedCourse) {
        state.selectedModules = Array.from(Array(state.selectedCourse.table_of_contents.length).keys());
      }
    },
  },
});

// Defines the actions that can be dispatched using dispatch and defines what happens with the state.
export const {
  changeSearchInput,
  changeSelectedCourse,
  changeSelectedModules,
  setErrorAssociatingCourse,
  setErrorFetchingCourses,
  setCourseArray,
  setLoading,
  toggleAllModules,
  updateMetadata
} = appSlice.actions;

// Connects variables to the state, when you want the state values in a component use this.
export const selectSearchInputText = (state) => state.searchInputText;
export const selectSelectedCourse = (state) => state.selectedCourse;
export const selectCourseArray = (state) => state.filteredCourseArray;
export const selectMetadata = (state) => state.metadata;
export const selectLoading = (state) => state.loading;
export const selectErrorFetchingCourses = (state) => state.errorFetchingCourses;
export const selectErrorAssociatingCourse = (state) => state.errorAssociatingCourse;
export const selectSelectedModules = (state) => state.selectedModules;
// These state selectors are related to the LTI Launch Data.
export const selectIss = (state) => state.iss;
export const selectContext = (state) => state.context;
export const selectClientId = (state) => state.clientId;
export const selectDeploymentId = (state) => state.deploymentId;
export const selectIdToken = (state) => state.id_token;
export const selectState = (state) => state.state;
export const selectTarget = (state) => state.target;
export const selectRootOutcomeGuid = (state) => state.root_outcome_guid;

// This function fetches the courses from the backend, it should be invoked when loading the application.
export const fetchCourses = (page) => (dispatch, getState) => {
  // Get the id_token from the state, thunks already have access to getState as the second argument.
  const idToken = getState().id_token;
  // Get the previous selection, if this has a value is because it has been sent from the backend, exists an association.
  const root_outcome_guid = getState().root_outcome_guid;
  // If no specific page is requested, request the first one.
  const requestedPage = page ? page : 1;
  // We must display an spinner when loading courses from the backend
  dispatch(setLoading(true));
  dispatch(setErrorFetchingCourses(false));
  fetch(`/harmony/courses?page=${requestedPage}`, {
    method: 'GET',
    headers: {'lti-id-token': idToken}
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`Problem fetching courses (status: ${response.status})`);
    }
    return response.json();
  }).then(json => {
    // Load the first batch of courses from the backend when the request has been resolved.
    dispatch(setCourseArray(json.records));
    // Load the pagination information for the first page.
    dispatch(updateMetadata(json.metadata));
  }).catch(reason => {
    // When there's an error fetching courses we must notify the components.
    dispatch(setErrorFetchingCourses(true));
    console.error(reason);
    // This is useful for local development purposes, load some dummy data instead of the error message.
    if (window.location.href.includes('localhost')) {
      dispatch(setCourseArray(DUMMY_DATA.records));
      dispatch(updateMetadata(DUMMY_DATA.metadata));
      dispatch(setErrorFetchingCourses(false));
    }
  }).finally(() => {
    // If a previous course selection has been made we must load the course from the backend.
    if (root_outcome_guid) {
      dispatch(fetchSingleCourse(root_outcome_guid));
    } else {
      // Remove the spinner once the request has been resolved.
      dispatch(setLoading(false));
    }
  });
};

// This function fetches a single course from the backend, it should be invoked when a course has been paired with the LMS course.
export const fetchSingleCourse = (rootOutcomeGuid) => (dispatch, getState) => {
  // Get the id_token from the state, thunks already have access to getState as the second argument.
  const idToken = getState().id_token;
  // We must display an spinner when loading courses from the backend
  dispatch(setLoading(true));
  dispatch(setErrorAssociatingCourse(false));
  fetch(`/harmony/courses?root_outcome_guid=${rootOutcomeGuid}`, {
    method: 'GET',
    headers: {'lti-id-token': idToken}
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`Problem fetching a single course (status: ${response.status})`);
    }
    return response.json();
  }).then(json => {
    if (Array.isArray(json.records) && json.records.length > 0) {
      dispatch(changeSelectedCourse(json.records[0]));
    } else {
      dispatch(setErrorAssociatingCourse(true));
    }
  }).catch(reason => {
    // When there's an error fetching courses we must notify the components.
    dispatch(setErrorAssociatingCourse(true));
    console.error(reason);
  }).finally(() => {
    // Remove the spinner once the request has been resolved.
    dispatch(setLoading(false));
  });
};

export default appSlice.reducer;
