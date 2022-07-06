import React from 'react';
import Form from 'react-bootstrap/Form';
import { useDispatch, useSelector } from 'react-redux';
import { changeSearchInput, selectSearchInputText } from '../../app/appSlice';

function SearchInput (props) {

  const dispatch = useDispatch();
  const searchInputText = useSelector(selectSearchInputText);

  return (<Form>
            <Form.Group>
              <Form.Control className="search-input" value={searchInputText}
                type="search"
                placeholder=" &#xF002; Search by keyword"
                onChange={(e) => dispatch(changeSearchInput(e.target.value))}
              />
            </Form.Group>
          </Form>);

}

export default SearchInput;
