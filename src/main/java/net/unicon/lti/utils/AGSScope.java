package net.unicon.lti.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AGSScope {
    AGS_LINEITEMS_SCOPE("https://purl.imsglobal.org/spec/lti-ags/scope/lineitem"),
    AGS_RESULTS_SCOPE("https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly"),
    AGS_SCORES_SCOPE("https://purl.imsglobal.org/spec/lti-ags/scope/score");

    @Getter private String scope;

}
