package net.unicon.lti.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "gc_user")
@Getter
@Setter
@NoArgsConstructor
public class GcUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;
    @Column(name = "gc_user_id", nullable = false, length = 4096)
    private String gcUserId;
    @Basic
    @Column(name = "email")
    private String email;
    @Basic
    @Column(name = "photo_url")
    private String photoUrl;
    @Basic
    @Column(name = "given_name")
    private String givenName;
    @Basic
    @Column(name = "family_name")
    private String familyName;
    @Basic
    @Column(name = "full_name")
    private String fullName;
    @Basic
    @Column(name = "permissions")
    private String permissions;
    @Basic
    @Column(name = "verified_teacher")
    private boolean verifiedTeacher;
    @ElementCollection
    @CollectionTable(name="gc_lti_roles")
    private List<String> ltiRoles;

    public GcUserEntity(String gcUserId, String email, String photoUrl, String givenName, String familyName, String fullName, String permissions, boolean verifiedTeacher, List<String> ltiRoles) {
        this.gcUserId = gcUserId;
        this.email = email;
        this.photoUrl = photoUrl;
        this.givenName = givenName;
        this.familyName = familyName;
        this.fullName = fullName;
        this.permissions = permissions;
        this.verifiedTeacher = verifiedTeacher;
        this.ltiRoles = ltiRoles;
    }
}
