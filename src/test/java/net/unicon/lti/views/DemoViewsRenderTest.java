/**
 * Copyright 2026 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.views;

import net.unicon.lti.model.ToolLink;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.Result;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.lti.dto.DeepLinkDTO;
import net.unicon.lti.model.lti.dto.LoginInitiationDTO;
import net.unicon.lti.model.lti.dto.PlatformRegistrationDTO;
import net.unicon.lti.model.lti.dto.ToolRegistrationDTO;
import net.unicon.lti.model.membership.CourseUser;
import net.unicon.lti.utils.DemoFormatter;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Renders every demo view with realistic sample data, so a broken Thymeleaf expression fails the build
 * instead of a live launch. Set -Ddemo.views.out=/some/dir to also save the rendered pages for a visual check.
 */
public class DemoViewsRenderTest {

    private static SpringTemplateEngine engine;
    private static GenericApplicationContext appContext;
    private static MockServletContext servletContext;

    private static final String ID_TOKEN = fakeJwt(
            "{\"alg\":\"RS256\",\"kid\":\"2026-11-01T00:00:00Z\",\"typ\":\"JWT\"}",
            "{\"iss\":\"https://canvas.instructure.com\",\"aud\":\"97140000000000754\",\"sub\":\"b7c1e2a4-5d6f-4a8b-9c0d-1e2f3a4b5c6d\","
                    + "\"nonce\":\"3f1c9a6e-2b7d-4e8f-a1c2-d3e4f5a6b7c8\",\"name\":\"Phil Ball\","
                    + "\"https://purl.imsglobal.org/spec/lti/claim/message_type\":\"LtiResourceLinkRequest\","
                    + "\"https://purl.imsglobal.org/spec/lti/claim/context\":{\"id\":\"4dde05e8ca1973bcca9bffc13e1548820eee93a3\",\"title\":\"Phil's Sandbox\"}}");

    @BeforeAll
    public static void setUp() {
        appContext = new GenericApplicationContext();
        appContext.registerBean("demoFormatter", DemoFormatter.class);
        appContext.refresh();

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setEnableSpringELCompiler(false);

        servletContext = new MockServletContext();
    }

    private String render(String template, Map<String, Object> model, Map<String, String> params) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        params.forEach(request::addParameter);
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(app.buildExchange(request, new MockHttpServletResponse()), Locale.US);
        context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(appContext, new DefaultConversionService()));
        context.setVariables(model);
        String html = engine.process(template, context);

        String out = System.getProperty("demo.views.out");
        if (out != null) {
            String name = template + (params.isEmpty() ? "" : "-" + String.join("-", params.values())) + ".html";
            Files.createDirectories(Path.of(out));
            Files.writeString(Path.of(out, name), html, StandardCharsets.UTF_8);
        }
        return html;
    }

    private String render(String template, Map<String, Object> model) throws Exception {
        return render(template, model, Collections.emptyMap());
    }

    // ---------- Launch flow ----------

    @Test
    public void audienceChooser() throws Exception {
        String html = render("demoAudienceChooser", Map.of("initiation_dto", loginInitiation()));
        assertTrue(html.contains("Who is this demo for?"));
        assertTrue(html.contains("value=\"97140000000000754\""));
    }

    @Test
    public void step1LoginInitiation() throws Exception {
        for (String audience : Arrays.asList("technical", "client")) {
            Map<String, Object> model = new LinkedHashMap<>(authRequest());
            model.put("initiation_dto", loginInitiation());
            model.put("client_id_received", "97140000000000754");
            model.put("deployment_id_received", "995:312a6c68150f3e8b789252ac1f739510ac44aa07");
            model.put("lti_storage_target", "_parent");
            model.put("oidc_authorization_uri", "https://sso.canvaslms.com/api/lti/authorize_redirect");
            model.put("demoAudience", audience);
            String html = render("oidcRedirect", model, Map.of("view", audience));
            assertTrue(html.contains("id=\"get_button\""));
            assertTrue(html.contains("data-tour-autostart=\"" + !"client".equals(audience) + "\""));
        }
    }

    @Test
    public void step3StateAndNonceCheck() throws Exception {
        for (String target : Arrays.asList("_parent", null)) {
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("expected_state", "5d1f0c3b9a7e46f2b8c1d0e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9");
            model.put("expected_nonce", "3f1c9a6e-2b7d-4e8f-a1c2-d3e4f5a6b7c8");
            model.put("token", "eyJ0eXAiOiJKV1QifQ.e30.sig");
            model.put("id_token", ID_TOKEN);
            model.put("ltiStorageTarget", target);
            model.put("oidc_authorization_uri", "https://sso.canvaslms.com/api/lti/authorize_redirect");
            model.put("demoAudience", "technical");
            String html = render("nonceStateCheck", model, Map.of("storage", target == null ? "cookies" : "storage"));
            assertTrue(html.contains("id=\"endpoint-form\""));
            assertTrue(html.contains("Decoded ID token claims"));
            assertTrue(html.contains("canvas.instructure.com"), "decoded token claims should render");
        }
    }

    @Test
    public void technicalLanding() throws Exception {
        Map<String, Object> model = landingModel();
        String html = render("lti3Result", model);
        assertTrue(html.contains("Launch complete"));
        assertTrue(html.contains("Instructor"));
        assertTrue(html.contains("demo_audience=technical"));
    }

    @Test
    public void clientLanding() throws Exception {
        String html = render("clientLanding", landingModel());
        assertTrue(html.contains("Biology 2e 1.1"));
        assertTrue(html.contains("demo_audience=client"));
    }

    @Test
    public void deepLinking() throws Exception {
        Map<String, Object> model = landingModel();
        model.put("deeplinks", Arrays.asList(
                new DeepLinkDTO("bio-1-1", "Biology 2e 1.1: The Science of Biology", "Reading."),
                new DeepLinkDTO("bio-1-quiz", "Chapter 1 Quiz: Study of Life", "Graded quiz (10 points).")));
        model.put("state_hash", "5d1f0c3b");
        model.put("nonce", "3f1c9a6e");
        model.put("token", "eyJ0eXAiOiJKV1QifQ.e30.sig");
        String technical = render("lti3DeepLink", model);
        assertTrue(technical.contains("id=\"deepLinkResponseForm\""));
        assertTrue(technical.contains("Example: two links"));
        String client = render("clientDeepLink", model);
        assertTrue(client.contains("Add to course"));
    }

    // ---------- LTI Advantage services ----------

    @Test
    public void roster() throws Exception {
        CourseUser teacher = courseUser("Phil Ball", "b7c1e2a4", "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor");
        CourseUser student = courseUser("Test Student", "c8d2f3b5", "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner");
        Map<String, Object> model = Map.of("results", Arrays.asList(teacher, student), "noAdvantageToken", false);
        String technical = render("ltiAdvMembershipMain", model, Map.of("demo_audience", "technical"));
        assertTrue(technical.contains("2 members"));
        assertTrue(technical.contains("b7c1e2a4"));
        String client = render("ltiAdvMembershipMain", model, Map.of("demo_audience", "client"));
        assertTrue(client.contains("Learner"));
        assertFalse(client.contains("b7c1e2a4"), "client view hides user ids");
        render("ltiAdvMembershipMain", Map.of("noSessionValues", true), Map.of("demo_audience", "none"));
    }

    @Test
    public void lineItemsAndScores() throws Exception {
        LineItem item = lineItem();
        Map<String, Object> listModel = new LinkedHashMap<>();
        listModel.put("lineitems", Collections.singletonList(item));
        listModel.put("advantageToken", "adv.token.value");
        listModel.put("noAdvantageToken", false);
        String technical = render("ltiAdvAgsMain", listModel, Map.of("demo_audience", "technical"));
        assertTrue(technical.contains("Create a line item"));
        assertTrue(technical.contains("/ags/1234?advantageToken="), "line item id should be extracted from its URL");
        String client = render("ltiAdvAgsMain", listModel, Map.of("demo_audience", "client"));
        assertFalse(client.contains("Create a line item"));

        Map<String, Object> detailModel = new LinkedHashMap<>(listModel);
        String detail = render("ltiAdvAgsDetail", detailModel, Map.of("demo_audience", "technical"));
        assertTrue(detail.contains("Post a score"));
        assertTrue(detail.contains("8 / 10"));
        render("ltiAdvAgsDetail", detailModel, Map.of("demo_audience", "client"));
        render("ltiAdvAgsMain", Map.of("noSessionValues", true), Map.of("demo_audience", "none"));
    }

    // ---------- Errors and registration ----------

    @Test
    public void errors() throws Exception {
        String html = render("lti3Error", Map.of("Error",
                "Not found any existing tool deployment with iss: https://canvas.instructure.com clientId: 97140000000000754 deploymentId: 995:abc"));
        assertTrue(html.contains("Likely cause"));
        render("lti3Error", Map.of("Error", "State was not as expected"), Map.of("case", "state"));
        render("registrationError", Map.of("Error", "Registration token expired"));
    }

    @Test
    public void dynamicRegistration() throws Exception {
        PlatformRegistrationDTO platform = new PlatformRegistrationDTO();
        platform.setIssuer("https://canvas.instructure.com");
        platform.setAuthorization_endpoint("https://sso.canvaslms.com/api/lti/authorize_redirect");
        platform.setToken_endpoint("https://sso.canvaslms.com/login/oauth2/token");
        platform.setJwks_uri("https://sso.canvaslms.com/api/lti/security/jwks");
        platform.setScopes_supported(Arrays.asList("https://purl.imsglobal.org/spec/lti-ags/scope/score",
                "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly"));
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("openid_configuration", "https://canvas.unicon.net/api/lti/security/openid-configuration");
        model.put("registration_token", "eyJhbGciOiJIUzI1NiJ9.registration.token");
        model.put("own_redirect_post_endpoint", "https://tool.example/registration/");
        model.put("platform_configuration", platform);
        model.put("tool_configuration", new ToolRegistrationDTO());
        String html = render("registrationRedirect", model);
        assertTrue(html.contains("Register with the LMS"));
        String done = render("registrationConfirmationDemo", Map.of("registration_confirmation", "{\"client_id\":\"97140000000000754\"}"));
        assertTrue(done.contains("97140000000000754"));
    }

    // ---------- Sample data ----------

    private static LoginInitiationDTO loginInitiation() {
        return new LoginInitiationDTO("https://canvas.instructure.com", "a1b2c3d4e5f6", "https://tool.example/lti3?link=bio-1-1",
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ2ZXJpZmllciI6ImFiYyJ9.message-hint-signature-value", "97140000000000754",
                null, "995:312a6c68150f3e8b789252ac1f739510ac44aa07", "_parent");
    }

    private static Map<String, Object> authRequest() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("client_id", "97140000000000754");
        m.put("login_hint", "a1b2c3d4e5f6");
        m.put("lti_message_hint", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ2ZXJpZmllciI6ImFiYyJ9.message-hint-signature-value");
        m.put("nonce", "3f1c9a6e-2b7d-4e8f-a1c2-d3e4f5a6b7c8");
        m.put("prompt", "none");
        m.put("redirect_uri", "https://tool.example/lti3");
        m.put("response_mode", "form_post");
        m.put("response_type", "id_token");
        m.put("scope", "openid");
        m.put("state", "5d1f0c3b9a7e46f2b8c1d0e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9");
        m.put("oidcEndpoint", "https://sso.canvaslms.com/api/lti/authorize_redirect");
        m.put("oidcEndpointComplete", "https://sso.canvaslms.com/api/lti/authorize_redirect?client_id=97140000000000754&prompt=none");
        return m;
    }

    private static Map<String, Object> landingModel() {
        LTI3Request request = mock(LTI3Request.class);
        when(request.getLtiTargetLinkUrl()).thenReturn("https://tool.example/lti3?link=bio-1-1");
        when(request.getLtiLinkId()).thenReturn("e1f2a3b4-resource-link");
        when(request.getLtiLinkTitle()).thenReturn("Biology 2e 1.1: The Science of Biology");
        when(request.getLtiMessageType()).thenReturn("LtiResourceLinkRequest");
        when(request.getLtiVersion()).thenReturn("1.3.0");
        when(request.getSub()).thenReturn("b7c1e2a4-5d6f-4a8b-9c0d-1e2f3a4b5c6d");
        when(request.getLtiName()).thenReturn("Phil Ball");
        when(request.getLtiGivenName()).thenReturn("Phil");
        when(request.getLtiEmail()).thenReturn("phil@example.edu");
        when(request.getLtiRoles()).thenReturn(Arrays.asList(
                "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor",
                "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor"));
        when(request.isRoleInstructor()).thenReturn(true);
        when(request.getLtiContextTitle()).thenReturn("Phil's Sandbox");
        when(request.getLtiContextLabel()).thenReturn("SANDBOX-101");
        when(request.getLtiContextId()).thenReturn("4dde05e8ca1973bcca9bffc13e1548820eee93a3");
        when(request.getLtiToolPlatformName()).thenReturn("Unicon");
        when(request.getLtiToolPlatformProduct()).thenReturn("canvas");
        when(request.getLtiToolPlatformVersion()).thenReturn("cloud");
        when(request.getLtiDeploymentId()).thenReturn("995:312a6c68150f3e8b789252ac1f739510ac44aa07");
        when(request.getLocale()).thenReturn("en");
        when(request.getLtiNamesRoleServiceContextMembershipsUrl()).thenReturn("https://canvas.unicon.net/api/lti/courses/995/names_and_roles");
        when(request.getLtiEndpointLineItems()).thenReturn("https://canvas.unicon.net/api/lti/courses/995/line_items");
        when(request.getLtiEndpointScope()).thenReturn(Arrays.asList(
                "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem", "https://purl.imsglobal.org/spec/lti-ags/scope/score"));
        when(request.getDeepLinkReturnUrl()).thenReturn("https://canvas.unicon.net/courses/995/deep_linking_response");
        when(request.getDeepLinkAcceptTypes()).thenReturn(Arrays.asList("ltiResourceLink", "link"));
        when(request.getDeepLinkAcceptPresentationDocumentTargets()).thenReturn(Arrays.asList("iframe", "window"));
        when(request.getDeepLinkAcceptMultiple()).thenReturn("true");
        Map<String, List<String>> examples = new LinkedHashMap<>();
        examples.put("Example: two links", Arrays.asList("eyJ0eXAiOiJKV1QifQ.e30.sig", "[{\"type\":\"ltiResourceLink\",\"title\":\"One\"}]"));
        when(request.getDeepLinkJwts()).thenReturn(examples);

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("lti3Request", request);
        model.put("toolLink", new ToolLink("bio-1-1", "Biology 2e 1.1: The Science of Biology",
                "Reading. OpenStax Biology 2e, Chapter 1, Section 1.", false, null));
        model.put("htmlContent", "Link Requested:\nTool Link Id:bio-1-1");
        model.put("advantageToken", "adv.token.value");
        model.put("id_token", ID_TOKEN);
        model.put("demoAudience", "technical");
        return model;
    }

    private static CourseUser courseUser(String name, String userId, String role) {
        CourseUser user = new CourseUser();
        user.setName(name);
        user.setUserId(userId);
        user.setEmail(name.toLowerCase().replace(' ', '.') + "@example.edu");
        user.setRoles(Collections.singletonList(role));
        return user;
    }

    private static LineItem lineItem() {
        Result result = new Result();
        result.setId("https://canvas.unicon.net/api/lti/courses/995/line_items/1234/results/77");
        result.setUserId("c8d2f3b5");
        result.setResultScore("8");
        result.setResultMaximum("10");
        result.setComment("Nice work");
        Results results = new Results();
        results.setResultList(Collections.singletonList(result));
        LineItem item = new LineItem();
        item.setId("https://canvas.unicon.net/api/lti/courses/995/line_items/1234");
        item.setLabel("Chapter 1 Quiz: Study of Life");
        item.setScoreMaximum("10");
        item.setResourceId("bio-1-quiz");
        item.setResourceLinkId("e1f2a3b4-resource-link");
        item.setResults(results);
        return item;
    }

    private static String fakeJwt(String header, String payload) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".signature";
    }
}
