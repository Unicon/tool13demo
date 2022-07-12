// Redux imports
import { useSelector } from 'react-redux';
import { selectSelectedModules } from '../app/appSlice';

// Component imports
import CourseTopic from './CourseTopic';
import InfoAlert from './alerts/InfoAlert';

function CourseTOC (props) {

  const selectedModules = useSelector(selectSelectedModules);

  // The table of contents object could be null, empty array or empty object.
  if (!Array.isArray(props.topics) || !props.topics.length) {
    return <div className="mt-4"><InfoAlert message="This course does not have topics"/></div>;
  }

  return props.topics.slice().map( (topic, index) => {
    return <CourseTopic
             key={index}
             index={index}
             selected={selectedModules && selectedModules.includes(index)}
             topic={topic}
            />;
  });

}

export default CourseTOC;
