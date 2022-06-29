import { useDispatch } from 'react-redux';
import { changeSelectedCourse } from '../app/appSlice';
import { formatDate, parseCourseCoverImage } from '../util/Utils.js';

import Card from 'react-bootstrap/Card';

function CourseCard (props) {

  const dispatch = useDispatch();

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url);

  return (
    <Card onClick={(e) => dispatch(changeSelectedCourse(props.course))} className="course-card">
      <Card.Img variant="top" src={courseCoverUrl} className="course-image" title={props.course.book_title}/>
      <Card.Body>
        <Card.Title className="mb-2">{props.course.book_title ? props.course.book_title : 'This course does not have a title.'}</Card.Title>
        <Card.Subtitle className="mb-2 text-muted">Released: {props.course.release_date ? formatDate(props.course.release_date) : 'No release date has been provided.'}</Card.Subtitle>
      </Card.Body>
    </Card>
  );

}

export default CourseCard;
