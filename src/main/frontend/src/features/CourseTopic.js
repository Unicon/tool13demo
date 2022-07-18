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
    <Row>
      <div className="section-header">
        <h2>{props.topic.name ? props.topic.name : <InfoAlert message="This course topic does not have a name."/>}</h2>
      </div>
      <div>
          {courseSectionSubtopics}
      </div>
    </Row>
  );

}

export default CourseTopic;
