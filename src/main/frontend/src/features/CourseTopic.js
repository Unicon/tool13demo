import InfoAlert from './alerts/InfoAlert';
import Row from 'react-bootstrap/Row';

function CourseTopic (props) {

  let courseSectionSubtopics = <InfoAlert message="This course topic does not have contents"/>;
  if (props.topic.sub_topics && props.topic.sub_topics.length > 0) {
    courseSectionSubtopics = (<ul>
          {props.topic.sub_topics.map((item, index) => {
            return <li key={index}>{item}</li>
          })}
        </ul>);
  }

  return (
    <Row className="mt-4">
      <div className="section-header">
        {props.topic.name ? props.topic.name : <InfoAlert message="This course topic does not have a name."/>}
      </div>
      <span className="section-header-separator"></span>
      <div>
          {courseSectionSubtopics}
      </div>
    </Row>
  );

}

export default CourseTopic;
