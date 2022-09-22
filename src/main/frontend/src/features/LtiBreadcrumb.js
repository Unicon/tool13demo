// Redux imports
import { useDispatch, useSelector } from 'react-redux';
// Store imports
import {
  changeSelectedCourse,
  selectSelectedCourse,
  selectRootOutcomeGuid,
  toggleAllModules
} from '../app/appSlice';
// Component imports
import Breadcrumb from 'react-bootstrap/Breadcrumb';

function LtiBreadcrumb(props) {

  const dispatch = useDispatch();
  const selectedCourse = useSelector(selectSelectedCourse);
  const rootOutcomeGuid = useSelector(selectRootOutcomeGuid);

  // A returning user means a user that previously associated a course with the LMS course, exists a previous root_outcome_guid selection.
  // When a course has been paired with the LMS, do not display the breadcrumb so the user cannot go back and select a different course.
  const isReturningUser = rootOutcomeGuid !== null;

  const resetSelection = () => {
    dispatch(changeSelectedCourse(null));
    dispatch(toggleAllModules(0));
  }

  return (
    <Breadcrumb role="navigation">
      {!isReturningUser &&
        <>
          <Breadcrumb.Item onClick={(e) => resetSelection()}>Lumen Learning</Breadcrumb.Item>
          <Breadcrumb.Item active={!selectedCourse} onClick={(e) => resetSelection()}>Add Course</Breadcrumb.Item>
          {selectedCourse && <Breadcrumb.Item active>{selectedCourse.book_title}</Breadcrumb.Item>}
        </>
      }
    </Breadcrumb>
  );

}

export default LtiBreadcrumb;
