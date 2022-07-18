// Component imports
import Alert from 'react-bootstrap/Alert';

function InfoAlert(props) {
  return <Alert><i className="fa fa-exclamation-circle info-icon" aria-hidden="true"></i> {props.message} </Alert>;
}

export default InfoAlert;
