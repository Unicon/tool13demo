package net.unicon.lti.model.membership;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUsers {
    private List<CourseUser> courseUserList = new ArrayList<>();

    public CourseUsers() {//Empty on purpose
    }

    @JsonProperty("members")
    public List<CourseUser> getCourseUserList() {
        return courseUserList;
    }

    public void setCourseUserList(List<CourseUser> courseUserList) {
        this.courseUserList = courseUserList;
    }
}
