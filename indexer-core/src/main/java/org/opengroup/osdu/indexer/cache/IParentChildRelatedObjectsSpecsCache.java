package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.indexer.model.indexproperty.ParentChildRelatedObjectsSpec;

import java.util.List;

public interface IParentChildRelatedObjectsSpecsCache extends ICache<String, List<ParentChildRelatedObjectsSpec>> {
}
