// Component imports
import Alert from 'react-bootstrap/Alert';

function ErrorAlert(props) {
  return <Alert variant="danger"><i className="fa fa-exclamation-circle error-icon" aria-hidden="true"></i> {props.message} </Alert>;
}

export default ErrorAlert;
