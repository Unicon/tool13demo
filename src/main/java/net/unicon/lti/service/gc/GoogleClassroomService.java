package net.unicon.lti.service.gc;

import com.google.api.services.classroom.model.Course;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.unicon.lti.model.GcCourseEntity;
import net.unicon.lti.model.GcLinkEntity;
import net.unicon.lti.model.GcUserEntity;

import java.util.List;

public interface GoogleClassroomService {
    GcUserEntity getCurrentUser(String linkUuid);

    void addClassworkMaterials(String courseId, Jws<Claims> jwt);

    List<Course> getCoursesFromGoogleClassroom();

    GcLinkEntity getGcLinkByUuid(String linkUuid);

    GcCourseEntity getGcCourseFromLinkId(String linkUuid);

    GcCourseEntity getGcCourseByGcCourseId(String gcCourseId);
}
