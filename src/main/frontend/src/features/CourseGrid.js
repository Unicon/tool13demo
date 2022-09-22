// Redux imports
import { useSelector } from 'react-redux';
import { selectCourseArray } from '../app/appSlice';

// Component imports
import Col from 'react-bootstrap/Col';
import CourseCard from './CourseCard';
import InfoAlert from './alerts/InfoAlert';
import Row from 'react-bootstrap/Row';

function CourseGrid(props) {

  // useSelector gets the value present in the store, this value may change at a component level.
  const courseArray = useSelector(selectCourseArray);

  // Display an info alert when there are no courses.
  if (!Array.isArray(courseArray) || !courseArray.length) {
    return <div className="header">
             <InfoAlert message="There are no courses to show"/>
           </div>;
  }

  // Provide one CourseCard per course found in the request.
  const content = courseArray.map ( (course, index)  => {
            return (<Col key={course.root_outcome_guid}>
                      <CourseCard course={course}/>
                    </Col>)
            });

  // Display 3 CourseCards in small screens, 2 in extra small and only 1 in super small.
  return  <Row sm={3} xs={2} xxs={1} className="g-5 course-grid">
            {content}
          </Row>;

}

export default CourseGrid;
