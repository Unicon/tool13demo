import courseImage from '../media/dna.webp'

import { useDispatch } from 'react-redux';
import { changeSelectedCourse } from '../app/appSlice';

import Card from 'react-bootstrap/Card';

function CourseCard (props) {

  const dispatch = useDispatch();

  const formatDate = (date) => {
    const dateFormatted = new Date(date);
    return dateFormatted.toLocaleString();
  }

  return (
    <Card onClick={(e) => dispatch(changeSelectedCourse(props.course))} className="course-card">
      <Card.Img variant="top" src={courseImage} className="course-image" title={props.course.book_title}/>
      <Card.Body>
        <Card.Title className="mb-2">{props.course.book_title ? props.course.book_title : 'This course does not have a title.'}</Card.Title>
        <Card.Subtitle className="mb-2 text-muted">Released: {props.course.release_date ? formatDate(props.course.release_date) : 'No release date has been provided.'}</Card.Subtitle>
      </Card.Body>
    </Card>
  );

}

export default CourseCard;
