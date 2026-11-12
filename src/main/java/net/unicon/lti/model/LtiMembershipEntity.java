package net.unicon.lti.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "lti_membership")
public class LtiMembershipEntity extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long membershipId;
    @Basic
    private Integer role;
    @Basic
    private Integer roleOverride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id")
    private LtiContextEntity context;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private LtiUserEntity user;

    protected LtiMembershipEntity() {
    }

    public LtiMembershipEntity(LtiContextEntity context, LtiUserEntity user, Integer role) {
        if (user == null) throw new AssertionError();
        if (context == null) throw new AssertionError();
        this.user = user;
        this.context = context;
        this.role = role;
    }

    public long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(long membershipId) {
        this.membershipId = membershipId;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getRoleOverride() {
        return roleOverride;
    }

    public void setRoleOverride(Integer roleOverride) {
        this.roleOverride = roleOverride;
    }

    public LtiContextEntity getContext() {
        return context;
    }

    public void setContext(LtiContextEntity context) {
        this.context = context;
    }

    public LtiUserEntity getUser() {
        return user;
    }

    public void setUser(LtiUserEntity user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LtiMembershipEntity that = (LtiMembershipEntity) o;

        if (context.getContextId() != that.context.getContextId()) return false;
        if (membershipId != that.membershipId) return false;
        if (user.getUserId() != that.user.getUserId()) return false;
        return Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        int result = (int) membershipId;
        result = 31 * result + (int) context.getContextId();
        result = 31 * result + (int) user.getUserId();
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

}
