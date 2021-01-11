/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.cache;

import com.google.auth.oauth2.AccessToken;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatastoreCredentialCache extends RedisCache<String, AccessToken> {

	// Datastore credentials are only valid for 1hr, release the key 2 minutes before the expiration
	public DatastoreCredentialCache(@Value("${REDIS_SEARCH_HOST}") final String REDIS_SEARCH_HOST, @Value("${REDIS_SEARCH_PORT}") final String REDIS_SEARCH_PORT) {
		super(REDIS_SEARCH_HOST, Integer.parseInt(REDIS_SEARCH_PORT), 58 * 60, String.class, AccessToken.class);
	}
}