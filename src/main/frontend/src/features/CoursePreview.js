// Redux imports
import { useDispatch } from 'react-redux';
// Store imports
import { changeSelectedCourse } from '../app/appSlice';
import { parseCourseCoverImage } from '../util/Utils.js';

import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import CourseTOC from './CourseTOC';
import Header from './Header';
import Image from 'react-bootstrap/Image';
import Row from 'react-bootstrap/Row';

function CoursePreview(props) {

  const dispatch = useDispatch();

  const resetSelectedCourse = () => {
    dispatch(changeSelectedCourse(null));
  }

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url);

  return (
    <>
      <div className="header">
        <Row>
          <Col>
            <Header header="Preview Your Selected Course" subheader="A high-level review of course concepts and structure" />
          </Col>
        </Row>
      </div>
      <div className="course-info">
        <Row>
          <Col sm={2}>
            <Image rounded fluid src={courseCoverUrl} title={props.course.book_title} />
          </Col>
          <Col sm={10}>
            {(props.course.book_title || props.course.description) ?
              <Header header={props.course.book_title} subheader={props.course.description} />
            :
              <Alert>This course does not have a title nor description.</Alert>
            }
          </Col>
        </Row>
        <div className="mt-1">
          <CourseTOC topics={props.course.table_of_contents} />
        </div>
      </div>
      <div className="fixed-bottom course-footer d-flex flex-row">
          <p className="text-secondary mb-0 mt-2 action-info">Clicking Add Course will add all of the content for this Lumen course to your LMS</p>
          <div className="ms-auto mx-3 d-flex d-row">
            <Button variant="secondary" onClick={(e) => resetSelectedCourse()}>Cancel</Button>
            <Button variant="primary" className="ms-1" onClick={(e) => alert(`That's All Folks!!`)}>Add Course</Button>
          </div>
      </div>
  </>
  );

}

export default CoursePreview;