/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti13demo.model;


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
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "lti_result")
public class LtiResultEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "result_id", nullable = false)
    private long resultId;
    @Basic
    @Column(name = "score_given")
    private Float scoreGiven;
    @Basic
    @Column(name = "score_maximum")
    private Float scoreMaximum;
    @Basic
    @Column(name = "comment", length = 4096)
    private String comment;
    @Basic
    @Column(name = "activity_progress")
    private String activityProgress;
    @Basic
    @Column(name = "grading_progress")
    private String gradingProgress;
    @Basic
    @Column(name = "timestamp", nullable = false )
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
        this.comment = activityProgress;
        this.comment = gradingProgress;
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

        return (resultId == that.resultId);

    }

}
