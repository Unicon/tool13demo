// Redux imports
import { useSelector } from 'react-redux';
import { selectCourseArray, selectErrorFetchingCourses, selectLoading } from '../app/appSlice';

// Components import
import Alert from 'react-bootstrap/Alert';
import Col from 'react-bootstrap/Col';
import CourseGrid from './CourseGrid';
import CoursePaginator from './CoursePaginator';
import Header from './Header';
import Row from 'react-bootstrap/Row';
import SearchInput from './controls/SearchInput';
import Spinner from 'react-bootstrap/Spinner';

function CoursePicker (props) {

  // useSelector gets the value present in the store, this value may change at a component level.
  const courseArray = useSelector(selectCourseArray);
  const isLoading = useSelector(selectLoading);
  const isErrorFetchingCourses = useSelector(selectErrorFetchingCourses);

  // Display a spinner when courses are loading...
  const loadingCoursesText = 'Loading courses...';
  let courseGrid = (<>
                    <Spinner animation="border" role="status"><span className="visually-hidden">{loadingCoursesText}</span></Spinner>
                    <p className="text-dark">{loadingCoursesText}</p>
                   </>);

  if (!isLoading) {
    courseGrid = <CourseGrid courses={courseArray} />;
  }

  if (isErrorFetchingCourses) {
    courseGrid = <Alert variant="danger">There was an error fetching courses, please try again later.</Alert>;
  }

  return (
    <>
      <div className="header">
        <Row>
          <Col sm={8}>
            <Header header="Add a Course" subheader="Select the course you would like to add" />
          </Col>
          <Col sm={4}>
            <SearchInput />
          </Col>
        </Row>
      </div>
      {/* Display 3 CourseCards in small screens, 2 in extra small and only 1 in super small.*/}
      <Row sm={3} xs={2} xxs={1} className="g-3 course-grid">
        {courseGrid}
      </Row>
      <div className="paginator d-flex justify-content-center">
        <Row>
          <CoursePaginator />
        </Row>
      </div>
    </>
  );

}

export default CoursePicker;
