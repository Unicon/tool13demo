/**
 * Copyright 2021 Unicon (R)
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
package net.unicon.lti.model.oauth2;

import com.google.common.collect.ImmutableList;
import net.unicon.lti.utils.LtiStrings;

import java.util.List;

// do this as a class instead of an enum so its easier to reuse values
// in annotations, should it ever come to that (as is typical in controller-based auth)
public abstract class Roles {

    public static final String GENERAL = LtiStrings.LTI_ROLE_GENERAL;
    public static final String LEARNER = LtiStrings.LTI_ROLE_LEARNER;
    public static final String INSTRUCTOR = LtiStrings.LTI_ROLE_INSTRUCTOR;
    public static final String MEMBERSHIP_INSTRUCTOR = LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
    public static final String MEMBERSHIP_LEARNER = LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER;
    public static final String ADMIN = LtiStrings.LTI_ROLE_ADMIN;


    public static final List<String> GENERAL_ROLE_LIST = ImmutableList.of(GENERAL);
    public static final List<String> STUDENT_ROLE_LIST = ImmutableList.of(LEARNER, MEMBERSHIP_LEARNER);
    public static final List<String> INSTRUCTOR_ROLE_LIST = ImmutableList.of(INSTRUCTOR, MEMBERSHIP_INSTRUCTOR);
    public static final List<String> ADMIN_ROLE_LIST = ImmutableList.of(ADMIN);
}
