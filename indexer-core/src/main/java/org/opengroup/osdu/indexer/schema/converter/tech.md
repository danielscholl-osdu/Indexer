## Table of contests.

1.  Purpose

2.  Package details and main classes

3.  Unit tests

4.  Troubleshooting

5.  Schema retrieval order
    
6.  Converter extensions/modifications

#### Purpose
The purpose of this document is to provide overview of the Schema
converter from the technical point of view. If you need to know more
about transformations, conversion rules, please see
<https://community.opengroup.org/osdu/platform/system/indexer-service/-/blob/master/indexer-core/src/main/java/org/opengroup/osdu/indexer/schema/converter/readme.md>

#### Package details and main classes
Schema converter is in org.opengroup.osdu.indexer.schema.converter
package.

<table>
<thead>
<tr class="header">
<th>Name</th>
<th>Description</th>
<th>Type</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td>config</td>
<td>Contains configuration classes, configuration can be changed via properties file</td>
<td>package</td>
</tr>
<tr class="even">
<td>exception</td>
<td>Processing exceptions</td>
<td>package</td>
</tr>
<tr class="odd">
<td>Interfaces/ SchemaToStorageFormat.java</td>
<td><p>Interface that provides</p>
<p>convertToString(String schemaServiceFormat, String kind); method</p></td>
<td>file</td>
</tr>
<tr class="even">
<td>tags</td>
<td>Supported tags</td>
<td>package</td>
</tr>
<tr class="odd">
<td>PropertiesProcessor.java</td>
<td>Recursively processes items</td>
<td>file</td>
</tr>
<tr class="even">
<td>SchemaToStorageFormatImpl.java</td>
<td>Parse json, calls PropertiesProcessor</td>
<td>file</td>
</tr>
<tr class="odd">
<td>Readme.md</td>
<td></td>
<td>file</td>
</tr>
</tbody>
</table>

Package members.

#### Unit tests

If you have any issues with schema (loading, conversion, etc.) you can
easily create a unit test that checks a schema in
SchemaToStorageFormatImplTest class.

For instance
```json
@Test(expected = SchemaProcessingException.**class**)  
public void wrongDefinitions() {  
testSingleFile("/converter/bad-schema/wrong-definitions-and-missed-type.json"**,
KIND);  
}
```
Expects that schema is wrong

testSingleFile loads a schema, tries to convert it, then compares it
with expected storage schema that is loaded from &lt;filename&gt; +
“.res” file.

Pay attention to folderPassed() method it recursively processes a set of
schemas in a folder.

#### Troubleshooting.

The latest schema converter tries to gather as much conversion errors as
possible (the more the better). Please see logs if conversion not
happens.

If a processing of some schema generates unexpected errors take that
schema and investigate with unit test as was described before.

#### Converter extensions/modifications

a) Add new tags to schema.converter.tags package

b) Modify PropertiesProcessor

c) Add/change unit and integration tests