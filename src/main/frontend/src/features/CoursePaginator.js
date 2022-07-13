// Redux imports
import { useDispatch, useSelector } from 'react-redux';
import { fetchCourses, selectMetadata } from '../app/appSlice';
// Components import
import Pagination from 'react-bootstrap/Pagination'

function CoursePaginator () {

  const dispatch = useDispatch();
  const metadata = useSelector(selectMetadata);
  const active = parseInt(metadata.page);

  const pages = [];
  // Push a link to the first page
  pages.push(<Pagination.First key="first" onClick={() => dispatch(fetchCourses(1))}/>);
  // Push a link for every page present in the metadata.
  for (let number = 1; number <= metadata.page_count; number++) {
    pages.push(
      <Pagination.Item key={number} active={number === active} onClick={() => dispatch(fetchCourses(number))}>
        {number}
      </Pagination.Item>
    );
  }
  // Push a link for the last page present in the metadata.
  pages.push(<Pagination.Last key="last" onClick={() => dispatch(fetchCourses(metadata.page_count))}/>);

  return <Pagination>{pages}</Pagination>;

}

export default CoursePaginator;
