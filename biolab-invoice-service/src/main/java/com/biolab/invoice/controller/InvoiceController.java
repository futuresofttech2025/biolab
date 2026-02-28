package com.biolab.invoice.controller;

import com.biolab.invoice.dto.*;
import com.biolab.invoice.service.InvoiceService;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/invoices") @Tag(name = "Invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    public InvoiceController(InvoiceService is) { this.invoiceService = is; }

    @GetMapping("/supplier")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List invoices for supplier org")
    public ResponseEntity<Page<InvoiceDto>> listSupplier(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listBySupplier(UUID.fromString(CurrentUserContext.require().orgId()), pageable));
    }

    @GetMapping("/buyer")
    @PreAuthorize("@perm.hasAnyRole('BUYER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List invoices for buyer org")
    public ResponseEntity<Page<InvoiceDto>> listBuyer(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listByBuyer(UUID.fromString(CurrentUserContext.require().orgId()), pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("@perm.isAdmin()")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "401", description = "Unauthorized"), @ApiResponse(responseCode = "403", description = "Forbidden")})
    @Operation(summary = "List all invoices (admin)")
    public ResponseEntity<Page<InvoiceDto>> listAll(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.listAll(pageable));
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.get(id));
    }

    @GetMapping("/number/{number}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success"), @ApiResponse(responseCode = "404", description = "Not found")})
    @Operation(summary = "Get invoice by number")
    public ResponseEntity<InvoiceDto> getByNumber(@PathVariable String number) {
        return ResponseEntity.ok(invoiceService.getByNumber(number));
    }

    @PostMapping
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "400", description = "Validation error")})
    @Operation(summary = "Create an invoice")
    public ResponseEntity<InvoiceDto> create(@Valid @RequestBody CreateInvoiceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(invoiceService.create(req, UUID.fromString(CurrentUserContext.require().orgId())));
    }

    @PatchMapping("/{id}/send")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<InvoiceDto> send(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, "SENT"));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("@perm.hasAnyRole('BUYER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<InvoiceDto> pay(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, "PAID"));
    }

    @GetMapping("/stats/supplier")
    @PreAuthorize("@perm.hasAnyRole('SUPPLIER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> supplierStats() {
        return ResponseEntity.ok(invoiceService.supplierStats(UUID.fromString(CurrentUserContext.require().orgId())));
    }

    @GetMapping("/stats/buyer")
    @PreAuthorize("@perm.hasAnyRole('BUYER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> buyerStats() {
        return ResponseEntity.ok(invoiceService.buyerStats(UUID.fromString(CurrentUserContext.require().orgId())));
    }
}
