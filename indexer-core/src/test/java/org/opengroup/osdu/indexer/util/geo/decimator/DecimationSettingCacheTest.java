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

package org.opengroup.osdu.indexer.util.geo.decimator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DecimationSettingCacheTest {
    private static final String VALID_KEY = "Tenant1-indexer-decimation-enabled";
    private static final String INVALID_KEY = "Tenant2-indexer-decimation-enabled";
    DecimationSettingCache cache;

    @Before
    public void setup() {
        cache = new DecimationSettingCache();
        cache.put(VALID_KEY, true);
    }

    @Test
    public void getValidKey() {
        Assert.assertTrue(cache.containsKey(VALID_KEY));
    }

    @Test
    public void getInvalidKey() {
        Assert.assertFalse(cache.containsKey(INVALID_KEY));
    }
}
