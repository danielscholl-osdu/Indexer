/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import org.opengroup.osdu.indexer.model.SearchRequest;
import org.opengroup.osdu.indexer.model.SearchResponse;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Interface to consume schemas from the Schema Service
 */
public interface SearchService {
    /**
     *
     * @param searchRequest
     * @return
     * @throws URISyntaxException
     */
    SearchResponse query(SearchRequest searchRequest) throws URISyntaxException;

    /**
     *
     * @param searchRequest
     * @return
     * @throws URISyntaxException
     */
    SearchResponse queryWithCursor(SearchRequest searchRequest) throws URISyntaxException;

    String createIdsFilter(List<String> ids);
}
