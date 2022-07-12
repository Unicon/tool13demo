// Redux imports
import { useDispatch, useSelector } from 'react-redux';
// Store imports
import { changeSelectedCourse, selectSelectedCourse, toggleAllModules } from '../app/appSlice';
// Component imports
import Breadcrumb from 'react-bootstrap/Breadcrumb';

function LtiBreadcrumb(props) {

  const dispatch = useDispatch();
  const selectedCourse = useSelector(selectSelectedCourse);

  const resetSelection = () => {
    dispatch(changeSelectedCourse(null));
    dispatch(toggleAllModules(0));
  }

  return (
    <Breadcrumb role="navigation">
      <Breadcrumb.Item onClick={(e) => resetSelection()}>Lumen Learning</Breadcrumb.Item>
      <Breadcrumb.Item active={!selectedCourse} onClick={(e) => resetSelection()}>Add Course</Breadcrumb.Item>
      {selectedCourse && <Breadcrumb.Item active>{selectedCourse.book_title}</Breadcrumb.Item>}
    </Breadcrumb>
  );

}

export default LtiBreadcrumb;
