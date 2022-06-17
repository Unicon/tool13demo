import CourseCard from './CourseCard';
import Col from 'react-bootstrap/Col';
import Alert from 'react-bootstrap/Alert';

function CourseGrid(props) {

  let itemArray = [];
  if (props.courses) {
    itemArray = props.courses.slice();
  }

  // Display an alert when there's no courses.
  if (!itemArray.length) {
    return <Alert>There are no courses to show.</Alert>;
  } else {
    // Provide one CourseCard per course found in the request.
    const content = props.courses.map ( (course, index)  => {
              return (<Col key={course.root_outcome_guid}>
                        <CourseCard course={course}/>
                      </Col>)
              });
    return content;

  }
}

export default CourseGrid;
