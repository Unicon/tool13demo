package net.unicon.lti.service.app;

import net.unicon.lti.service.app.impl.ScheduledServiceImpl;
import net.unicon.lti.service.lti.NonceStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ScheduledServiceImplTest {

    @Mock
    private APIDataService apiDataService;

    @Mock
    private NonceStateService nonceStateService;

    private ScheduledServiceImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ScheduledServiceImpl(apiDataService, nonceStateService);
    }

    @Test
    public void deleteOldTokensCleansTheTokens() {
        service.deleteOldTokens();
        verify(apiDataService).cleanOldTokens();
        verifyNoInteractions(nonceStateService);
    }

    @Test
    public void deleteOldNoncesCleansTheNonces() {
        service.deleteOldNonces();
        verify(nonceStateService).deleteOldNonces();
        verifyNoInteractions(apiDataService);
    }
}
