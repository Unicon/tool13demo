import CourseTopic from './CourseTopic';
import InfoAlert from './alerts/InfoAlert';

function CourseTOC (props) {

  // The table of contents object could be null, empty array or empty object.
  if (!props.topics || Object.keys(props.topics).length === 0 || props.topics.length === 0) {
    return <InfoAlert message="This course does not have topics"/>;
  }

  const courseTableOfContents = props.topics.map((topic, index) => {
    return <CourseTopic key={index} topic={topic} />;
  });

  return courseTableOfContents;

}

export default CourseTOC;
