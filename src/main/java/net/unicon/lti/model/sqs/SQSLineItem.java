package net.unicon.lti.model.sqs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.utils.LtiStrings;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class SQSLineItem {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH.mm.ss.SSS'Z'");

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("deployment_id")
    private String deploymentId;

    private String issuer;

    @JsonProperty("lineitem_url")
    private String lineitemUrl;

    private float score; // between 0.0 - 1.0

    public Score toScore() {
        Score score = new Score();
        score.setUserId(this.userId);
        score.setScoreGiven(this.score);
        score.setScoreMaximum(1.0f);
        score.setActivityProgress(LtiStrings.ACTIVITY_PROGRESS_COMPLETED);
        score.setGradingProgress(LtiStrings.GRADING_PROGRESS_FULLY_GRADED);
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(FORMATTER);
        score.setTimestamp(now);
        return score;
    }
}
