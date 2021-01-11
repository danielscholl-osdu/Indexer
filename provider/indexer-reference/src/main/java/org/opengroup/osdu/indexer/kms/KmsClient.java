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

package org.opengroup.osdu.indexer.kms;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import com.google.api.services.cloudkms.v1.model.DecryptRequest;
import com.google.api.services.cloudkms.v1.model.DecryptResponse;
import com.google.api.services.cloudkms.v1.model.EncryptRequest;
import com.google.api.services.cloudkms.v1.model.EncryptResponse;
import org.opengroup.osdu.core.common.provider.interfaces.IKmsClient;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequestScope
public class KmsClient implements IKmsClient {

    @Value("${GOOGLE_CLOUD_PROJECT}")
    private String GOOGLE_CLOUD_PROJECT;

    @Value("${KMS_KEY}")
    private String KMS_KEY;

    @Value("${KEY_RING}")
    private String KEY_RING;

    private static final String KEY_NAME = "projects/%s/locations/global/keyRings/%s/cryptoKeys/%s";

    /**
     * Encrypts the given plaintext using the specified crypto key.
     * Google KMS automatically uses the new primary key version to encrypt data, so this could be directly used for key rotation
     */
    public String encryptString(String textToBeEncrypted) throws IOException {
        Preconditions.checkNotNullOrEmpty(textToBeEncrypted, "textToBeEncrypted cannot be null");

        byte[] plaintext = textToBeEncrypted.getBytes(StandardCharsets.UTF_8);
        String resourceName = String.format(KEY_NAME, GOOGLE_CLOUD_PROJECT, KEY_RING, KMS_KEY);
        CloudKMS kms = createAuthorizedClient();
        EncryptRequest request = new EncryptRequest().encodePlaintext(plaintext);
        EncryptResponse response = kms.projects().locations().keyRings().cryptoKeys()
                .encrypt(resourceName, request)
                .execute();
        return response.getCiphertext();
    }

    /**
     * Decrypts the provided ciphertext with the specified crypto key.
     * Google KMS automatically uses the correct key version to decrypt data, as long as the key version is not disabled
     */
    public String decryptString(String textToBeDecrypted) throws IOException {
        Preconditions.checkNotNullOrEmpty(textToBeDecrypted, "textToBeDecrypted cannot be null");

        CloudKMS kms = createAuthorizedClient();
        String cryptoKeyName = String.format(KEY_NAME, GOOGLE_CLOUD_PROJECT, KEY_RING, KMS_KEY);
        DecryptRequest request = new DecryptRequest().setCiphertext(textToBeDecrypted);
        DecryptResponse response = kms.projects().locations().keyRings().cryptoKeys()
                .decrypt(cryptoKeyName, request)
                .execute();
        return new String(response.decodePlaintext(), StandardCharsets.UTF_8).trim();
    }

    /**
     * Creates an authorized CloudKMS client service using Application Default Credentials.
     *
     * @return an authorized CloudKMS client
     * @throws IOException if there's an error getting the default credentials.
     */
    private CloudKMS createAuthorizedClient() throws IOException {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = GoogleCredential.getApplicationDefault();
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(CloudKMSScopes.all());
        }
        return new CloudKMS.Builder(transport, jsonFactory, credential)
                .setApplicationName("CloudKMS snippets")
                .build();
    }
}