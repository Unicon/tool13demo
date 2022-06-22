import { createSlice } from '@reduxjs/toolkit';
import { COURSE_ARRAY } from './staticData.js';

// This defines the initial state of the store
const initialState = {
  searchInputText: '',
  selectedCategoriesArray: [],
  selectedCourse: null,
  courseArray: COURSE_ARRAY
};

// This defines the store slice
export const appSlice = createSlice({
  // Name of the slice
  name: 'appSlice',
  // Initial state
  initialState,
  // A reducer is a function that takes the state and the action and defines the next state
  // In the example defines what happens when the category selection changes.
  reducers: {
    changeSearchInput: (state, action) => {
      debugger;
      const searchInputText = action.payload;
      state.searchInputText = searchInputText;
      // This filters courses depending on the text input value.
      state.courseArray = COURSE_ARRAY.slice().filter((course) => {
        return searchInputText === '' || (course.book_title && course.book_title.toLowerCase().includes(searchInputText.toLowerCase()))
      });
    },
    changeCategoryInput: (state, action) => {
      const isChecked = action.payload.checked;
      const value = action.payload.value;
      let newSelectedCategoriesArray = state.selectedCategoriesArray.slice();
      if (isChecked) {
        newSelectedCategoriesArray.push(value);
      } else {
        newSelectedCategoriesArray = state.selectedCategoriesArray.slice().filter(item => item !== value);
      }
      state.selectedCategoriesArray = newSelectedCategoriesArray;
    },
    changeSelectedCourse: (state, action) => {
      state.selectedCourse = action.payload;
    },
  },
});

// Defines the actions that can be dispatched using dispatch and defines what happens with the state.
export const { changeSearchInput, changeCategoryInput, changeSelectedCourse } = appSlice.actions;
// Connects variables to the state, when you want the state values in a component use this.
export const selectSearchInputText = (state) => state.searchInputText;
export const selectSelectedCourse = (state) => state.selectedCourse;
export const selectSelectedCategoriesArray = (state) => state.selectedCategoriesArray;
export const selectCourseArray = (state) => state.courseArray;

export default appSlice.reducer;
