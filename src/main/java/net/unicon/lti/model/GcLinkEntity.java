package net.unicon.lti.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gc_link")
@Getter
@Setter
@NoArgsConstructor
public class GcLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;
    @Column(name = "uuid", nullable = false, length = 4096)
    private String uuid;
    @Basic
    @Column(name = "title", length = 4096)
    private String title;
    @Basic
    @Column(name = "url")
    private String url;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private GcCourseEntity gcCourseEntity;

    public GcLinkEntity(String title, GcCourseEntity gcCourseEntity) {
        this.uuid = UUID.randomUUID().toString();
        this.title = title;
        this.gcCourseEntity = gcCourseEntity;
    }
}
