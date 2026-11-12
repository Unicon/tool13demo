package net.unicon.lti.service.lti;

import net.unicon.lti.model.lti.dto.NonceState;
import net.unicon.lti.repository.NonceStateRepository;
import net.unicon.lti.service.lti.impl.NonceStateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NonceStateServiceImplTest {

    @Mock
    private NonceStateRepository nonceStateRepository;

    private NonceStateServiceImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NonceStateServiceImpl(nonceStateRepository);
    }

    @Test
    public void deleteOldNoncesDeletesNoncesOlderThanAnHour() {
        long before = System.currentTimeMillis();
        service.deleteOldNonces();
        long after = System.currentTimeMillis();
        ArgumentCaptor<Date> captor = ArgumentCaptor.forClass(Date.class);
        verify(nonceStateRepository).deleteByCreatedAtBefore(captor.capture());
        long hour = 60L * 60 * 1000;
        assertTrue(captor.getValue().getTime() >= before - hour && captor.getValue().getTime() <= after - hour);
    }

    @Test
    public void getNonceLooksUpTheNonce() {
        NonceState nonceState = new NonceState("nonce-1", "hash", "state", "_parent");
        when(nonceStateRepository.findByNonce("nonce-1")).thenReturn(nonceState);
        assertSame(nonceState, service.getNonce("nonce-1"));
    }

    @Test
    public void deleteNonceDeletesTheNonce() {
        service.deleteNonce("nonce-1");
        verify(nonceStateRepository).deleteByNonce("nonce-1");
    }
}
