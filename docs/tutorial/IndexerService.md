## Indexer service

### Table of contents <a name="TOC"></a>
- [Indexer service](#indexer-service)
  - [Table of contents <a name="TOC"></a>](#table-of-contents)
- [Get indexing status <a name="get-indexing-status"></a>](#get-indexing-status)
- [Reindex <a name="reindex"></a>](#reindex)
- [Copy Index <a name="copy-index"></a>](#copy-index)
- [Get task status <a name="get-task-status"></a>](#get-task-status)
- [Schema Service adoption <a name="schema-service-adoption"></a>](#schema-service-adoption)
  - [R3 Schema Support <a name="r3-schema-support"></a>](#r3-schema-support)


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

Indexer service adds internal meta data to each record which registers the status of the indexing. The meta data includes the status and the last indexing date and time. This additional meta block helps to see the details of indexing. The format of the index meta block is as follows:

```
"index": {
    "trace": [
        String,
        String
    ],
    "statusCode": Integer,
    "lastUpdateTime": Datetime
}
```
Example:
```
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

```
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

```
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

## Copy Index <a name="copy-index"></a>

Copy Index API can be used copy `kind` index from `common` to a private `data partition` search backend. To call it, kind from `common` partition should be provided as path parameter and private partition-id should be specified in OSDU-Data-Partition-Id header.

__Note__: Copy Index API is intended for __only__ copying `kind` index from `common` cluster to private `partition` cluster, no other combination of data partitions are honored at this time.

```
POST /api/indexer/v2/copyIndex/copy/{kind}
OSDU-Data-Partition-Id:OSDU
```

<details><summary>**Curl**</summary>

```
curl --request POST \
  --url '/api/indexer/v2/copyIndex/copy/common:welldb:wellbore:1.0.0' \
  --header 'accept: application/json' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'OSDU-Data-Partition-Id: OSDU'
```
</details>

The successful response from the above request will be a `task-id`, this can be later used to track the status of the task via task status API(#get-task-status).

```
{ 
  "task": "CrOX4STSQF6kgtSRdERhbw:92863567"
}
```

[Back to table of contents](#TOC)

## Get task status <a name="get-task-status"></a>

Status of ongoing or completed index copy request for given `taskId` can retrieved via GET task status api.

```
GET /api/indexer/v2/copyIndex/taskStatus/{taskId}
```

<details><summary>**Curl**</summary>

```
curl --request GET \
  --url '/api/indexer/v2/copyIndex/taskStatus/[taskid]]' \
  --header 'accept: application/json' \
  --header 'authorization: Bearer <JWT>' \
  --header 'content-type: application/json' \
  --header 'OSDU-Data-Partition-Id: OSDU'
```
</details>

API will respond with status of task.

```
{
    "completed": true,
    "task": {
        "node": "[nodeid]",
        "id": 113159669,
        "type": "transport",
        "action": "indices:data/write/reindex",
        "status": {
            "total": 1530,
            "updated": 0,
            "created": 1530,
            "deleted": 0,
            "batches": 1,
            "version_conflicts": 0,
            "noops": 0,
            "retries": {
                "bulk": 0,
                "search": 0
            },
            "throttled_millis": 0,
            "requests_per_second": -1,
            "throttled_until_millis": 0
        },
        "description": "reindex from [scheme=https host=host-id port=9243 query={\n  \"match_all\" : {\n    \"boost\" : 1.0\n  }\n}][common:welldb:wellbore:1.0.0] to [common:welldb:wellbore:1.0.0]",
        "start_time_in_millis": 1539735233086,
        "running_time_in_nanos": 1094744315,
        "cancellable": true,
        "headers": {}
    },
    "response": {
        "took": 1084,
        "timed_out": false,
        "total": 1530,
        "updated": 0,
        "created": 1530,
        "deleted": 0,
        "batches": 1,
        "version_conflicts": 0,
        "noops": 0,
        "retries": {
            "bulk": 0,
            "search": 0
        },
        "throttled_millis": 0,
        "requests_per_second": -1,
        "throttled_until_millis": 0,
        "failures": []
    }
}
``` 

[Back to table of contents](#TOC)

##Schema Service adoption <a name="schema-service-adoption"></a>

Indexer service is in adaptation process to use schemas from the Schema service instead of Storage Service.
The Indexer Service retrieves a schema from the Schema Service if the schema is not found on the Storage Service.
Change affects only Azure implementation so far.
Later call to the Storage Service will be deprecated and then removed (after the end of the deprecation period).

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

