// Redux imports
import { useSelector } from 'react-redux';

// Store imports
import {
  selectSelectedCourse,
  selectLoading,
  selectLtiSystemError
} from './app/appSlice';

// Components import
import Container from 'react-bootstrap/Container';
import CoursePreview from './features/CoursePreview';
import CoursePicker from './features/CoursePicker';
import ErrorAlert from './features/alerts/ErrorAlert';
import LoadingCoursesPage from './features/LoadingCoursesPage';
import LtiBreadcrumb from './features/LtiBreadcrumb';

// Stylesheet imports
import './App.css';
import "@fontsource/lato";
import "@fontsource/public-sans";

// Utils
import { LTI_SYSTEM_ERRORS } from './util/LtiSystemErrors';

function App() {

  // The window will never notice if the user is browsing in long contents or not, should always scroll to top when navigating across courses.
  window.scrollTo(0, 0);

  const selectedCourse = useSelector(selectSelectedCourse);
  const ltiSystemErrorCode = useSelector(selectLtiSystemError);
  const isLoading = useSelector(selectLoading);

  // When there's a system error from the backend we must inform the user and do not render any content.
  if (Number.isInteger(parseInt(ltiSystemErrorCode))) {
    let systemErrorMessage = null;
    switch (parseInt(ltiSystemErrorCode)) {
      case LTI_SYSTEM_ERRORS.DYNAMIC_REGISTRATION_GENERAL_ERROR:
        systemErrorMessage = `Oops something went wrong with the Lumen Dynamic Registration. Please contact Lumen Support.`;
        break;
      case LTI_SYSTEM_ERRORS.DYNAMIC_REGISTRATION_DUPLICATE_ERROR:
        systemErrorMessage = `Oops something went wrong. It appears you already have a tool registered with this Lumen domain.`;
        break;
      case LTI_SYSTEM_ERRORS.LINEITEMS_SYNCING_ERROR:
        systemErrorMessage = `Oops, something went wrong and we couldn't load your content. Please try again. If the issue persists, please contact Lumen Support.`;
        break;
      default:
        systemErrorMessage = `Unrecognized error message. Please try again. If the issue persists, please contact Lumen Support.`;
        break;
    }
    return <Container className="App" fluid role="main">
             <div className="mt-3"><ErrorAlert message={systemErrorMessage} /></div>
           </Container>;
  }

  // When courses are being loaded display a loading page.
  if (isLoading) {
    return <LoadingCoursesPage />;
  }

  // When a course has been selected display the course preview.
  return <>
           <LtiBreadcrumb course={selectedCourse} />
           <Container className="App" fluid role="main">
             {selectedCourse ? <CoursePreview course={selectedCourse} /> : <CoursePicker />}
           </Container>
         </>;

}

export default App;
