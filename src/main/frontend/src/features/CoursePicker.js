// Redux imports
import { useSelector } from 'react-redux';
import { selectCourseArray, selectLoading } from '../app/appSlice';

// Components import
import Col from 'react-bootstrap/Col';
import CourseGrid from './CourseGrid';
import Header from './Header';
import Row from 'react-bootstrap/Row';
import SearchInput from './controls/SearchInput';
import Spinner from 'react-bootstrap/Spinner';

function CoursePicker (props) {

  // useSelector gets the value present in the store, this value may change at a component level.
  const courseArray = useSelector(selectCourseArray);
  const isLoading = useSelector(selectLoading);

  // Display a spinner when courses are loading...
  const loadingCoursesText = 'Loading courses...';
  let courseGrid = (<>
                    <Spinner animation="border" role="status"><span className="visually-hidden">{loadingCoursesText}</span></Spinner>
                    <p className="text-dark">{loadingCoursesText}</p>
                   </>);

  if (!isLoading) {
    courseGrid = <CourseGrid courses={courseArray} />;
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
    </>
  );

}

export default CoursePicker;
