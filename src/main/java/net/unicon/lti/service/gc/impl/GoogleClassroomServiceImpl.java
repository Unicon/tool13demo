package net.unicon.lti.service.gc.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Link;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.Material;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.Teacher;
import com.google.api.services.classroom.model.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.model.GcCourseEntity;
import net.unicon.lti.model.GcLinkEntity;
import net.unicon.lti.model.GcUserEntity;
import net.unicon.lti.repository.GcCourseRepository;
import net.unicon.lti.repository.GcLinkRepository;
import net.unicon.lti.repository.GcUserRepository;
import net.unicon.lti.service.gc.GoogleClassroomService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static net.unicon.lti.utils.LtiStrings.DEEP_LINK_TITLE;
import static net.unicon.lti.utils.LtiStrings.LTI_CONTENT_ITEMS;
import static net.unicon.lti.utils.LtiStrings.LTI_ROLE_INSTRUCTOR;
import static net.unicon.lti.utils.LtiStrings.LTI_ROLE_OTHER;
import static net.unicon.lti.utils.LtiStrings.LTI_ROLE_STUDENT;

@Slf4j
@Service
public class GoogleClassroomServiceImpl implements GoogleClassroomService {
    @Autowired
    GcCourseRepository gcCourseRepository;

    @Autowired
    GcLinkRepository gcLinkRepository;

    @Autowired
    GcUserRepository gcUserRepository;

    protected static final String APPLICATION_NAME = "Google Classroom API Java Quickstart";
    protected static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    protected static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(
            ClassroomScopes.CLASSROOM_COURSES_READONLY,
            ClassroomScopes.CLASSROOM_COURSES,
            ClassroomScopes.CLASSROOM_COURSEWORK_ME,
            ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS,
            ClassroomScopes.CLASSROOM_ROSTERS,
            ClassroomScopes.CLASSROOM_ROSTERS_READONLY,
            ClassroomScopes.CLASSROOM_PROFILE_EMAILS,
            ClassroomScopes.CLASSROOM_PROFILE_PHOTOS,
            "openid",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/userinfo.email"
    );
    private static final String CREDENTIALS_FILE_PATH = "/google-cloud-credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    protected Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) {
        try {
            // Load client secrets.
            InputStream in = GoogleClassroomServiceImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            // TODO: CHANGE THIS SO THAT YOU DON'T HAVE TO COPY/PASTE LINKS OUT OF THE LOGS TO CONTINUE
            // TODO: FIND OUT IF THIS REALLY HAS TO BE ON A DIFFERENT PORT THAN EVERYTHING ELSE AND WHY THAT IS
            // TODO: FIND OUT IF IT WOULD BE A PROBLEM IN PROD THAT THIS DIRECTS TO LOCALHOST, HOW TO FIX
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (IOException e) {
            log.error(e.getStackTrace().toString());
        }
        return null;
    }

    public GcUserEntity getCurrentUser(String linkUuid) {
        try {
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Classroom service =
                    new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            UserProfile userProfile = service.userProfiles().get("me").execute();
            if (userProfile == null) {
                log.error("Could not retrieve current Google Classroom user.");
                return null;
            }
            log.debug("Found user in GC");

            GcUserEntity gcUserEntity = gcUserRepository.getByGcUserId(userProfile.getId());
            boolean gcUserEntityDoesNotExist = gcUserEntity == null || gcUserEntity.getGcUserId() == null;
            ArrayList<String> ltiRoles = new ArrayList<>();
            if ((gcUserEntityDoesNotExist || gcUserEntity.getLtiRoles().isEmpty()) && StringUtils.isNotBlank(linkUuid)) {
                log.debug("User does not exist or does not have roles and a link uuid is available for gc course lookup");
                GcCourseEntity gcCourseEntity = gcLinkRepository.getByUuid(linkUuid).getGcCourseEntity();
                if (gcCourseEntity != null && StringUtils.isNotBlank(gcCourseEntity.getGcCourseId())) {
                    log.debug("Associated course was found in db.");
                    // TODO: See if this can be handled without the try-catches.
                    try {
                        Teacher teacher = service.courses().teachers().get(gcCourseEntity.getGcCourseId(), userProfile.getId()).execute();
                        if (teacher != null) {
                            ltiRoles.add(LTI_ROLE_INSTRUCTOR);
                        }
                    } catch (GoogleJsonResponseException e) {
                        if (e.getDetails().getCode() != 404) { // if response is 404, that just means the user is not a teacher
                            e.printStackTrace();
                            return null;
                        }
                        log.debug("User is not a teacher.");
                    }
                    try {
                        Student student = service.courses().students().get(gcCourseEntity.getGcCourseId(), userProfile.getId()).execute();
                        if (student != null) {
                            ltiRoles.add(LTI_ROLE_STUDENT);
                        }
                    } catch (GoogleJsonResponseException e) {
                        if (e.getDetails().getCode() != 404) { // if response is 404, that just means the user is not a student
                            e.printStackTrace();
                            return null;
                        }
                        log.debug("User is not a student.");
                    }
                    if (ltiRoles.isEmpty()) {
                        ltiRoles.add(LTI_ROLE_OTHER);
                    }
                    log.debug("ltiRoles are {}", ltiRoles);
                } else {
                    log.debug("Could not find course for linkUuid {}", linkUuid);
                }
            }
            if (gcUserEntityDoesNotExist) {
                gcUserEntity = new GcUserEntity(
                        userProfile.getId(),
                        userProfile.getEmailAddress(),
                        userProfile.getPhotoUrl(),
                        userProfile.getName().getGivenName(),
                        userProfile.getName().getFamilyName(),
                        userProfile.getName().getFullName(),
                        userProfile.getPermissions().toString(),
                        userProfile.getVerifiedTeacher() != null ? userProfile.getVerifiedTeacher() : false,
                        ltiRoles.isEmpty() ? null : ltiRoles
                );
                log.debug("Created user: {} - {} - {}", gcUserEntity.getGcUserId(), gcUserEntity.getEmail(), gcUserEntity.getFullName());
                gcUserRepository.save(gcUserEntity);
            } else if (!ltiRoles.isEmpty()) {
                gcUserEntity.setLtiRoles(ltiRoles);
                log.debug("Updating user roles to {}", ltiRoles);
                gcUserRepository.save(gcUserEntity);
            }

            return gcUserEntity;
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addClassworkMaterials(String gcCourseId, Jws<Claims> jwt) {
        try {
            String linkPath = "/app";
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Classroom service =
                    new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            GcCourseEntity gcCourseEntity = gcCourseRepository.getByGcCourseId(gcCourseId);
            if (gcCourseEntity == null || gcCourseEntity.getGcCourseId() == null) {
                throw new AssertionError("GC course entity must exist.");
            }

            CourseWork courseWork;
            ArrayList<LinkedHashMap> contentItems = jwt.getBody().get(LTI_CONTENT_ITEMS, ArrayList.class);
            for (LinkedHashMap contentItem : contentItems) {
                GcLinkEntity gcLinkEntity = new GcLinkEntity(
                        contentItem.get("title").toString(),
                        gcCourseEntity
                );

                String contentItemUrl = UriComponentsBuilder.fromUriString(contentItem.get("url").toString()).replacePath(linkPath + "/" + gcLinkEntity.getUuid()).build().toUriString();
                gcLinkEntity.setUrl(contentItemUrl);

                // Create a link to add as a material on course work.
                Link link = new Link()
                        .setTitle(contentItem.get(DEEP_LINK_TITLE).toString())
                        .setUrl(contentItemUrl);

                // Create a list of Materials to add to course work.
                List<Material> materials = Arrays.asList(new Material().setLink(link));

                CourseWork content = new CourseWork()
                        .setTitle(contentItem.get(DEEP_LINK_TITLE).toString())
                        .setMaterials(materials)
                        .setWorkType("ASSIGNMENT")
                        .setState("PUBLISHED");

                log.debug("Creating course work in gc: gcCourseId: {}, content: {}", gcCourseId, content);
                courseWork = service.courses().courseWork().create(gcCourseId, content).execute();

                log.debug("Saving gc link to db...");
                gcLinkRepository.save(gcLinkEntity);

                /* Prints the created courseWork. */
                log.debug("CourseWork created: {}\n", courseWork.getTitle());
            }
        } catch (GoogleJsonResponseException e) {
            //TODO (developer) - handle error appropriately
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 404) {
                log.error("The gcCourseId does not exist: {}.\n", gcCourseId);
            } else {
                log.error(e.getStackTrace().toString());
            }
        } catch (Exception e) {
            log.error(e.getStackTrace().toString());
        }
    }

    public List<Course> getCoursesFromGoogleClassroom() {
        try {
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Classroom service =
                    new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            // List the first 10 courses that the user has access to.
            ListCoursesResponse response = service.courses().list().execute();
            List<Course> courses = response.getCourses();
            if (courses == null || courses.size() == 0) {
                log.error("No courses found.");
            } else {
                log.debug("Courses:");
                for (Course course : courses) {
                    log.debug("{}\n", course.getName());
                }
            }

            return courses;
        } catch(IOException | GeneralSecurityException e) {
            log.error(e.getStackTrace().toString());
        }
        return null;
    }

    public GcLinkEntity getGcLinkByUuid(String linkUuid) {
        return gcLinkRepository.getByUuid(linkUuid);
    }

    public GcCourseEntity getGcCourseFromLinkId(String linkUuid) {
        GcLinkEntity gcLinkEntity = gcLinkRepository.getByUuid(linkUuid);
        return gcLinkEntity.getGcCourseEntity();
    }

    public GcCourseEntity getGcCourseByGcCourseId(String gcCourseId) {
        try {
            GcCourseEntity gcCourseEntity = gcCourseRepository.getByGcCourseId(gcCourseId);
            if (gcCourseEntity != null && StringUtils.isNotBlank(gcCourseEntity.getGcCourseId())) {
                return gcCourseEntity;
            }

            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Classroom service =
                    new Classroom.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            Course course = service.courses().get(gcCourseId).execute();
            if (course == null) {
                log.error("Course should not be null");
                return null;
            }

            gcCourseEntity = new GcCourseEntity(
                    course.getId(),
                    course.getName(),
                    course.getSection(),
                    course.getDescriptionHeading(),
                    course.getDescription(),
                    course.getRoom(),
                    course.getOwnerId(),
                    course.getCreationTime(),
                    course.getUpdateTime(),
                    course.getEnrollmentCode(),
                    course.getCourseState(),
                    course.getAlternateLink(),
                    course.getTeacherGroupEmail(),
                    course.getCourseGroupEmail(),
                    course.getGuardiansEnabled()
            );

            gcCourseRepository.save(gcCourseEntity);

            return gcCourseEntity;
        } catch(IOException | GeneralSecurityException e) {
            log.error(e.getStackTrace().toString());
        }
        return null;
    }
}
