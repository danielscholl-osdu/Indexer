In order to improve indexing and search performance for documents with large geometry, the geo-shape of the following 
GeoJSON types in the original shape attribute and virtual shape attribute if exists are decimated 
by implementing Ramer–Douglas–Peucker algorithm:
- LineString
- MultiLineString
- Polygon
- MultiPolygon  

In order to reduce the risk when extended evaluation of the solution is still on going, a feature flag that is managed by 
the Partition Service is applied to the solution.
Here is an example to enable this feature by setting the property "indexer-decimation-enabled" in a given data partition:
```
{
   "indexer-decimation-enabled": {
        "sensitive": false,
        "value": "true"
    }
}
```

If the property "indexer-decimation-enabled" is not created or the property value is set to "false" (String type) in the 
given data partition, the geo-shape decimation will be ignored.
