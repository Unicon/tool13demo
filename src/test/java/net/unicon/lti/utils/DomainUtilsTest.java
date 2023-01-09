package net.unicon.lti.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainUtilsTest {

    @Test
    public void testExtractDomain() {
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-sunymar.one.lumenlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-sunymar.one.lumenlearning.com/any/extra/path?whatever=extra"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://home-sunymar.one.lumenlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-sunymar.lumenlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://goldilocks-sunymar.lumenlearning.com"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-staging-sunymar.preprod-exemplar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://valkyrie-staging-sunymar.preprod-exemplar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-stage-sunymar.preprod.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://goldilocks-sunymar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-dev-sunymar.preprod-exemplar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://valkyrie-dev-sunymar.preprod-exemplar.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://lti-dev-sunymar.preprod.ludev.team"));
        assertEquals("sunymar", DomainUtils.extractDomain("https://goldilocks-dev-sunymar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti.one.lumenlearning.com"));
        assertNull(DomainUtils.extractDomain("https://home.one.lumenlearning.com"));
        assertNull(DomainUtils.extractDomain("https://lti.lumenlearning.com"));
        assertNull(DomainUtils.extractDomain("https://goldilocks.lumenlearning.com"));
        assertNull(DomainUtils.extractDomain("https://lti-staging.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://valkyrie-staging.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti-stage.preprod.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://goldilocks.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti-dev.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://valkyrie-dev.preprod-exemplar.ludev.team"));
        assertNull(DomainUtils.extractDomain("https://lti-dev.preprod.ludev.team\""));
        assertNull(DomainUtils.extractDomain("https://goldilocks-dev.ludev.team"));
    }

    @Test
    public void testInsertDomain() {

        //    Prod Valkyrie/Harmony/One:
        //    Default application.url (middleware url): https://lti.one.lumenlearning.com
        //    Modified application.url: https://lti-sunymar.one.lumenlearning.com
        //    Default domain.url (Valkyrie/Harmony/One url): https://home.one.lumenlearning.com
        //    Modified domain.url: https://home-sunymar.one.lumenlearning.com

        assertEquals("https://lti-sunymar.one.lumenlearning.com", DomainUtils.insertDomain("sunymar", "https://lti.one.lumenlearning.com"));
        assertEquals("https://home-sunymar.one.lumenlearning.com", DomainUtils.insertDomain("sunymar", "https://home.one.lumenlearning.com"));

        //    Prod Goldilocks/Waymaker:
        //    Default application.url (middleware url): https://lti.lumenlearning.com
        //    Modified application.url: https://lti-sunymar.lumenlearning.com
        //    Default domain.url (Goldilocks/Waymaker url): https://goldilocks.lumenlearning.com
        //    Modified domain.url: https://goldilocks-sunymar.lumenlearning.com

        assertEquals("https://lti-sunymar.lumenlearning.com", DomainUtils.insertDomain("sunymar", "https://lti.lumenlearning.com"));
        assertEquals("https://goldilocks-sunymar.lumenlearning.com", DomainUtils.insertDomain("sunymar", "https://goldilocks.lumenlearning.com"));

        //    Staging Valkyrie/Harmony/One:
        //    Default application.url (middleware url): https://lti-staging.preprod-exemplar.ludev.team
        //    Modified application.url: https://lti-staging-sunymar.preprod-exemplar.ludev.team
        //    Default domain.url (Valkyrie/Harmony/One url): https://valkyrie-staging.preprod-exemplar.ludev.team
        //    Modified domain.url: https://valkyrie-staging-sunymar.preprod-exemplar.ludev.team

        assertEquals("https://lti-staging-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-staging.preprod-exemplar.ludev.team"));
        assertEquals("https://valkyrie-staging-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://valkyrie-staging.preprod-exemplar.ludev.team"));

        //    Staging Goldilocks/Waymaker:
        //    Default application.url (middleware url): https://lti-stage.preprod.ludev.team
        //    Modified application.url: https://lti-stage-sunymar.preprod.ludev.team
        //    Default domain.url (Goldilocks/Waymaker url): https://goldilocks.ludev.team
        //    Modified domain.url: https://goldilocks-sunymar.ludev.team

        assertEquals("https://lti-stage-sunymar.preprod.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-stage.preprod.ludev.team"));
        assertEquals("https://goldilocks-sunymar.ludev.team", DomainUtils.insertDomain("sunymar", "https://goldilocks.ludev.team"));

        //    Dev Valkyrie/Harmony/One:
        //    Default application.url (middleware url): https://lti-dev.preprod-exemplar.ludev.team
        //    Modified application.url: https://lti-dev-sunymar.preprod-exemplar.ludev.team
        //    Default domain.url (Valkyrie/Harmony/One url): https://valkyrie-dev.preprod-exemplar.ludev.team
        //    Modified domain.url: https://valkyrie-dev-sunymar.preprod-exemplar.ludev.team

        assertEquals("https://lti-dev-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-dev.preprod-exemplar.ludev.team"));
        assertEquals("https://valkyrie-dev-sunymar.preprod-exemplar.ludev.team", DomainUtils.insertDomain("sunymar", "https://valkyrie-dev.preprod-exemplar.ludev.team"));

        //    Dev Goldilocks/Waymaker:
        //    Default application.url (middleware url): https://lti-dev.preprod.ludev.team
        //    Modified application.url: https://lti-dev-sunymar.preprod.ludev.team
        //    Default domain.url (Goldilocks/Waymaker url): https://goldilocks-dev.ludev.team
        //    Modified domain.url: https://goldilocks-dev-sunymar.ludev.team

        assertEquals("https://lti-dev-sunymar.preprod.ludev.team", DomainUtils.insertDomain("sunymar", "https://lti-dev.preprod.ludev.team"));
        assertEquals("https://goldilocks-dev-sunymar.ludev.team", DomainUtils.insertDomain("sunymar", "https://goldilocks-dev.ludev.team"));
    }





}