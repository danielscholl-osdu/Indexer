package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.indexer.model.indexproperty.ParentChildRelationshipSpec;

import java.util.List;

public interface IParentChildRelationshipSpecsCache extends ICache<String, List<ParentChildRelationshipSpec>> {
}
