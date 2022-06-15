import Row from 'react-bootstrap/Row';

function CourseSection (props) {
  return (
    <Row>
      <div className="section-header">
        <h2>{props.section.name}</h2>
      </div>
      <div>
        <ul>
          {props.section.items.map((item, index) => {
            return <li key={index}>{item}</li>
          })}
        </ul>
      </div>
    </Row>
  );

}

export default CourseSection;
