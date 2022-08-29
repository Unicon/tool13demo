// Redux imports
import { useSelector } from 'react-redux';
import { selectErrorAssociatingCourse, selectErrorFetchingCourses } from '../app/appSlice';

// Components import
import Col from 'react-bootstrap/Col';
import CourseGrid from './CourseGrid';
import CoursePaginator from './CoursePaginator';
import ErrorAlert from './alerts/ErrorAlert';
import Header from './Header';
import Row from 'react-bootstrap/Row';
import SearchInput from './controls/SearchInput';

function CoursePicker (props) {

  // useSelector gets the value present in the store, this value may change at a component level.
  const isErrorFetchingCourses = useSelector(selectErrorFetchingCourses);
  const isErrorAssociatingCourse = useSelector(selectErrorAssociatingCourse);

  // Display an error message when the courses cannot be fetched from Harmony.
  if (isErrorFetchingCourses) {
    return <div className="header">
             <Row><ErrorAlert message="Oops, we couldn't load the Lumen content. Please try again." /></Row>
           </div>;
  }

  // Display an error message when the LMS course has been paired with a non existing Lumen course.
  if (isErrorAssociatingCourse) {
    return <div className="header">
             <Row><ErrorAlert message="Oops, something went wrong. We could not add the correct Lumen content links for your course. Please contact Lumen Support." /></Row>
           </div>;
  }

  return (
    <>
      <div className="header">
        <Row>
          <Col sm={8}>
            <Header header="Add a Course" subheader="Select the course you would like to add." />
          </Col>
          <Col sm={4}>
            <SearchInput />
          </Col>
        </Row>
      </div>
      <>
        <CourseGrid />
        <div className="paginator d-flex justify-content-center">
          <Row>
            <CoursePaginator />
          </Row>
        </div>
      </>
    </>
  );

}

export default CoursePicker;
