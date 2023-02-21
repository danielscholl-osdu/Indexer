package org.opengroup.osdu.indexer.api;

import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.indexer.model.IndexAliasesProvisionResult;
import org.opengroup.osdu.indexer.service.IndexAliasService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import javax.inject.Inject;

public class IndexProvisionApi {
    @Inject
    private IndexAliasService indexAliasService;

    @PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "')")
    @PostMapping
    public IndexAliasesProvisionResult previsionIndexAliases()  {
       return indexAliasService.createIndexAliasesForAll();
    }
}
