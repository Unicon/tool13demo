package net.unicon.lti.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainUtilsTest {

    @Test
    public void testExtractDomain() {
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-sunymar.one.luminlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://home-sunymar.one.luminlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://home-sunymar.preprod-exemplar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-sunymar.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://home.one.luminlearning.com"));
        assertNull(DomainUtils.extractDomain("https://lti.one.luminlearning.com"));
        assertNull(DomainUtils.extractDomain("https://goldilocks.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti-stage.preprod.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://valkyrie-staging.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti-staging.preprod-exemplar.ludev.team"));
    }

    @Test
    public void testInsertDomain() {
        assertEquals("https://home-sunymar.one.luminlearning.com", DomainUtils.insertDomain("sunymar", "https://home.one.luminlearning.com", "home-"));
        assertEquals("https://lti-sunymar.one.luminlearning.com", DomainUtils.insertDomain("sunymar", "https://home.one.luminlearning.com", "lti-"));
        assertEquals("https://home-sunymar.luminlearning.com", DomainUtils.insertDomain("sunymar", "https://goldilocks.luminlearning.com", "home-"));
        assertEquals("https://lti-sunymar.luminlearning.com", DomainUtils.insertDomain("sunymar", "https://goldilocks.luminlearning.com", "lti-"));
        assertEquals("https://home-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://valkyrie-staging.preprod-exemplar.ludev.team", "home-"));
        assertEquals("https://lti-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-staging.preprod-exemplar.ludev.team", "lti-"));
        assertEquals("https://home-sunymar.ludev.team", DomainUtils.insertDomain("sunymar", "https://goldilocks.ludev.team", "home-"));
        assertEquals("https://lti-sunymar.preprod.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-stage.preprod.ludev.team", "lti-"));


    }

}