## Indexer service

### Table of contents <a name="TOC"></a>
- [Indexer service](#indexer-service)
- [Get indexing status <a name="get-indexing-status"></a>](#get-indexing-status)
- [Reindex <a name="reindex"></a>](#reindex)
- [Schema Service adoption <a name="schema-service-adoption"></a>](#schema-service-adoption)
  - [R3 Schema Support <a name="r3-schema-support"></a>](#r3-schema-support)
- [Version info endpoint](#version-info-endpoint)


##Introduction <a name="introduction"></a>
The Indexer API provides a mechanism for indexing documents that contain structured or unstructured data. Documents and indices are saved in a separate persistent store optimized for search operations. The indexer API can index any number of documents.

The indexer is indexes attributes defined in the schema. Schema can be created at the time of record ingestion in Data Ecosystem via Storage Service. The Indexer service also adds number of Data Ecosystem meta attributes such as id, kind, parent, acl, namespace, type, version, legaltags, index to each record at the time of indexing.

##Indexer API access <a name="indexer-api-access"></a>

* Required roles

   Indexer service requires that users (and service accounts) have dedicated roles in order to use it. The following roles should be created and assigned using the entitlements service:
   
   __users.OSDU.viewers__

   __users.OSDU.editors__
   
   __users.OSDU.admin__
   
* Required headers

  The Data Ecosystem stores data in different data partitions depending on the different accounts in the OSDU system.

  A user may belong to many partitions in OSDU e.g. a OSDU user may belong to both the OSDU partition and a customer's partition. When a user logs into the OSDU portal they choose which data partition they currently want to be active.

  When using the Indexer APIs you need to specify which data partition they currently have active and send it in the OSDU-Data-Partition-Id header. e.g.
  ```
  OSDU-Data-Partition-Id: OSDU
  ```
  The correct values can be obtained from CFS services.

  We use this value to work out which partition to use. There is also a special data partition known as common
  ```
  OSDU-Data-Partition-Id: common
  ```
  This has all public data in the Data Ecosystem. Users always have access to this as well as their current active data partition.

  You should also send a correlation id as a header so that a single request can be tracked throughout all the services it passes through. This can be a GUID on the header with a key
  ```
  OSDU-Correlation-Id: 1e0fef08-22fd-49b1-a5cc-dffa21bc0b70
  ```
  If you are the service initiating the request you should generate the id, otherwise you should just forward it on in the request.
  
[Back to table of contents](#TOC)

## Get indexing status <a name="get-indexing-status"></a>

Indexer service adds internal metadata to each record which registers the status of the indexing. The meta data includes the status and the last indexing date and time. This additional meta block helps to see the details of indexing. The format of the index meta block is as follows:

```json
{
  "index": {
      "trace": [
          String,
          String
      ],
      "statusCode": Integer,
      "lastUpdateTime": Datetime
  }
}
```
Example:
```json
{
    "results": [
        {
            "index": {
                "trace": [
                    "datetime parsing error: unknown format for attribute: endDate | value: 9000-01-01T00:00:00.0000000",
                    "datetime parsing error: unknown format for attribute: startDate | value: 1990-01-01T00:00:00.0000000"
                ],
                "statusCode": 400,
                "lastUpdateTime": "2018-11-16T01:44:08.687Z"
            }
        }
    ],
    "totalCount": 31895
} 
```

Details of the index block:
1) trace: This field collects all the issues related to the indexing and concatinates using '|'. This is a String field.
2) statusCode: This field determines the category of the error. This is integer field. It can have the following values:
    * 200 - All OK
    * 404 - Schema is missing in Storage
    * 400 - Some fields were not properly mapped with the schema defined
3) lastUpdateTime: This field captures the last time the record was updated by by the indexer service. This is datetime field so you can do range queries on this field.

You can query the index status using the following example query:

```bash
curl --request POST \
  --url /api/search/v2/query \
  --header 'Authorization: Token' \
  --header 'Content-Type: application/json' \
  --header 'OSDU-Data-Partition-Id: Data partition id' \
  --data '{"kind": "*:*:*:*","query": "index.statusCode:404","returnedFields": ["index"]}'
  
NOTE: By default, the API response excludes the 'index' attribute block. The user must specify 'index' as the 'returnedFields" in order to see it in the response.
```
The above query will return all records which had problems due to fields mismatch.

Please refer to the [Search service](searchservice#query) documentation for examples on different kinds of search queries.

[Back to table of contents](#TOC)
  
## Reindex <a name="reindex"></a>

Reindex API allows users to re-index a `kind` without re-ingesting the records via storage API. Reindexing a kind is an asynchronous operation and when a user calls this API, it will respond with HTTP 200 if it can launch the re-indexing or appropriate error code if it cannot. The current status of the indexing can be tracked by calling search API and making query with this particular kind. Please be advised, it may take few seconds to few hours to finish the re-indexing as multiple factors contribute to latency, such as number of records in the kind, current load at the indexer service etc.

__Note__: If a kind has been previously indexed with particular schema and if you wish to apply the schema changes during re-indexing, previous kind index has to be deleted via Index Delete API. In absence of this clean-up, reindex API will use the same schema and overwrite records with the same ids.      

```
POST /api/indexer/v2/reindex
{
  "kind": "common:welldb:wellbore:1.0.0"
}
```

<details><summary>**Curl**</summary>

```bash
curl --request POST \
  --url '/api/indexer/v2/reindex' \
  --header 'accept: application/json' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'OSDU-Data-Partition-Id: common' \
  --data '{
  "kind": "common:welldb:wellbore:1.0.0"
}'
```
</details>

[Back to table of contents](#TOC)

##Schema Service adoption <a name="schema-service-adoption"></a>

Indexer service is in adaptation process to use schemas from the Schema service instead of Storage Service.
The Indexer Service retrieves a schema from the Schema Service if the schema is not found on the Storage Service.
Change affects only Azure implementation so far. Later call to the Storage Service will be deprecated and then removed (after the end of the deprecation period).

[Back to table of contents](#TOC)

###R3 Schema Support <a name="r3-schema-support"></a>

Indexer service support r3 schema. These schemas are created via Schema service. 

Here is an example following end-to-end workflow can be exercised (please update the schema based on your environment):

* Ingest r3 schema for `opendes:wks:master-data--Wellbore:1.0.0`. Schema service payload can be found [here](https://community.opengroup.org/osdu/platform/system/indexer-service/-/blob/master/testing/indexer-test-core/src/main/resources/testData/r3-index_record_wks_master.schema.json).

* Ingest r3 master-data Wellbore record. Storage service payload can be found [here](https://community.opengroup.org/osdu/platform/system/indexer-service/-/blob/master/testing/indexer-test-core/src/main/resources/testData/r3-index_record_wks_master.json)

* Records can be searched via Search service. Here is sample payload:

```
POST /api/search/v2/query HTTP/1.1
Content-Type: application/json
data-partition-id: opendes
{
    "kind": "opendes:wks:master-data--Wellbore:1.0.0",
    "spatialFilter": {
        "field": "data.SpatialLocation.Wgs84Coordinates",
        "byBoundingBox": {
            "topLeft": {
                "longitude": -100.0,
                "latitude": 52.0
            },
            "bottomRight": {
                "longitude": 100.0,
                "latitude": 0.0
            }
        }
    }
}
```
[Back to table of contents](#TOC)

## Version info endpoint
For deployment available public `/info` endpoint, which provides build and git related information.
#### Example response:
```json
{
    "groupId": "org.opengroup.osdu",
    "artifactId": "storage-gcp",
    "version": "0.10.0-SNAPSHOT",
    "buildTime": "2021-07-09T14:29:51.584Z",
    "branch": "feature/GONRG-2681_Build_info",
    "commitId": "7777",
    "commitMessage": "Added copyright to version info properties file",
    "connectedOuterServices": [
      {
        "name": "elasticSearch",
        "version":"..."
      },
      {
        "name": "postgresSql",
        "version":"..."
      },
      {
        "name": "redis",
        "version":"..."
      }
    ]
}
```
This endpoint takes information from files, generated by `spring-boot-maven-plugin`,
`git-commit-id-plugin` plugins. Need to specify paths for generated files to matching
properties:
- `version.info.buildPropertiesPath`
- `version.info.gitPropertiesPath`

[Back to table of contents](#TOC)