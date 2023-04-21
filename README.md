# Introduction 

os-indexer-azure is a [Spring Boot](https://spring.io/projects/spring-boot) service that is responsible for indexing Records that enable the `os-search` service to execute OSDU R2 domain searches against Elasticsearch.

## Azure Implementation

The [os-indexer-azure README.md](./provider/indexer-azure/README.md) has all the information needed to get started
running the `os-indexer` Azure implementation

#***REMOVED*** Implementation

All documentation for the Google Cloud implementation of `os-indexer` lives [here](./provider/indexer-gc/README.md)

#***REMOVED*** Implementation

All documentation for the AWS implementation of `os-indexer` lives [here](./provider/indexer-aws/README.md)

## Open API 3.0 - Swagger
- Swagger UI : https://host/context-path/swagger (will redirect to https://host/context-path/swagger-ui/index.html)
- api-docs (JSON) : https://host/context-path/api-docs
- api-docs (YAML) : https://host/context-path/api-docs.yaml

All the Swagger and OpenAPI related common properties are managed here [swagger.properties](./indexer-core/src/main/resources/swagger.properties)
