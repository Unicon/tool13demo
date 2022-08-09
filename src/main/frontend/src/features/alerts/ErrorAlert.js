// Component imports
import Alert from 'react-bootstrap/Alert';

function ErrorAlert(props) {
  return <Alert variant="danger"
           onClose={props.onClose}
           dismissible={props.dismissible}>
             <i className="fa fa-exclamation-circle error-icon" aria-hidden="true"></i> {props.message}
         </Alert>;
}

export default ErrorAlert;
