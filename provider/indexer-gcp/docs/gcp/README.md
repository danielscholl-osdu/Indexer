## Service Configuration for GCP

## Environment variables:

Define the following environment variables.

Must have:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `SPRING_PROFILES_ACTIVE` | ex `gcp` | Spring profile that activate default configuration for GCP environment | false | - |

Defined in default application property file but possible to override:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `service` | Logging prefix | no | - |
| `LOG_LEVEL` | `****` | Logging level | no | - |
| `SECURITY_HTTPS_CERTIFICATE_TRUST` | ex `false` | Elastic client connection uses TrustSelfSignedStrategy(), if it is 'true' | false | output of infrastructure deployment |
| `REDIS_SEARCH_HOST` | ex `127.0.0.1` | Redis host for search | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_SEARCH_PORT` | ex `6379` | Redis host for search | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_GROUP_HOST` | ex `127.0.0.1` | Redis host for groups | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_GROUP_PORT` | ex `6379` | Redis host for search | no | https://console.cloud.google.com/memorystore/redis/instances |
| `PARTITION_HOST` | ex `https://partition.com` | Partition host | no | output of infrastructure deployment |
| `ENTITLEMENTS_HOST` | ex `https://entitlements.com` | Entitlements host | no | output of infrastructure deployment |
| `STORAGE_HOST` | ex `https://storage.com` | Storage host | no | output of infrastructure deployment |
| `INDEXER_QUEUE_HOST` | ex `http://indexer-queue/api/indexer-queue/v1/_dps/task-handlers/enqueue` | Indexer-Queue host endpoint used for reprocessing tasks | no | output of infrastructure deployment |
| `SCHEMA_BASE_HOST` | ex `https://schema.com` | Schema service host | no | output of infrastructure deployment |
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |

These variables define service behavior, and are used to switch between `anthos` or `gcp` environments, their overriding and usage in mixed mode was not tested.
Usage of spring profiles is preferred.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `PARTITION_AUTH_ENABLED` | ex `true` or `false` | Disable or enable auth token provisioning for requests to Partition service | no | - |
| `OQMDRIVER` | `rabbitmq` or `pubsub` | Oqm driver mode that defines which message broker will be used | no | - |
| `SERVICE_TOKEN_PROVIDER` | `GCP` or `OPENID` |Service account token provider, `GCP` means use Google service account `OPEIND` means use OpenId provider like `Keycloak` | no | - |

## Pubsub configuration:

At Pubsub should be created topic with name:

**name:** `indexing-progress`

## Elasticsearch configuration

### Properties set in Partition service:

**prefix:** `elasticsearch`

It can be overridden by:

- through the Spring Boot property `elastic-search-properties-prefix`
- environment variable `ELASTIC_SEARCH_PROPERTIES_PREFIX`

**Propertyset:**

| Property | Description |
| --- | --- |
| elasticsearch.host | server URL |
| elasticsearch.port | server port |
| elasticsearch.configuration | username and password |

<details><summary>Example of a definition for a single tenant</summary></details>

```

curl -L -X PATCH 'http://partition.com/api/partition/v1/partitions/opendes' -H 'data-partition-id: opendes' -H 'Authorization: Bearer ...' -H 'Content-Type: application/json' --data-raw '{
  "properties": {
    "elasticsearch.host": {
      "sensitive": false,
      "value": "elastic.us-central1.gcp.cloud.es.io"
    },
    "elasticsearch.port": {
      "sensitive": false,
      "value": "9243"
    },
    "elasticsearch.configuration": {
      "sensitive": true,
      "value": "elasticuser:elasticpassword"
    }
  }
}'

```

## Google cloud service account configuration :
TBD

| Required roles |
| ---    |
| - |
