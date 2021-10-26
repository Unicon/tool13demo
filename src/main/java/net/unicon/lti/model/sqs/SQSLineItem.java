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
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("deployment_id")
    private String deploymentId;

    private String issuer;

    @JsonProperty("line_item_url")
    private String lineItemUrl;

    private float score; // between 0.0 - 1.0

    public Score toScore() {
        Score score = new Score();
        score.setUserId(this.userId);
        score.setScoreGiven(this.score);
        score.setScoreMaximum(1.0f);
        score.setActivityProgress(LtiStrings.ACTIVITY_PROGRESS_COMPLETED);
        score.setGradingProgress(LtiStrings.GRADING_PROGRESS_FULLY_GRADED);
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        score.setTimestamp(now);
        return score;
    }
}
