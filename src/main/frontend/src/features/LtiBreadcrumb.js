// Redux imports
import { useDispatch, useSelector } from 'react-redux';
// Store imports
import { changeSelectedCourse, selectSelectedCourse } from '../app/appSlice';
// Component imports
import Breadcrumb from 'react-bootstrap/Breadcrumb';

function LtiBreadcrumb(props) {

  const dispatch = useDispatch();
  const selectedCourse = useSelector(selectSelectedCourse);

  const resetSelectedCourse = () => {
    dispatch(changeSelectedCourse(null));
  }

  return (
    <Breadcrumb role="navigation">
      <Breadcrumb.Item onClick={(e) => resetSelectedCourse()}>Waymaker</Breadcrumb.Item>
      <Breadcrumb.Item active={!selectedCourse} onClick={(e) => resetSelectedCourse()}>Add Course</Breadcrumb.Item>
      {selectedCourse && <Breadcrumb.Item active>{selectedCourse.book_title}</Breadcrumb.Item>}
    </Breadcrumb>
  );

}

export default LtiBreadcrumb;
