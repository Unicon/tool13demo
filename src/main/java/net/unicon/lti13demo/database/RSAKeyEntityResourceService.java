/**
 * Copyright 2019 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.unicon.lti13demo.database;

import net.unicon.lti13demo.model.RSAKeyEntity;
import net.unicon.lti13demo.service.ResourceService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RSAKeyEntityResourceService implements ResourceService<RSAKeyEntity> {

	static String PLATFORM_RSA_KEYS_RESOURCE = "classpath:data/rsa_keys";
	
	@Override
	public String getDirectoryPath() {
		return PLATFORM_RSA_KEYS_RESOURCE;
	}

	@Override
	public void setDefaults() throws IOException {
	}

}
