import { useDispatch, useSelector } from 'react-redux';
import { changeSelectedCourse, selectRootOutcomeGuid, toggleAllModules } from '../app/appSlice';
import { formatDate, parseCourseCoverImage } from '../util/Utils.js';

import Card from 'react-bootstrap/Card';

function CourseCard (props) {

  const dispatch = useDispatch();
  const rootOutcomeGuid = useSelector(selectRootOutcomeGuid);

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url);
  const courseTitle = props.course.book_title ? props.course.book_title : 'This course does not have a title.';
  const courseCoverTitle = `The cover image for the ${courseTitle} course.`;

  const changeCourseSelection = () => {
    dispatch(changeSelectedCourse(props.course));
    // When a new Lumen course has been selected and the course is not paired with the LMS, we must check all the modules.
    dispatch(toggleAllModules(rootOutcomeGuid === null));
  }

  // If the pressed key is the enter we have to select the course.
  const checkPressedKey = (e) => {
    if (e.key === 'Enter') {
      changeCourseSelection();
    }
  }

  return (
    <Card className="course-card"
      tabIndex="0"
      onKeyPress={(e) => checkPressedKey(e)}
      onClick={(e) => changeCourseSelection()} >
        <Card.Img variant="top" src={courseCoverUrl} className="course-card-image" title={courseCoverTitle} />
        <Card.Body>
          <Card.Title className="course-card-title">{courseTitle}</Card.Title>
          <Card.Subtitle className="course-card-subtitle">Released: {props.course.release_date ? formatDate(props.course.release_date) : 'No release date has been provided.'}</Card.Subtitle>
        </Card.Body>
    </Card>
  );

}

export default CourseCard;
