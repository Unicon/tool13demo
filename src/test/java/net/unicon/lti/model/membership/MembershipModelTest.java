package net.unicon.lti.model.membership;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MembershipModelTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    public void testCourseUserAccessors() {
        CourseUser user = new CourseUser();
        List<String> roles = List.of("Learner");
        user.setStatus("Active");
        user.setName("Full Name");
        user.setPicture("https://pic");
        user.setGivenName("Given");
        user.setFamilyName("Family");
        user.setMiddleName("Middle");
        user.setEmail("a@b.c");
        user.setUserId("u1");
        user.setLisPersonSourcedid("sis");
        user.setLti11LegacyUserId("legacy");
        user.setRoles(roles);

        assertEquals("Active", user.getStatus());
        assertEquals("Full Name", user.getName());
        assertEquals("https://pic", user.getPicture());
        assertEquals("Given", user.getGivenName());
        assertEquals("Family", user.getFamilyName());
        assertEquals("Middle", user.getMiddleName());
        assertEquals("a@b.c", user.getEmail());
        assertEquals("u1", user.getUserId());
        assertEquals("sis", user.getLisPersonSourcedid());
        assertEquals("legacy", user.getLti11LegacyUserId());
        assertSame(roles, user.getRoles());
    }

    @Test
    public void testCourseUsersParsesMembersArray() {
        CourseUsers courseUsers = jsonMapper.readValue(
                "{\"id\":\"ignored\",\"members\":[{\"user_id\":\"u1\",\"given_name\":\"G\",\"family_name\":\"F\","
                        + "\"middle_name\":\"M\",\"lis_person_sourcedid\":\"sis\",\"lti11_legacy_user_id\":\"old\","
                        + "\"roles\":[\"Instructor\"],\"unknown\":1}]}",
                CourseUsers.class);

        assertEquals(1, courseUsers.getCourseUserList().size());
        CourseUser user = courseUsers.getCourseUserList().get(0);
        assertEquals("u1", user.getUserId());
        assertEquals("G", user.getGivenName());
        assertEquals("F", user.getFamilyName());
        assertEquals("M", user.getMiddleName());
        assertEquals("sis", user.getLisPersonSourcedid());
        assertEquals("old", user.getLti11LegacyUserId());
        assertEquals(List.of("Instructor"), user.getRoles());
    }

    @Test
    public void testCourseUsersList() {
        CourseUsers courseUsers = new CourseUsers();
        assertTrue(courseUsers.getCourseUserList().isEmpty());
        List<CourseUser> list = List.of(new CourseUser());
        courseUsers.setCourseUserList(list);
        assertSame(list, courseUsers.getCourseUserList());
    }
}
