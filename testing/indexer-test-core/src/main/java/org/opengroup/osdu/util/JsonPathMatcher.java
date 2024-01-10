package org.opengroup.osdu.util;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class JsonPathMatcher {

    // Since there's no unit tests for tests, there's this:
    public static void main(String[] args) {
        String jsonGoodString = """
{       "id": "tenant1:reference-data--IndexPropertyPathConfiguration:index-property--Wellbore:1.",
        "data": {
            "Name": "Wellbore-IndexPropertyPathConfiguration",
            "Description": "The index property list for index-property--Wellbore:1., valid for all index-property--Wellbore kinds for major version 1.",
            "Code": "test:indexer:index-property--Wellbore:1.",
            "AttributionAuthority": "OSDU",
            "Configurations": [{
                    "Name": "WellUWI",
                    "Policy": "ExtractFirstMatch",
                    "Paths": [{
                            "ValueExtraction": {
                                "RelatedConditionMatches": [
                                    "UniqueIdentifier:$",
                                    "RegulatoryName:$",
                                    "PreferredName:$",
                                    "CommonName:$",
                                    "ShortName:$"
                                ],
                                "RelatedConditionProperty": "data.NameAliases[].AliasNameTypeID",
                                "ValuePath": "data.NameAliases[].AliasName"
                            }
                        }
                    ],
                    "UseCase": "As a user I want to discover and match Wells by their UWI. I am aware that this is not globally reliable, however, I am able to specify a prioritized AliasNameType list to look up value in the NameAliases array."
                }
            ]
        }
 }
 """;
        String jsonBadString = """
{       "id": "tenant1:reference-data--IndexPropertyPathConfiguration:index-property--Wellbore:1.",
        "data": {
            "Name": "Wellbore-IndexPropertyPathConfiguration",
            "Description": "The index property list for index-property--Wellbore:1., valid for all index-property--Wellbore kinds for major version 1.",
            "Code": "test:indexer:index-property--Wellbore:1.",
            "AttributionAuthority": "OSDU",
            "Configurations": [{
                    "Name": "WellUWI",
                    "Policy": "ExtractFirstMatch",
                    "Paths": [{
                            "ValueExtraction": {
                                "RelatedConditionMatches": "[UniqueIdentifier:$,RegulatoryName:$,PreferredName:$]",
                                "RelatedConditionProperty": "data.NameAliases[].AliasNameTypeID",
                                "ValuePath": "data.NameAliases[].AliasName"
                            }
                        }
                    ],
                    "UseCase": "As a user I want to discover and match Wells by their UWI. I am aware that this is not globally reliable, however, I am able to specify a prioritized AliasNameType list to look up value in the NameAliases array."
                }
            ]
        }
 }
 """;

        try {
            // Create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            // Parse JSON string into a Map<String, Object>
            Map<String, Object> dataMap = objectMapper.readValue(jsonGoodString, Map.class);
            List<String> stringList = java.util.Arrays.asList("data.Configurations.Paths.ValueExtraction.RelatedConditionMatches".split("\\."));
            boolean found = FindInJson(dataMap, "", stringList);
            System.out.println("in Good String Found? "+(found?"Y":"N"));
            dataMap = objectMapper.readValue(jsonBadString, Map.class);
            found = FindInJson(dataMap, "", stringList);
            System.out.println("in Bad String Found? "+(found?"Y":"N"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Assure that field in the Json path in stringList is an array of strings
    public static boolean FindInJson(Object data, String prefix, List<String> stringList) {
        if (data instanceof Map) {
            // Handle Map
            return handleMap((Map<String, Object>) data, prefix, stringList);
        } else if (data instanceof ArrayList) {
            // Handle ArrayList
            return handleArrayList((ArrayList<?>) data, prefix, stringList);
        } else {
            // Handle other types
            System.out.println(prefix + data.toString());
            return false;
        }
    }

    private static boolean handleMap(Map<String, Object> map, String prefix, List<String> stringList) {
        System.out.println("Checking for "+ stringList.get(0));

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (!entry.getKey().equals(stringList.get(0))) {
                System.out.println("Skipping " + entry.getKey() + ":\n");
                continue;
            }
            if (FindInJson(value, prefix + "\t", stringList.subList(1, stringList.size()))) { return true; }
        }
        return false;
    }

    private static boolean handleArrayList(ArrayList<?> arrayList, String prefix, List<String> stringList) {
        System.out.println("We see an array length " + arrayList.size());
        if (stringList.isEmpty()) return true; // we're done!
        if (arrayList.isEmpty()) {
            System.out.println(prefix + ": []\n");
            return false;
        }
        int i = 0;
        for (Object arrayElementValue : arrayList) {
            System.out.println(prefix + i + ":\n");
            if (FindInJson(arrayElementValue, prefix + "\t", stringList)) { return true; }
            i++;
        }
        return false;
    }
}
