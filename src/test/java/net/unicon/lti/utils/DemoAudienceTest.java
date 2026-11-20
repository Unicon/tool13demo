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
package net.unicon.lti.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DemoAudienceTest {

    @Test
    public void normalizeAcceptsKnownValuesOnly() {
        assertEquals(DemoAudience.CLIENT, DemoAudience.normalize("client"));
        assertEquals(DemoAudience.TECHNICAL, DemoAudience.normalize(" Technical "));
        assertNull(DemoAudience.normalize("everyone"));
        assertNull(DemoAudience.normalize(""));
        assertNull(DemoAudience.normalize(null));
    }

    @Test
    public void isClient() {
        assertTrue(DemoAudience.isClient("client"));
        assertFalse(DemoAudience.isClient("technical"));
        assertFalse(DemoAudience.isClient(null));
    }

    @Test
    public void readsAudienceFromTargetLinkUri() {
        assertEquals(DemoAudience.CLIENT, DemoAudience.fromTargetLinkUri("https://tool.example/lti3?demo_audience=client"));
        assertEquals(DemoAudience.TECHNICAL, DemoAudience.fromTargetLinkUri("https://tool.example/lti3?link=bio-1-1&demo_audience=technical"));
        assertNull(DemoAudience.fromTargetLinkUri("https://tool.example/lti3"));
        assertNull(DemoAudience.fromTargetLinkUri(null));
    }

    @Test
    public void readsLinkIdWithoutPickingUpOtherParameters() {
        assertEquals("bio-1-1", DemoAudience.linkFromTargetLinkUri("https://tool.example/lti3?link=bio-1-1"));
        assertEquals("bio-1-1", DemoAudience.linkFromTargetLinkUri("https://tool.example/lti3?link=bio-1-1&demo_audience=client"));
        assertEquals("bio-1-1", DemoAudience.linkFromTargetLinkUri("https://tool.example/lti3?demo_audience=client&link=bio-1-1"));
        // Previously a launch without ?link= produced a garbage id from substring(); now it is simply absent.
        assertNull(DemoAudience.linkFromTargetLinkUri("https://tool.example/lti3"));
        assertNull(DemoAudience.linkFromTargetLinkUri("https://tool.example/lti3?link="));
    }
}
