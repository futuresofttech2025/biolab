package com.biolab.document.controller;

import com.biolab.document.dto.DocumentDto;
import com.biolab.document.service.DocumentService;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/documents") @Tag(name = "Documents")
public class DocumentController {

    private final DocumentService docService;
    public DocumentController(DocumentService ds) { this.docService = ds; }

    @GetMapping("/project/{projectId}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List documents for a project")
    public ResponseEntity<List<DocumentDto>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(docService.listByProject(projectId));
    }

    @PostMapping("/project/{projectId}/upload")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','BUYER','ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Upload a document")
    public ResponseEntity<DocumentDto> upload(@PathVariable UUID projectId,
                                              @RequestParam("file") MultipartFile file) throws Exception {
        UUID userId = CurrentUserContext.require().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(docService.upload(projectId, userId, file));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a document")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws Exception {
        DocumentDto meta = docService.getMetadata(id);
        byte[] data = docService.download(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.fileName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get document metadata")
    public ResponseEntity<DocumentDto> metadata(@PathVariable UUID id) {
        return ResponseEntity.ok(docService.getMetadata(id));
    }
}
