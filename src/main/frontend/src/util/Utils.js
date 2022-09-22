import missingPreview from '../media/preview.png';
import missingCover from '../media/course.png';

export const formatDate = (date) => {
    return new Date(date).toLocaleDateString();
}

export const parseCourseCoverImage = (source, preview) => {
    if (source === null || source.includes('example')) {
      return preview ? missingPreview : missingCover;
    } else {
      return source;
    }
}

// Simple function to verify that the rootOutcomeGuid is a valid GUID.
export const isValidRootOutcomeGuid = (rootOutcomeGuid) => {
  return rootOutcomeGuid && rootOutcomeGuid.split('-').length === 5;
}
