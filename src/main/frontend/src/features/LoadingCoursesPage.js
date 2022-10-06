// Component imports
import Container from 'react-bootstrap/Container';
import Spinner from 'react-bootstrap/Spinner';

function LoadingCoursesPage (props) {

  // Display a Spinner when courses are being loaded.
  const loadingText = 'Loading content, please wait...';
  const loadingComponent = <div className="loading-courses d-flex justify-content-center align-items-center m-4">
                             <Spinner animation="border" role="status">
                               <span className="visually-hidden">{loadingText}</span>
                             </Spinner>
                             {loadingText}
                           </div>;

  return <Container className="App" fluid role="main">
           {loadingComponent}
         </Container>;

}

export default LoadingCoursesPage;
