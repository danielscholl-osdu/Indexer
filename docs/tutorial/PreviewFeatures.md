## Geoshape Decimation

In order to improve indexing and search performance for documents with large geometry, the geo-shape of the following 
GeoJSON types in the original shape attribute and virtual shape attribute if exists are decimated 
by implementing Ramer–Douglas–Peucker algorithm:
- LineString
- MultiLineString
- Polygon
- MultiPolygon  


The feature is enabled by default for all data partitions. If client does not want the geo-shape to be decimated in their
data partitions, they can disable geo-shape decimation through the Partition Service.  
Here is an example to disable this feature by setting the property "indexer-decimation-enabled" in a given data partition:
```
{
   "indexer-decimation-enabled": {
        "sensitive": false,
        "value": "false"
    }
}
```

If the property "indexer-decimation-enabled" is not created or the property value is set to "true" (String type) in the 
given data partition, the geo-shape decimation will be enabled.
