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

package org.opengroup.osdu.indexer.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class PropertyUtilTest {

    @Test
    public void isPropertyPathMatched() {
        Assert.assertTrue(PropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityName"));
        Assert.assertTrue(PropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHoleLocation"));

        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityNameAliase"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHole"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched("", "data.ProjectedBottomHole"));
        Assert.assertFalse(PropertyUtil.isPropertyPathMatched(null, "data.ProjectedBottomHole"));
    }

    @Test
    public void removeDataPrefix() {
        Assert.assertEquals("FacilityName", PropertyUtil.removeDataPrefix("data.FacilityName"));
        Assert.assertEquals("FacilityName", PropertyUtil.removeDataPrefix("FacilityName"));
        Assert.assertEquals("ProjectedBottomHoleLocation", PropertyUtil.removeDataPrefix("data.ProjectedBottomHoleLocation"));
        Assert.assertEquals("ProjectedBottomHoleLocation", PropertyUtil.removeDataPrefix("ProjectedBottomHoleLocation"));
        Assert.assertEquals("", PropertyUtil.removeDataPrefix(""));
        Assert.assertNull(PropertyUtil.removeDataPrefix(null));
    }


}
