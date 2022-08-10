import { useDispatch } from 'react-redux';
import { changeSelectedCourse } from '../app/appSlice';
import { formatDate, parseCourseCoverImage } from '../util/Utils.js';

import Card from 'react-bootstrap/Card';

function CourseCard (props) {

  const dispatch = useDispatch();

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url);

  // If the pressed key is the enter we have to select the course.
  const checkPressedKey = (e) => {
    if (e.key === 'Enter') {
      dispatch(changeSelectedCourse(props.course));
    }
  }

  return (
    <Card className="course-card"
      tabindex="0"
      onKeyPress={(e) => checkPressedKey(e)}
      onClick={(e) => dispatch(changeSelectedCourse(props.course))} >
        <Card.Img variant="top" src={courseCoverUrl} className="course-card-image" title={props.course.book_title}/>
        <Card.Body>
          <Card.Title className="course-card-title">{props.course.book_title ? props.course.book_title : 'This course does not have a title.'}</Card.Title>
          <Card.Subtitle className="course-card-subtitle">Released: {props.course.release_date ? formatDate(props.course.release_date) : 'No release date has been provided.'}</Card.Subtitle>
        </Card.Body>
    </Card>
  );

}

export default CourseCard;
