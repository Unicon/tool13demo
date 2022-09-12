// Redux imports
import { useSelector } from 'react-redux';

// Store imports
import {
  selectSelectedCourse
} from './app/appSlice';

// Components import
import Container from 'react-bootstrap/Container';
import CoursePreview from './features/CoursePreview';
import CoursePicker from './features/CoursePicker';
import LtiBreadcrumb from './features/LtiBreadcrumb';

// Stylesheet imports
import './App.css';
import "@fontsource/lato";
import "@fontsource/public-sans";

function App() {

  // The window will never notice if the user is browsing in long contents or not, should always scroll to top when navigating across courses.
  window.scrollTo(0, 0);

  const selectedCourse = useSelector(selectSelectedCourse);

  // Show the course picker by default
  let appContent = <CoursePicker />;

  // When a course has been selected display the course preview.
  if (selectedCourse) {
    appContent = <CoursePreview course={selectedCourse} />
  }

  return (
    <>
      <LtiBreadcrumb course={selectedCourse} />
      <Container className="App" fluid role="main">
        {appContent}
      </Container>
    </>
  );

}

export default App;
