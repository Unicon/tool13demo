import Alert from 'react-bootstrap/Alert';
import CourseTopic from './CourseTopic';

function CourseTOC (props) {

  // The table of contents object could be null, empty array or empty object.
  if (!props.topics || Object.keys(props.topics).length === 0 || props.topics.length === 0) {
    return <Alert>This course does not have topics.</Alert>;
  }

  const courseTableOfContents = props.topics.map((topic, index) => {
    return <CourseTopic key={index} topic={topic} />;
  });

  return courseTableOfContents;

}

export default CourseTOC;
