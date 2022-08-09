// Redux imports
import { useSelector } from 'react-redux';
// Store imports
import { selectSelectedCourse } from './app/appSlice';
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

  const selectedCourse = useSelector(selectSelectedCourse);

  return (
    <>
      <LtiBreadcrumb course={selectedCourse} />
      <Container className="App" fluid role="main">
        {/* When a course is selected we must display the course info, otherwise the course picker.*/}
        {selectedCourse ? <CoursePreview course={selectedCourse} /> : <CoursePicker />}
      </Container>
    </>
  );

}

export default App;
