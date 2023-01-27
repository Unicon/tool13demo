package net.unicon.lti.controller.app;

import com.google.api.services.classroom.model.Course;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.GcCourseEntity;
import net.unicon.lti.model.GcLinkEntity;
import net.unicon.lti.model.GcUserEntity;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.service.gc.GoogleClassroomService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LtiOidcUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import static net.unicon.lti.utils.TextConstants.LTI3_SUFFIX;

@Controller
@Scope("session")
@RequestMapping("/app")
@Slf4j
public class AppController {
    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    GoogleClassroomService googleClassroomService;

    // Launches a mock standalone home page with a button to start the LTI DL --> GC CW flow.
    @RequestMapping({"", "/{linkUuid}"})
    public String appInit(HttpServletRequest req, Model model, @PathVariable(value = "linkUuid", required = false) String linkUuid, @RequestParam(value = "link", required = false) String link) throws GeneralSecurityException, IOException {
        String targetUri = ltiDataService.getLocalUrl() + TextConstants.LTI3_SUFFIX;
        if (linkUuid != null) {
            targetUri = targetUri + "?gcLink=" + linkUuid;
            if (link != null) {
                // TODO: Ultimately I end up letting the original link id just fall by the wayside. That should probably change in the end.
                targetUri = targetUri + "&link=" + link;
            }
        }

        LoginInitiationDTO loginInitiationDTO = new LoginInitiationDTO(
                ltiDataService.getLocalUrl(),
                "uuid or user's gc course id",
                targetUri,
                LtiOidcUtils.generateLtiMessageHint(ltiDataService, linkUuid, link),
                "self-client-id",
                "self-deployment-id"
        );
        model.addAttribute("oidcRequestUrl", ltiDataService.getLocalUrl() + "/oidc/login_initiations");
        model.addAttribute("oidcLoginRequest", loginInitiationDTO);

        if (StringUtils.isBlank(linkUuid)) { // if no linkUuid then go to "home page" to pick course for content selection (Deep Linking) launch
            List<Course> courses = googleClassroomService.getCoursesFromGoogleClassroom();
            model.addAttribute("courses", courses);
            model.addAttribute("title", "App!");
            log.debug("Going to home page to pick course for content selection...");
            return "app";
        } else { // if linkUuid present, do LTI launch to specific link
            log.debug("Sending platform OIDC Login Request...");
            return "platformOIDCLoginRequest";
        }
    }

    // Acts as a platform's LTI auth response containing an id_token with a Deep Linking message type.
    @RequestMapping({"/platform-oidc-authorize"})
    public String generatePlatformOidcAuthorization(HttpServletRequest req, Model model) throws GeneralSecurityException {
        log.debug("Received platform authorization request.");
        // TODO validate nonce
        Jws<Claims> jwt = ltijwtService.validateState(req.getParameter("lti_message_hint"));
        String linkUuid = jwt.getBody().get("linkUuid") != null ? jwt.getBody().get("linkUuid").toString() : null;
        model.addAttribute("state", req.getParameter("state"));
        String target = ltiDataService.getLocalUrl() + LTI3_SUFFIX;

        GcUserEntity gcUserEntity = googleClassroomService.getCurrentUser(linkUuid);
        GcLinkEntity gcLinkEntity = googleClassroomService.getGcLinkByUuid(linkUuid);

        if (linkUuid == null) { // deep linking flow
            log.debug("Preparing deep linking flow...");
            GcCourseEntity gcCourseEntity = googleClassroomService.getGcCourseByGcCourseId(req.getParameter("login_hint"));
            model.addAttribute("id_token", LtiOidcUtils.generateLtiIdToken(ltiDataService, req.getParameter("nonce"), gcUserEntity, gcCourseEntity, gcLinkEntity, true));
        } else { // resource link flow
            log.debug("Preparing resource link flow...");
            GcCourseEntity gcCourseEntity = googleClassroomService.getGcCourseFromLinkId(linkUuid);
            target = target + "?link=" + linkUuid;
            // TODO: May want to query GC to ensure that gcCourse is up to date in db before generating id_token
            model.addAttribute("id_token", LtiOidcUtils.generateLtiIdToken(ltiDataService, req.getParameter("nonce"), gcUserEntity, gcCourseEntity, gcLinkEntity, false));
        }
        model.addAttribute("target", target);
        log.debug("Sending platform auth response with id_token...");
        return "platformAuthResponse";
    }

    // Handles deep linking responses, converting them into Google Classroom Coursework
    @RequestMapping({"/gccoursework/{gcCourseId}"})
    public String gcCourseWork(HttpServletRequest req, Model model, @PathVariable("gcCourseId") String gcCourseId) throws GeneralSecurityException, IOException {
        // validate and parse jwt
        Jws<Claims> jwt = ltijwtService.validateState(req.getParameter("JWT"));
        if (jwt == null) {
            model.addAttribute("Error", "Could not validate or parse JWT.");
            return "app";
        }

        // insert the links into that course
        googleClassroomService.addClassworkMaterials(gcCourseId, jwt);

        model.addAttribute("title", "Your assignments have been added to Google Classroom!");
        return "gcAddMaterialsResponse";
    }
}
