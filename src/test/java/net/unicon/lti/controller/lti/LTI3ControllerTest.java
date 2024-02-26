package net.unicon.lti.controller.lti;

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.*;
import net.unicon.lti.service.app.APIJWTService;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.utils.LtiStrings;
import net.unicon.lti.utils.TextConstants;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(LTI3Controller.class)
public class LTI3ControllerTest {
    private static String VALID_STATE = "eyJraWQiOiJPV05LRVkiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJsdGlTdGFydGVyIiwic3ViIjoiaHR0cHM6Ly9jYW52YXMuaW5zdHJ1Y3R1cmUuY29tIiwiYXVkIjoiOTcxNDAwMDAwMDAwMDAwNDkiLCJleHAiOjE3MDg3MjgzODIsIm5iZiI6MTcwODcyNDc4MiwiaWF0IjoxNzA4NzI0NzgyLCJqdGkiOiJjYzUzNmRmNi1jYmExLTRiYjQtYmQzMC0wYmI3OTIwN2U0ZmQiLCJvcmlnaW5hbF9pc3MiOiJodHRwczovL2NhbnZhcy5pbnN0cnVjdHVyZS5jb20iLCJ0YXJnZXRMaW5rVXJpIjoiaHR0cHM6Ly90ZXN0LWx0aS51bmljb24ubmV0L2x0aTM_bGluaz0xMjM0IiwiY2xpZW50SWQiOiI5NzE0MDAwMDAwMDAwMDA0OSIsImx0aURlcGxveW1lbnRJZCI6IjIwNzo1NDQwYTA4NDIyYWIxZWU3Nzk0YTA1ODhiNWU0Y2I0YTA5NGM0MjU2Iiwibm9uY2UiOiJjYzUzNmRmNi1jYmExLTRiYjQtYmQzMC0wYmI3OTIwN2U0ZmQiLCJjb250cm9sbGVyIjoiL29pZGMvbG9naW5faW5pdGlhdGlvbnMifQ.Py2dlpu4OMhcwC1QQVohDkBZ9j7Q130kdZ0yzIDPN_8c2HiSszKJ5N7YxFto7Zp43IPlFDSkG67uIEv5GhPFZr_Ykc3kPqoc_nnaAWB-OOiuJXjFBetXFgAXCiNKYyCf2k_GZEB332R16jJnBDK3d46dXau_3dXghlg732marxWd0fNF4fqxEWR8xWZIQH9vzIyojqi6lO66QL_wc6UAfLsFbDjj7DwBPyWkJe7m09Jp7C1dySY7TaLkgP443DQalmLG_wLCtsfBE1WvUDf-YJWu-4vTp8GXZbRXfsXpz4-akfWD7rsHSdALcKihMKT5V6vNLpavoJeRhDAmf6gpig";
    private static String ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjQtMDEtMDFUMDA6MDA6MDFaXzgzMTc1YzAxLTQ3M2YtNDRkMi05MjY4LTcxOTRkYzk2MDliOCJ9.eyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9tZXNzYWdlX3R5cGUiOiJMdGlSZXNvdXJjZUxpbmtSZXF1ZXN0IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vdmVyc2lvbiI6IjEuMy4wIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJpZCI6ImEzYTE1M2I0LTc0MjgtNDViOS04OTUwLTY4MmYyMTQ5MmE3MSIsImRlc2NyaXB0aW9uIjpudWxsLCJ0aXRsZSI6Ik15IHRlc3QgbGluayIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiYXVkIjoiOTcxNDAwMDAwMDAwMDAwNDkiLCJhenAiOiI5NzE0MDAwMDAwMDAwMDA0OSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2RlcGxveW1lbnRfaWQiOiIyMDc6NTQ0MGEwODQyMmFiMWVlNzc5NGEwNTg4YjVlNGNiNGEwOTRjNDI1NiIsImV4cCI6MTcwODcyODM4MCwiaWF0IjoxNzA4NzI0NzgwLCJpc3MiOiJodHRwczovL2NhbnZhcy5pbnN0cnVjdHVyZS5jb20iLCJub25jZSI6ImNjNTM2ZGY2LWNiYTEtNGJiNC1iZDMwLTBiYjc5MjA3ZTRmZCIsInN1YiI6IjQ5OTljOTcyLTdkYTktNDBjOC1hZmU2LTNjMjc4YTllNTg5NSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vdGVzdC1sdGkudW5pY29uLm5ldC9sdGkzP2xpbms9MTIzNCIsInBpY3R1cmUiOiJodHRwczovL2NhbnZhcy5pbnN0cnVjdHVyZS5jb20vaW1hZ2VzL21lc3NhZ2VzL2F2YXRhci01MC5wbmciLCJlbWFpbCI6ImRkZWxibGFuY29AdW5pY29uLm5ldCIsIm5hbWUiOiJEaWVnbyBkZWwgQmxhbmNvIiwiZ2l2ZW5fbmFtZSI6IkRpZWdvIiwiZmFtaWx5X25hbWUiOiJkZWwgQmxhbmNvIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGlzIjp7InBlcnNvbl9zb3VyY2VkaWQiOiIwMDM0TjAwMDAycnZUWW0iLCJjb3Vyc2Vfb2ZmZXJpbmdfc291cmNlZGlkIjpudWxsLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOiI0YWJmYjIwZTg4ZDUyNjQ5YzkzMWUyYWExZDU5NzEwYjcwMmM2NGJiIiwidGl0bGUiOiJMVEkgMTMgRGVtbyIsInR5cGUiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvY291cnNlI0NvdXJzZU9mZmVyaW5nIl0sInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJsYWJlbCI6IkxUSTEwMSJ9LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS90b29sX3BsYXRmb3JtIjp7Imd1aWQiOiJwb3QyOHlOcERTWnMxRnZNUWE0Mk1pUFdsUk9MUUNBZWR6UVg2TWMyOmNhbnZhcy1sbXMiLCJuYW1lIjoiVW5pY29uIiwidmVyc2lvbiI6ImNsb3VkIiwicHJvZHVjdF9mYW1pbHlfY29kZSI6ImNhbnZhcyIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319fSwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vbGF1bmNoX3ByZXNlbnRhdGlvbiI6eyJkb2N1bWVudF90YXJnZXQiOiJpZnJhbWUiLCJyZXR1cm5fdXJsIjoiaHR0cHM6Ly91bmljb24uaW5zdHJ1Y3R1cmUuY29tL2NvdXJzZXMvNDA5L2V4dGVybmFsX2NvbnRlbnQvc3VjY2Vzcy9leHRlcm5hbF90b29sX3JlZGlyZWN0IiwibG9jYWxlIjoiZW4iLCJ2YWxpZGF0aW9uX2NvbnRleHQiOm51bGwsImVycm9ycyI6eyJlcnJvcnMiOnt9fX0sImxvY2FsZSI6ImVuIiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcm9sZXMiOlsiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvaW5zdGl0dXRpb24vcGVyc29uI0FkbWluaXN0cmF0b3IiLCJodHRwOi8vcHVybC5pbXNnbG9iYWwub3JnL3ZvY2FiL2xpcy92Mi9pbnN0aXR1dGlvbi9wZXJzb24jSW5zdHJ1Y3RvciIsImh0dHA6Ly9wdXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNTdHVkZW50IiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lzdGVtL3BlcnNvbiNVc2VyIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2N1c3RvbSI6eyJzaXNpZCI6IjAwMzROMDAwMDJydlRZbSIsImR1ZV9hdCI6IiRDYW52YXMuYXNzaWdubWVudC5kdWVBdC5pc284NjAxIiwibG9ja19hdCI6IiRDYW52YXMuYXNzaWdubWVudC5sb2NrQXQuaXNvODYwMSIsInVubG9ja19hdCI6IiRDYW52YXMuYXNzaWdubWVudC51bmxvY2tBdC5pc284NjAxIiwiZ3JvdXBfbmFtZSI6IiRjb20uaW5zdHJ1Y3R1cmUuR3JvdXAubmFtZSIsImxtc19jb3Vyc2VfaWQiOiI0MDkiLCJjYW52YXNfdXNlcl9pZCI6IjE5NyIsImxtc19hY2NvdW50X2lkIjoiMSIsImNhbnZhc19sb2dpbl9pZCI6ImRkZWxibGFuY29AdW5pY29uLm5ldCIsImNhbnZhc191c2VyX25hbWUiOiJkZGVsYmxhbmNvQHVuaWNvbi5uZXQiLCJsbXNfYWNjb3VudF9uYW1lIjoiVW5pY29uIiwidG9vbF9wbGF0Zm9ybV91cmwiOiIkVG9vbFBsYXRmb3JtSW5zdGFuY2UudXJsIiwiY2FudmFzX3NlY3Rpb25faWRzIjoiMzY0LDM2OSIsInRvb2xfcGxhdGZvcm1fZ3VpZCI6IiRUb29sUGxhdGZvcm1JbnN0YW5jZS5ndWlkIiwidG9vbF9wbGF0Zm9ybV9uYW1lIjoiJFRvb2xQbGF0Zm9ybUluc3RhbmNlLm5hbWUiLCJjYW52YXNfY291cnNlX3N0YXJ0IjoiMjAyMC0wNS0yMVQxMjo1NTozMFoiLCJsbXNfcm9vdF9hY2NvdW50X2lkIjoiMSIsImNhbnZhc19hc3NpZ25tZW50X2lkIjoiJENhbnZhcy5hc3NpZ25tZW50LmlkIiwiY2FudmFzX3Jlc291cmNlX3R5cGUiOm51bGwsInRvb2xfcGxhdGZvcm1fdmVyc2lvbiI6IiRUb29sUGxhdGZvcm0udmVyc2lvbiIsInBlcnNvbl9hZGRyZXNzX2NvdW50cnkiOiIkUGVyc29uLmFkZHJlc3MuY291bnRyeSIsInBlcnNvbl9hZGRyZXNzX3RpbWV6b25lIjoiQW1lcmljYS9QaG9lbml4IiwidG9vbF9wbGF0Zm9ybV9kZXNjcmlwdGlvbiI6IiRUb29sUGxhdGZvcm1JbnN0YW5jZS5kZXNjcmlwdGlvbiIsInRvb2xfcGxhdGZvcm1fY29udGFjdF9lbWFpbCI6IiRUb29sUGxhdGZvcm1JbnN0YW5jZS5jb250YWN0RW1haWwiLCJ0b29sX3BsYXRmb3JtX3Byb2R1Y3RfZmFtaWx5X2NvZGUiOiIkVG9vbFBsYXRmb3JtLnByb2R1Y3RGYW1pbHlDb2RlIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTExX2xlZ2FjeV91c2VyX2lkIjoiNjUwM2Q2OGVhZDM3Yzk0Y2EwZDNmZjk0MzE4YTYwYjY3MGZhYmFkYSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2x0aTFwMSI6eyJ1c2VyX2lkIjoiNjUwM2Q2OGVhZDM3Yzk0Y2EwZDNmZjk0MzE4YTYwYjY3MGZhYmFkYSIsInZhbGlkYXRpb25fY29udGV4dCI6bnVsbCwiZXJyb3JzIjp7ImVycm9ycyI6e319LCJyZXNvdXJjZV9saW5rX2lkIjoiYmFmZGNlOTA3NzI5MWI3OTlmNTFkNWU4ZDY2OWNiZGY5MTkwZWM1NSJ9LCJlcnJvcnMiOnsiZXJyb3JzIjp7fX0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9jbGFpbS9lbmRwb2ludCI6eyJzY29wZSI6WyJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvbGluZWl0ZW0iLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvbGluZWl0ZW0ucmVhZG9ubHkiLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3Mvc2NvcGUvcmVzdWx0LnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Njb3JlIl0sImxpbmVpdGVtcyI6Imh0dHBzOi8vY2FudmFzLnVuaWNvbi5uZXQvYXBpL2x0aS9jb3Vyc2VzLzQwOS9saW5lX2l0ZW1zIiwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1ucnBzL2NsYWltL25hbWVzcm9sZXNlcnZpY2UiOnsiY29udGV4dF9tZW1iZXJzaGlwc191cmwiOiJodHRwczovL2NhbnZhcy51bmljb24ubmV0L2FwaS9sdGkvY291cnNlcy80MDkvbmFtZXNfYW5kX3JvbGVzIiwic2VydmljZV92ZXJzaW9ucyI6WyIyLjAiXSwidmFsaWRhdGlvbl9jb250ZXh0IjpudWxsLCJlcnJvcnMiOnsiZXJyb3JzIjp7fX19LCJodHRwczovL3d3dy5pbnN0cnVjdHVyZS5jb20vcGxhY2VtZW50IjpudWxsfQ.u0RnbYJ_iCEVTtk8ha85Hh1GpKvyqOV1EdyGhzYlfzde6lQOV9OJi-KltxaqkrcagU-b3e_n4QqoIwUo5lLHaNOjhjVJB0-uYaqOHt5g5pFvsv0JUMNEbBEaouQzJg6FM0VoFbMvRcQmVd0zswF1wmb2IUl5QJTqgHgjlcbxiMxE5sWDLA-0einnwEVMcZfrUfU-gtK9yO6ZqZx6pKXIeb6S5jozNMJQ5eBoQw4_E77DaZZXSFPiFs0XtjIOAHDbXoRIanYWXIrGeZBusnd2QFc2DAdSgZlBFdN1tIGMMUepcNS7a8-3nNVRKB8Y-h3SNk0XkW6KtQnRaY_oku2fjA";
    private static String HASH_STATE = "526ed651d74f86bc8ce665fa78c44512eaa5bd6892c29e5ad5977256611d756e";
    private static String NONCE = "cc536df6-cba1-4bb4-bd30-0bb79207e4fd";
    private static String TOKEN = "eyJraWQiOiJPV05LRVkiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJPdXIgb3duIHRvb2wiLCJzdWIiOiJPdXIgb3duIHRvb2wiLCJhdWQiOiJPdXIgb3duIHRvb2wiLCJleHAiOjE3MDg3Mjg2NjcsIm5iZiI6MTcwODcyNTA2NywiaWF0IjoxNzA4NzI1MDY3LCJleHBlY3RlZF9oYXNoIjoiZmZmNTY5YTNlMjQxZDdmOTFiZTg1Y2MzZDQwN2NlNzQwZGI2MjBlZDc5YTYwMjRlMjQ1ZmI3Mjg1YzRhYTYzMSJ9.md0NvQsf34j-ESo_PcZ2JI9oYxRlABzvX30SSnxbagEW4HVuKt5L6UjJkwIon-2gyr-_Fwfo6uImNXqoxzKZYq_iPuM4OyKJ2IkMCb_J1wF8T4kWjTamWEmU4eTyUlk68lIj4tul5nu2gMEixFtRXW2Ie5bki25xnVkTm5GFJrgOu9M2iSuuNuN8R7pBFX1JypKMjI6Qv679Q1w_XDRVql2IrDxpbH1_gcvC_OT08e_pY1iqiMifyWiAdAHQKZM99XUK9aTsPqXnJxhriq8PX_cB1UhiNE6uB5fPjREONmQtr_REPlas-xGyA2kzsbL6L2PuCub93RzTu5tEp2tYWw";
    @InjectMocks
    private LTI3Controller lti3Controller = new LTI3Controller();

    @InjectMocks
    private AllRepositories allRepositories;

    @MockBean
    private LTIJWTService ltijwtService;

    @MockBean
    private APIJWTService apijwtService;

    @MockBean
    private LtiLinkRepository ltiLinkRepository;

    @MockBean
    private LTIDataService ltiDataService;

    @MockBean
    private LtiContextRepository ltiContextRepository;

    @MockBean
    private NonceStateRepository nonceStateRepository;

    @MockBean
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private HttpServletRequest req;

    private Model model = new ExtendedModelMap();

    @Mock
    Jws<Claims> jwsClaims;

    @Mock
    Claims claims;

    @Mock
    LTI3Request lti3Request;

    @Mock
    LtiContextEntity ltiContextEntity;

    @Mock
    private PlatformDeployment platformDeployment1;

    private static MockedStatic<LTI3Request> lti3RequestMockedStatic;

    @Configuration
    static class ContextConfiguration {

    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        lti3RequestMockedStatic = Mockito.mockStatic(LTI3Request.class);
        when(req.getParameter("state")).thenReturn(HASH_STATE);
        when(req.getParameter("link")).thenReturn("https://tool.com/test");
        when(req.getParameter("id_token")).thenReturn(ID_TOKEN);
        when(req.getParameter("expected_state")).thenReturn(HASH_STATE);
        when(req.getParameter("expected_nonce")).thenReturn(NONCE);
        when(req.getParameter("nonce")).thenReturn(NONCE);
        when(req.getParameter("link")).thenReturn("");

        when(jwsClaims.getBody()).thenReturn(claims);
        when(ltijwtService.validateState(VALID_STATE)).thenReturn(jwsClaims);
        when(ltiDataService.getRepos()).thenReturn(allRepositories);
        lti3RequestMockedStatic.when(() -> LTI3Request.getInstance("https://tool.com/test")).thenReturn(lti3Request);
        lti3RequestMockedStatic.when(() -> LTI3Request.getInstance(null)).thenReturn(lti3Request);
    }

    @AfterEach
    public void close() {
        lti3RequestMockedStatic.close();
    }

    @Test
    public void testLTI3InvalidClientId() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(lti3Request.getAud()).thenReturn("bad-client-id");
            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody()).thenReturn(claims);
            when(req.getParameter("token")).thenReturn(TOKEN);
            String tohash = ID_TOKEN + HASH_STATE + NONCE;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            when(claims.get("expected_hash")).thenReturn(expected_hash);
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);
            String response = lti3Controller.lti3checked(req, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(response, TextConstants.LTI3ERROR);
            assertEquals(model.getAttribute(TextConstants.ERROR), "Invalid Client Id");
        } catch (DataServiceException | ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3InvalidDeploymentIdAndValidClientId() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
            String tohash = ID_TOKEN + HASH_STATE + NONCE;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            when(claims.get("expected_hash")).thenReturn(expected_hash);
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("bad-deployment-id");
            when(req.getParameter("token")).thenReturn(TOKEN);
            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody()).thenReturn(claims);
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);
            String response = lti3Controller.lti3checked(req, model);

            Mockito.verify(ltijwtService).validateNonceState(TOKEN);
            assertEquals(response, TextConstants.LTI3ERROR);
            assertEquals(model.getAttribute(TextConstants.ERROR), "Invalid Deployment Id");
        } catch (DataServiceException | ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");

            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("bad-deployment-id");
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);

            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody().get("expected_hash")).thenReturn(HASH_STATE);
            when(jwsClaims.getBody().get(eq("original_iss"), any())).thenReturn("iss-1");
            when(jwsClaims.getBody().get(eq("clientId"), any())).thenReturn("client-id-1");
            when(jwsClaims.getBody().get(eq("ltiDeploymentId"), any())).thenReturn("deployment-id-1");
            List<PlatformDeployment> mockPlatformDeploymentList = new ArrayList<>();
            PlatformDeployment mockPlatformDeployment = new PlatformDeployment();
            mockPlatformDeployment.setOidcEndpoint("mockOidcEndpoint");
            mockPlatformDeploymentList.add(mockPlatformDeployment);

            //when(ltiDataService.getRepos().platformDeploymentRepository.findByIssAndClientIdAndDeploymentId(iss, clientId, ltiDeploymentId))
            //        .thenReturn(mockPlatformDeploymentList);


            when(allRepositories.platformDeploymentRepository.findByIssAndClientIdAndDeploymentId("iss-1", "client-id-1", "deployment-id-1")).thenReturn(mockPlatformDeploymentList);
            when(platformDeployment1.getOidcEndpoint()).thenReturn("https://tool.net/oidc");
            String response = lti3Controller.lti3(req, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            assertEquals(response, "nonceStateCheck");
        } catch (DataServiceException | ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3DemoModeOff() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");
            String tohash = ID_TOKEN + HASH_STATE + NONCE;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            when(claims.get("expected_hash")).thenReturn(expected_hash);
            when(req.getParameter("token")).thenReturn(TOKEN);
            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody()).thenReturn(claims);
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);

            String response = lti3Controller.lti3checked(req, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            Mockito.verify(apijwtService).buildJwt(eq(true), eq(lti3Request));
            assertTrue(response.contains("redirect:/app/app.html?token="));

        } catch (DataServiceException | ConnectionException | GeneralSecurityException | IOException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3DemoModeOffWithoutClientIdOrDeploymentIdInState() {
        try {
            when(claims.get("clientId")).thenReturn(null);
            when(claims.get("ltiDeploymentId")).thenReturn(null);
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(ltiDataService.getDemoMode()).thenReturn(false);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");
            String tohash = ID_TOKEN + HASH_STATE + NONCE;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            when(claims.get("expected_hash")).thenReturn(expected_hash);
            when(req.getParameter("token")).thenReturn(TOKEN);
            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody()).thenReturn(claims);
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);
            when(lti3Request.getClaims()).thenReturn(claims);

            String response = lti3Controller.lti3checked(req, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            Mockito.verify(apijwtService).buildJwt(eq(true), eq(lti3Request));
            assertTrue(response.contains("redirect:/app/app.html?token="));

        } catch (DataServiceException | ConnectionException | GeneralSecurityException | IOException e) {
            fail("Exception should not be thrown.");
        }
    }

    @Test
    public void testLTI3DemoModeOn() {
        try {
            when(claims.get("clientId")).thenReturn("client-id-1");
            when(claims.get("ltiDeploymentId")).thenReturn("deployment-id-1");
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(lti3Request.getContext()).thenReturn(ltiContextEntity);
            when(lti3Request.getLtiMessageType()).thenReturn(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
            when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://tool.com/test");
            when(ltiDataService.getDemoMode()).thenReturn(true);
            String tohash = ID_TOKEN + HASH_STATE + NONCE;
            String expected_hash = Hashing.sha256()
                    .hashString(tohash, StandardCharsets.UTF_8)
                    .toString();
            when(claims.get("expected_hash")).thenReturn(expected_hash);
            when(req.getParameter("token")).thenReturn(TOKEN);
            when(ltijwtService.validateNonceState(any(String.class))).thenReturn(jwsClaims);
            when(jwsClaims.getBody()).thenReturn(claims);
            NonceState nonceState = new NonceState(NONCE, HASH_STATE, VALID_STATE, "_parent");
            when(nonceStateRepository.findByStateHash(any(String.class))).thenReturn(nonceState);

            String finalResponse = lti3Controller.lti3checked(req, model);

            Mockito.verify(ltijwtService).validateState(VALID_STATE);
            Mockito.verify(ltiDataService).getDemoMode();
            assertEquals(finalResponse, "lti3Result");

        } catch (DataServiceException | ConnectionException e) {
            fail("Exception should not be thrown.");
        }
    }
}