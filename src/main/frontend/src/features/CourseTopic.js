// Redux imports
import { useDispatch } from 'react-redux';
import { changeSelectedModules } from '../app/appSlice';

// Component imports
import InfoAlert from './alerts/InfoAlert';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';

function CourseTopic (props) {

  const dispatch = useDispatch();

  let courseSectionSubtopics = <InfoAlert message="This course topic does not have contents"/>;
  if (Array.isArray(props.topic.sub_topics) && props.topic.sub_topics.length) {
    courseSectionSubtopics = (<ul>
          {props.topic.sub_topics.map((item, index) => {
            return <li key={index} tabIndex="0">{item}</li>
          })}
        </ul>);
  }

  // This just builds the topic name using the prefix 'Module XX:' or displays information that the topic does not have a name.
  const topicName = props.topic.name;
  let moduleName = <><i className="fa fa-exclamation-circle error-icon" aria-hidden="true"></i><span className="fst-italic">This course topic does not have a name.</span></>;
  if (topicName) {
    moduleName = `Module ${props.index + 1}: ${topicName}`;
  }

  const toggleModule = (e) => {
    const moduleInfo = {
      moduleId: props.index,
      selected: e.target.checked
    }
    dispatch(changeSelectedModules(moduleInfo));
  }

  return (
    <Row className="mt-4">
      <div className="section-header">
        <Form.Check type="checkbox" onChange={(e) => toggleModule(e)} checked={props.selected} id={`module-${props.index}`} label={moduleName}/>
      </div>
      <span className="section-header-separator"></span>
      <div>
          {courseSectionSubtopics}
      </div>
    </Row>
  );

}

export default CourseTopic;
