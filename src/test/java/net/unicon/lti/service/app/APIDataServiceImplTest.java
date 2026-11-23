package net.unicon.lti.service.app;

import net.unicon.lti.model.ApiOneUseToken;
import net.unicon.lti.repository.AllRepositories;
import net.unicon.lti.repository.ApiOneUseTokenRepository;
import net.unicon.lti.service.app.impl.APIDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class APIDataServiceImplTest {

    @Mock
    private ApiOneUseTokenRepository apiOneUseTokenRepository;

    private AllRepositories repos;
    private APIDataServiceImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repos = new AllRepositories(null, null, null, null, null, null, null, null, apiOneUseTokenRepository, null);
        service = new APIDataServiceImpl(repos);
    }

    @Test
    public void exposesTheRepositories() {
        assertSame(repos, service.getRepos());
    }

    @Test
    public void addOneUseTokenStoresTheToken() {
        service.addOneUseToken("token-1");
        ArgumentCaptor<ApiOneUseToken> captor = ArgumentCaptor.forClass(ApiOneUseToken.class);
        verify(apiOneUseTokenRepository).save(captor.capture());
        assertEquals("token-1", captor.getValue().getToken());
    }

    @Test
    public void findAndDeleteOneUseTokenDeletesAKnownToken() {
        ApiOneUseToken token = new ApiOneUseToken("token-1");
        when(apiOneUseTokenRepository.findByToken("token-1")).thenReturn(token);
        assertTrue(service.findAndDeleteOneUseToken("token-1"));
        verify(apiOneUseTokenRepository).delete(token);
    }

    @Test
    public void findAndDeleteOneUseTokenReportsAnUnknownToken() {
        assertFalse(service.findAndDeleteOneUseToken("missing"));
        verify(apiOneUseTokenRepository, never()).delete(any());
    }

    @Test
    public void cleanOldTokensDeletesTokensOlderThanADay() {
        long before = System.currentTimeMillis();
        service.cleanOldTokens();
        long after = System.currentTimeMillis();
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(apiOneUseTokenRepository).deleteByCreatedAtBefore(captor.capture());
        long day = 24L * 60 * 60 * 1000;
        assertTrue(captor.getValue().getTime() >= before - day && captor.getValue().getTime() <= after - day);
    }
}
