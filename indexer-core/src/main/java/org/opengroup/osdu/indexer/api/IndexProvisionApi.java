package org.opengroup.osdu.indexer.api;

import org.opengroup.osdu.core.common.model.search.SearchServiceRole;
import org.opengroup.osdu.indexer.model.IndexAliasesResult;
import org.opengroup.osdu.indexer.service.IndexAliasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@RestController
@RequestScope
public class IndexProvisionApi {
    @Inject
    private IndexAliasService indexAliasService;

    //@PreAuthorize("@authorizationFilter.hasPermission('" + SearchServiceRole.ADMIN + "')")
    @PostMapping(path = "/aliases")
    public ResponseEntity<IndexAliasesResult> createIndexAliases()  {
       return new ResponseEntity<>(indexAliasService.createIndexAliasesForAll(), HttpStatus.OK);
    }
}
