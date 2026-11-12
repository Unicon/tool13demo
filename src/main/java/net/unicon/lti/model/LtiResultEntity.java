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
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "lti_result")
public class LtiResultEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long resultId;
    @Basic
    private Float scoreGiven;
    @Basic
    private Float scoreMaximum;
    @Basic
    @Column(length = 4096)
    private String comment;
    @Basic
    private String activityProgress;
    @Basic
    private String gradingProgress;
    @Basic
    @Column(nullable = false)
    private Timestamp timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id")
    private LtiLinkEntity link;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private LtiUserEntity user;

    protected LtiResultEntity() {
    }

    /**
     * @param user        the user for this grade result
     * @param link        the link which this is a grade for
     * @param retrievedAt the date the grade was retrieved (null indicates now)
     * @param scoreGiven       [OPTIONAL] the grade value
     */
    public LtiResultEntity(LtiUserEntity user, LtiLinkEntity link, Date retrievedAt, Float scoreGiven, Float scoreMaximum, String comment, String activityProgress, String gradingProgress) {
        if (user == null) throw new AssertionError();
        if (link == null) throw new AssertionError();
        if (retrievedAt == null) {
            retrievedAt = new Date();
        }

        this.timestamp = new Timestamp(retrievedAt.getTime());
        this.user = user;
        this.link = link;
        this.scoreGiven = scoreGiven;
        this.scoreMaximum = scoreMaximum;
        this.comment = comment;
        this.activityProgress = activityProgress;
        this.gradingProgress = gradingProgress;
    }


    public long getResultId() {
        return resultId;
    }

    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    public Float getScoreGiven() {
        return scoreGiven;
    }

    public void setScoreGiven(Float scoreGiven) {
        this.scoreGiven = scoreGiven;
    }

    public Float getScoreMaximum() {
        return scoreMaximum;
    }

    public void setScoreMaximum(Float scoreMaximum) {
        this.scoreMaximum = scoreMaximum;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getActivityProgress() {
        return activityProgress;
    }

    public void setActivityProgress(String activityProgress) {
        this.activityProgress = activityProgress;
    }

    public String getGradingProgress() {
        return gradingProgress;
    }

    public void setGradingProgress(String gradingProgress) {
        this.gradingProgress = gradingProgress;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public LtiLinkEntity getLink() {
        return link;
    }

    public void setLink(LtiLinkEntity link) {
        this.link = link;
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

        LtiResultEntity that = (LtiResultEntity) o;

        return resultId == that.resultId;

    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId);
    }
}
