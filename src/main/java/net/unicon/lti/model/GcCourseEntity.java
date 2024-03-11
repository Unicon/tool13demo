package net.unicon.lti.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "gc_course")
@Setter
@Getter
@NoArgsConstructor
public class GcCourseEntity {
    // Internal id for the course
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    // Google's id for the course
    @Basic
    @Column(name = "gc_course_id", nullable = false, length = 4096)
    private String gcCourseId;

    @Basic
    @Column(name = "name", length = 4096)
    private String name;
    @Basic
    @Column(name = "section")
    private String section;
    @Basic
    @Column(name = "description_heading")
    private String descriptionHeading;
    @Basic
    @Column(name = "description")
    private String description;
    @Basic
    @Column(name = "room")
    private String room;
    @Basic
    @Column(name = "owner_id")
    private String ownerId;
    @Basic
    @Column(name = "creation_time")
    private String creationTime;
    @Basic
    @Column(name = "update_time")
    private String updateTime;
    @Basic
    @Column(name = "enrollment_code")
    private String enrollmentCode;
    @Basic
    @Column(name = "course_state")
    private String courseState;
    @Basic
    @Column(name = "alternate_link")
    private String alternateLink;
    @Basic
    @Column(name = "teacher_group_email")
    private String teacherGroupEmail;
    @Basic
    @Column(name = "course_group_email")
    private String courseGroupEmail;
    @Basic
    @Column(name = "guardians_enabled")
    private boolean guardiansEnabled;

    public GcCourseEntity(String gcCourseId, String name, String section, String descriptionHeading, String description, String room, String ownerId, String creationTime, String updateTime, String enrollmentCode, String courseState, String alternateLink, String teacherGroupEmail, String courseGroupEmail, boolean guardiansEnabled) {
        this.gcCourseId = gcCourseId;
        this.name = name;
        this.section = section;
        this.descriptionHeading = descriptionHeading;
        this.description = description;
        this.room = room;
        this.ownerId = ownerId;
        this.creationTime = creationTime;
        this.updateTime = updateTime;
        this.enrollmentCode = enrollmentCode;
        this.courseState = courseState;
        this.alternateLink = alternateLink;
        this.teacherGroupEmail = teacherGroupEmail;
        this.courseGroupEmail = courseGroupEmail;
        this.guardiansEnabled = guardiansEnabled;
    }
}
