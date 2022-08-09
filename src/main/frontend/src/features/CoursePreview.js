// React imports
import React, { useState } from 'react';
// Redux imports
import { useDispatch } from 'react-redux';
// Store imports
import { changeSelectedCourse } from '../app/appSlice';
import { parseCourseCoverImage } from '../util/Utils.js';

import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Collapse  from 'react-bootstrap/Collapse';
import CourseTOC from './CourseTOC';
import ErrorAlert from './alerts/ErrorAlert';
import Header from './Header';
import Image from 'react-bootstrap/Image';
import InfoAlert from './alerts/InfoAlert';
import Row from 'react-bootstrap/Row';

function CoursePreview(props) {

  // In case there's an error adding the links, display an error message.
  const [errorAddingLinks, setErrorAddingLinks] = useState(false);
  const errorMessage = 'Oops, the Lumen content was not successfully added. Please try again or contact Lumen Support.';

  const dispatch = useDispatch();

  const resetSelectedCourse = () => {
    dispatch(changeSelectedCourse(null));
  }

  // Some courses may not have a valid cover image, use a default instead
  const courseCoverUrl = parseCourseCoverImage(props.course.cover_img_url);
  const addCourseToLMS = () => {
      const ltiLaunchData = JSON.parse(document.getElementById('root').getAttribute('lti-launch-data'));
      const idToken = ltiLaunchData.id_token;
      const target = ltiLaunchData.target;
      const state = ltiLaunchData.state;
      const contextAPIUrl = target.replace("/lti3", "/context");

      // TODO: Handle error cases of blank/null values

      const bookPairingData = {
        "id_token": idToken,
        "root_outcome_guid": props.course.root_outcome_guid
      }

      fetch(contextAPIUrl + "?state=" + state, {
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
      });
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
        <Row>
          <Col sm={1}>
            <Image rounded fluid src={courseCoverUrl} title={props.course.book_title} />
          </Col>
          <Col sm={11}>
            {(props.course.book_title || props.course.description) ?
              <Header header={props.course.book_title} subheader={props.course.description} />
            :
              <InfoAlert message="This course does not have a title nor description."/>
            }
          </Col>
        </Row>
        <CourseTOC topics={props.course.table_of_contents} />
      </div>
      <div className="fixed-bottom course-footer d-flex flex-row">
          <p className="action-info">Clicking Add Course will add all of the content for this Lumen course to your LMS</p>
          <div className="ms-auto mx-3 d-flex d-row">
            <Button variant="secondary" onClick={(e) => resetSelectedCourse()}>Cancel</Button>
            <Button variant="primary" className="ms-1" onClick={(e) => addCourseToLMS()}>Add Course</Button>
          </div>
      </div>
  </>
  );

}

export default CoursePreview;
