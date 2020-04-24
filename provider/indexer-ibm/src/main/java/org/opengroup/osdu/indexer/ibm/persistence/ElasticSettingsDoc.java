// Copyright 2020 IBM Corp. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.ibm.persistence;

import org.opengroup.osdu.indexer.ibm.model.ElasticSettingSchema;
import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElasticSettingsDoc {
	
	public static final String DB_NAME =  "SearchSettings";   //collection name
	
    @Id
    private String _id;
    private String _rev;
    private ElasticSettingSchema settingSchema;
    
	public void setId(String id) {
		this._id = id;		
	}
	
}