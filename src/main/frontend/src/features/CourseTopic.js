import Row from 'react-bootstrap/Row';
import Alert from 'react-bootstrap/Alert';

function CourseTopic (props) {

  let courseSectionSubtopics = <Alert>This course topic does not have contents.</Alert>;
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
        <h2>{props.topic.name ? props.topic.name : <Alert>This course topic does not have a name.</Alert>}</h2>
      </div>
      <div>
          {courseSectionSubtopics}
      </div>
    </Row>
  );

}

export default CourseTopic;
