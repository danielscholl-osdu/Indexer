## Geoshape Decimation

In order to improve indexing and search performance for documents with large geometry, the geo-shape of the following
GeoJSON types in the original shape attribute and virtual shape attribute if exists are decimated
by implementing Ramer–Douglas–Peucker algorithm:
- LineString
- MultiLineString
- Polygon
- MultiPolygon

The feature is enabled for all data partitions since M19.

## Index extension

OSDU Standard index extensions are defined by OSDU Data Definition work-streams with the intent to provide
user/application friendly, derived properties. The standard set, together with the OSDU schemas, form the
interoperability foundation. They can contribute to deliver domain specific APIs according to the Domain Driven Design
principles.

The configurations are encoded in OSDU reference-data records, one per each major schema version. The type name
is IndexPropertyPathConfiguration. With this, the extension properties can be defined as if they were provided by a schema.

In order to reduce the risk when extended evaluation of the solution is still on going, a feature flag that is managed by
the Partition Service is applied to the solution. Here is an example to enable this feature by setting the property 
"index-augmenter-enabled" in a given data partition:
```
{
   "index-augmenter-enabled": {
        "sensitive": false,
        "value": "true"
    }
}
```

If the property "index-augmenter-enabled" is not created or the property value is set to "false" (String type) in the
given data partition, the configurations defined as type IndexPropertyPathConfiguration will be ignored and index extension will be disabled. 
