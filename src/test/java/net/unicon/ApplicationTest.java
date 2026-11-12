package net.unicon;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApplicationTest {

    @Test
    public void testMainRunsTheSpringApplication() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            Application.main(args);

            springApplication.verify(() -> SpringApplication.run(Application.class, args));
        }
    }

    @Test
    public void testSecurityAutoConfigurationIsExcluded() {
        SpringBootApplication annotation = Application.class.getAnnotation(SpringBootApplication.class);

        assertNotNull(new Application());
        assertEquals(List.of(SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class), List.of(annotation.exclude()));
    }
}
