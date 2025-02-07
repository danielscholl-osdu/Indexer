### Running E2E Tests

You will need to have the following environment variables defined.

| name                                | value                                  | description                              | sensitive? | source |
|-------------------------------------|----------------------------------------|------------------------------------------|------------|--------|
| `HOST`                              | eg. `https://osdu.com`                 | -                                        | no         | -      |
| `INDEXER_HOST`                      | eg. `https://osdu.com/api/indexer/v2/` | -                                        | no         | -      |
| `ELASTIC_HOST`                      | eg. `elastic.core.com`                 | -                                        | no         | -      |
| `SEARCH_HOST`                       | eg. `https://osdu.com/api/search/v2/`  | -                                        | no         | -      |
| `STORAGE_HOST`                      | eg. `https://osdu.com/api/storage/v2/` | -                                        | no         | -      |
| `SECURITY_HTTPS_CERTIFICATE_TRUST`  | eg. `true` / `false`                   | -                                        | no         | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | eg. `osdu`                             | OSDU tenant used for testing             | no         | -      |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | eg. `osdu`                             | Alternative OSDU tenant used for testing | no         | -      |
| `ENTITLEMENTS_DOMAIN`               | eg. `group`                            | -                                        | no         | -      |
| `LEGAL_TAG`                         | eg. `osdu-legaltag`                    | -                                        | no         | -      |
| `OTHER_RELEVANT_DATA_COUNTRIES`     | eg. `US`                               | -                                        | no         | -      |

Authentication can be provided as OIDC config:

| name                                            | value                                   | description             | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|-------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | ROOT_USER Client Id     | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | ROOT_USER Client Secret | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID Provider Url     | yes        | -      |
| `ELASTIC_USER_NAME`                             | `********`                              | Elastic User            | ?          | -      |
| `ELASTIC_PASSWORD`                              | `********`                              | Elastic User Password   | yes        | -      |
| `ELASTIC_PORT`                                  | `********`                              | Elastic Request Port    | ?          | -      |
Or tokens can be used directly from env variables:

| name              | value      | description           | sensitive? | source |
|-------------------|------------|-----------------------|------------|--------|
| `ROOT_USER_TOKEN` | `********` | PRIVILEGED_USER Token | yes        | -      |

#### Entitlements configuration for Integration Accounts

| INTEGRATION_TESTER            | NO_DATA_ACCESS_TESTER |
|-------------------------------|-----------------------|
| users                         |                       |
| users.datalake.ops            |                       |
| service.storage.creator       |                       |
| service.entitlements.user     |                       |
| service.search.user           |                       |
| service.search.admin          |                       |
| data.test1                    |                       |
| data.integration.test         |                       |
| users@{tenant1}@{groupId}.com |                       |

Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd indexer-acceptance-test && mvn clean verify)
 ```

## License

Copyright © Google LLC

Copyright © EPAM Systems

Copyright © ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
