package org.opengroup.osdu.indexer.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class VirtualPropertyUtilTest {

    @Test
    public void isPropertyPathMatched() {
        Assert.assertTrue(VirtualPropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityName"));
        Assert.assertTrue(VirtualPropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHoleLocation"));

        Assert.assertFalse(VirtualPropertyUtil.isPropertyPathMatched("data.FacilityName", "data.FacilityNameAliase"));
        Assert.assertFalse(VirtualPropertyUtil.isPropertyPathMatched("data.ProjectedBottomHoleLocation.Wgs84Coordinates", "data.ProjectedBottomHole"));
        Assert.assertFalse(VirtualPropertyUtil.isPropertyPathMatched("", "data.ProjectedBottomHole"));
        Assert.assertFalse(VirtualPropertyUtil.isPropertyPathMatched(null, "data.ProjectedBottomHole"));
    }

    @Test
    public void removeDataPrefix() {
        Assert.assertEquals("FacilityName", VirtualPropertyUtil.removeDataPrefix("data.FacilityName"));
        Assert.assertEquals("FacilityName", VirtualPropertyUtil.removeDataPrefix("FacilityName"));
        Assert.assertEquals("ProjectedBottomHoleLocation", VirtualPropertyUtil.removeDataPrefix("data.ProjectedBottomHoleLocation"));
        Assert.assertEquals("ProjectedBottomHoleLocation", VirtualPropertyUtil.removeDataPrefix("ProjectedBottomHoleLocation"));
        Assert.assertEquals("", VirtualPropertyUtil.removeDataPrefix(""));
        Assert.assertNull(VirtualPropertyUtil.removeDataPrefix(null));
    }
}
