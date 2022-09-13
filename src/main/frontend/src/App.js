// Redux imports
import { useSelector } from 'react-redux';

// Store imports
import {
  selectSelectedCourse,
  selectLtiLineItemsSyncError
} from './app/appSlice';

// Components import
import Container from 'react-bootstrap/Container';
import CoursePreview from './features/CoursePreview';
import CoursePicker from './features/CoursePicker';
import ErrorAlert from './features/alerts/ErrorAlert';
import LtiBreadcrumb from './features/LtiBreadcrumb';

// Stylesheet imports
import './App.css';
import "@fontsource/lato";
import "@fontsource/public-sans";

function App() {

  // The window will never notice if the user is browsing in long contents or not, should always scroll to top when navigating across courses.
  window.scrollTo(0, 0);

  const selectedCourse = useSelector(selectSelectedCourse);
  const errorSyncingLineItems = useSelector(selectLtiLineItemsSyncError);

  // This error comes from the backend when there's an issue syncing line items, we must display an error when this happens. it's sent as string.
  if (errorSyncingLineItems === "true") {
    const syncingErrorMessage = `Oops, something went wrong and we couldn't load your content. Please try again. If the issue persists, please contact Lumen Support.`;
    return <Container className="App" fluid role="main">
             <div className="mt-3"><ErrorAlert message={syncingErrorMessage} /></div>
           </Container>;
  }

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
