// Redux imports
import { useDispatch, useSelector } from 'react-redux';
import { changeSearchInput, selectSearchInputText } from '../../app/appSlice';

// Component imports
import InputGroup from 'react-bootstrap/InputGroup';
import Form from 'react-bootstrap/Form';

function SearchInput (props) {

  const dispatch = useDispatch();
  const searchInputText = useSelector(selectSearchInputText);

  return <InputGroup>
           <InputGroup.Text className="bg-white border-end-0"><i className="fa fa-search"></i></InputGroup.Text>
           <Form.Control value={searchInputText}
             type="search"
             className="border-start-0 search-input"
             placeholder="Search by keyword"
             onChange={(e) => dispatch(changeSearchInput(e.target.value))}
           />
         </InputGroup>;
}

export default SearchInput;
