// React imports
import React, { useState } from 'react';

// Redux imports
import { useDispatch, useSelector } from 'react-redux';

// Store imports
import {
  changeSelectedCourse,
  toggleAllModules,
  selectIdToken,
  selectRootOutcomeGuid,
  selectSelectedModules,
  selectState,
  selectTarget,
  setIsFetchingDeepLinks,
  selectLtiStorageTarget
} from '../app/appSlice';

import { parseCourseCoverImage } from '../util/Utils.js';

// Component imports
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Collapse  from 'react-bootstrap/Collapse';
import CourseTOC from './CourseTOC';
import ErrorAlert from './alerts/ErrorAlert';
import Form from 'react-bootstrap/Form';
import Header from './Header';
import Image from 'react-bootstrap/Image';
import InfoAlert from './alerts/InfoAlert';
import LoadingPage from './LoadingPage';
import Row from 'react-bootstrap/Row';

function CoursePreview(props) {

  // These values are needed to add the courses to the LMS.
  const idToken = useSelector(selectIdToken);
  const target = useSelector(selectTarget);
  const state = useSelector(selectState);
  const rootOutcomeGuid = useSelector(selectRootOutcomeGuid);
  const selectedModules = useSelector(selectSelectedModules);
  const ltiStorageTarget = useSelector(selectLtiStorageTarget);

  // Check if the user has selected any module, the Add button must be disabled if there's no selection.
  const hasSelectedModules = Array.isArray(selectedModules) && selectedModules.length > 0;

  // A returning user means a user that previously associated a course with the LMS course, exists a previous root_outcome_guid selection.
  const isReturningUser = rootOutcomeGuid !== null;

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url, true);
  const courseTitle = props.course.book_title ? props.course.book_title : 'This course does not have a title.';
  const courseCoverTitle = `The cover image for the ${courseTitle} course.`;
  const dispatch = useDispatch();

  // In case there's an error adding the links, display an error message.
  const [errorAddingLinks, setErrorAddingLinks] = useState(false);
  const errorMessage = 'Oops, the Lumen content was not successfully added. Please try again or contact Lumen Support.';

  const [isFetchingDeepLinks, setFetchingDeepLinks] = useState(false);

  // The 'Select All' checkbox is controlled and depends on the state, enables or disables all the modules at the same time.
  // If the course has not been paired with a Lumen course, 'Select All' must be checked.
  const [selectAllChecked, setSelectAllChecked] = useState(!isReturningUser);
  // If the course has been paired with a Lumen course we must display a different text for the button.
  const addButtonText = !isReturningUser ? 'Add Course' : 'Add Content';
  const helpText = !isReturningUser ? 'Clicking Add Course will add the selected content for this Lumen course to your LMS' :
                   'Clicking Add Content will add the selected content for this Lumen course to your LMS';

  // When the user clicks cancel it should reset the module and the course selection.
  const resetSelection = () => {
    dispatch(changeSelectedCourse(null));
    dispatch(toggleAllModules(0));
  }

  // When the user checks or unchecks 'Select All' it must toggle all the modules.
  const handleSelectAll = () => {
    const newSelectAll = !selectAllChecked;
    setSelectAllChecked(newSelectAll);
    dispatch(toggleAllModules(newSelectAll));
  }

  const addCourseToLMS = () => {
      // The window will never notice if the user is browsing in long contents or not, should always scroll to top when fetching items.
      window.scrollTo(0, 0);

      // Display the spinner when fetching the deep links.
      setFetchingDeepLinks(true);
      // Notify other components that a deep linking request has been started.
      dispatch(setIsFetchingDeepLinks(true));

      const contextAPIUrl = target.replace("/lti3", "/context");

      // Append the selected moduleIds to the PUT request body.
      // Do not append moduleIds when all the modules have been selected.
      // When all the modules have been selected just send null as module_ids.
      let moduleIdList = null;
      if (Array.isArray(props.course.table_of_contents) && props.course.table_of_contents.length !== selectedModules.length) {
        // Filter the selected items by index, then get the module_ids.
        moduleIdList = props.course.table_of_contents
          .filter( (item, index) => selectedModules.includes(index))
          .map( (module) => module.module_id );
      }

      // TODO: Handle error cases of blank/null values
      const bookPairingData = {
        "id_token": idToken,
        "root_outcome_guid": props.course.root_outcome_guid,
        "module_ids": moduleIdList
      }

      fetch(contextAPIUrl + "?state=" + state + "&lti_storage_target=" + ltiStorageTarget, {
        method: 'PUT',
        headers: {
            'Content-Type':'application/json'
        },
        body: JSON.stringify(bookPairingData)
      }).then((response,reject)=> {
        if (response.ok) return response.json();
        return Promise.reject(`Http error status: ${response.status}, response: ${response.body}`)
      }).then (response => {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = response.deep_link_return_url;
        document.body.appendChild(form);
        const formField = document.createElement('input');
        formField.type = 'hidden';
        formField.name = 'JWT';
        formField.value = response.JWT;
        form.appendChild(formField);
        form.submit();
      }).catch(reason => {
        setErrorAddingLinks(true);
        // Remove the spinner once the request has responded with an error, otherwise the LMS will close the modal.
        setFetchingDeepLinks(false);
        // Notify other components that the request has been completed with an error.
        dispatch(setIsFetchingDeepLinks(false));
      });
  }

  if (isFetchingDeepLinks) {
    return <LoadingPage />;
  }

  return (
    <>
      <Collapse in={errorAddingLinks}>
        <div className="floating-alert">
          <ErrorAlert dismissible="dismissible" onClose={() => setErrorAddingLinks(false)} message={errorMessage}/>
        </div>
      </Collapse>
      <div className="header">
        <Row>
          <Col>
            <Header header="Preview Your Selected Course" subheader="A high-level review of course concepts and structure." />
          </Col>
        </Row>
      </div>
      <div className="course-info">
        <Row className="course-preview-info">
          <Col sm={2}>
            <Image rounded fluid src={courseCoverUrl} title={courseCoverTitle} />
          </Col>
          <Col sm={10}>
            {(props.course.book_title || props.course.description) ?
              <Header header={props.course.book_title} subheader={props.course.description} />
            :
              <InfoAlert message="This course does not have a title nor description."/>
            }
          </Col>
        </Row>
        <div className="select-all">
          <Form.Check type="checkbox" onChange={handleSelectAll} checked={selectAllChecked} id="toggle-all" label="Select All" />
        </div>
        <CourseTOC topics={props.course.table_of_contents} />
      </div>
      <div className="fixed-bottom course-footer d-flex flex-row">
          <p className="action-info">{helpText}</p>
          <div className="ms-auto mx-3 d-flex d-row">
            {!isReturningUser && <Button variant="secondary" onClick={(e) => resetSelection()}>Cancel</Button>}
            <Button disabled={!hasSelectedModules} variant="primary" className="ms-1" onClick={(e) => addCourseToLMS()}>{addButtonText}</Button>
          </div>
      </div>
  </>
  );

}

export default CoursePreview;
