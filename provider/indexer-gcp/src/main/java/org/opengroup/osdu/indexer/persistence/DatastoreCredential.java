// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.persistence;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.SignJwt;
import com.google.api.services.iam.v1.model.SignJwtRequest;
import com.google.api.services.iam.v1.model.SignJwtResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.time.DateUtils;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.indexer.cache.DatastoreCredentialCache;

import java.util.Date;

public class DatastoreCredential extends GoogleCredentials {

	private static final long serialVersionUID = 8344377091688956815L;
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private Iam iam;

	private final TenantInfo tenant;
	private final DatastoreCredentialCache cache;

	protected DatastoreCredential(TenantInfo tenant, DatastoreCredentialCache cache) {
		this.tenant = tenant;
		this.cache = cache;
	}

	@Override
	public AccessToken refreshAccessToken() {

		String cacheKey = this.getCacheKey();

		AccessToken accessToken = this.cache.get(cacheKey);

		if (accessToken != null) {
			return accessToken;
		}

		try {
			SignJwtRequest signJwtRequest = new SignJwtRequest();
			signJwtRequest.setPayload(this.getPayload());

			String serviceAccountName = String.format("projects/-/serviceAccounts/%s", this.tenant.getServiceAccount());

			SignJwt signJwt = this.getIam().projects().serviceAccounts().signJwt(serviceAccountName, signJwtRequest);

			SignJwtResponse signJwtResponse = signJwt.execute();
			String signedJwt = signJwtResponse.getSignedJwt();

			accessToken = new AccessToken(signedJwt, DateUtils.addSeconds(new Date(), 3600));

			this.cache.put(cacheKey, accessToken);

			return accessToken;
		} catch (Exception e) {
			throw new RuntimeException("Error creating datastore credential", e);
		}
	}

	private String getPayload() {
		JsonObject payload = new JsonObject();
		payload.addProperty("iss", this.tenant.getServiceAccount());
		payload.addProperty("sub", this.tenant.getServiceAccount());
		payload.addProperty("aud", "https://datastore.googleapis.com/google.datastore.v1.Datastore");
		payload.addProperty("iat", System.currentTimeMillis() / 1000);

		return payload.toString();
	}

	protected void setIam(Iam iam) {
		this.iam = iam;
	}

	private Iam getIam() throws Exception {
		if (this.iam == null) {

			Iam.Builder builder = new Iam.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
					GoogleCredential.getApplicationDefault()).setApplicationName("Search Service");

			this.iam = builder.build();
		}
		return this.iam;
	}

	private String getCacheKey() {
		return Crc32c.hashToBase64EncodedString(String.format("datastoreCredential:%s", this.tenant.getName()));
	}
}