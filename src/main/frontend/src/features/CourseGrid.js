import Alert from 'react-bootstrap/Alert';
import Col from 'react-bootstrap/Col';
import CourseCard from './CourseCard';

function CourseGrid(props) {

  let courseArray = [];
  if (props.courses) {
    courseArray = props.courses.slice();
  }
  // Display an alert when there are no courses.
  if (!courseArray.length) {
    return <Alert>There are no courses to show.</Alert>;
  } else {
    // Provide one CourseCard per course found in the request.
    const content = courseArray.map ( (course, index)  => {
              return (<Col key={course.root_outcome_guid}>
                        <CourseCard course={course}/>
                      </Col>)
              });
    return content;

  }
}

export default CourseGrid;
