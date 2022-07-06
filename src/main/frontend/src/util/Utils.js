import missingPreview from '../media/missing.jpg';

export const formatDate = (date) => {
    return new Date(date).toLocaleDateString();
}

export const parseCourseCoverImage = (source) => {
    if (source === null || source.includes('example')) {
      return missingPreview;
    } else {
      return source;
    }
}
