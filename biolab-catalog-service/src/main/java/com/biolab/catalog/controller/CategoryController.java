package com.biolab.catalog.controller;

import com.biolab.catalog.dto.CategoryDto;
import com.biolab.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/catalog/categories")
@Tag(name = "Service Categories")
public class CategoryController {

    private final CatalogService catalogService;
    public CategoryController(CatalogService cs) { this.catalogService = cs; }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List all active categories")
    public ResponseEntity<List<CategoryDto>> list() {
        return ResponseEntity.ok(catalogService.listCategories());
    }
}
