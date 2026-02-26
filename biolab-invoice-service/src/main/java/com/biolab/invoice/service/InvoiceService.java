package com.biolab.invoice.service;

import com.biolab.invoice.dto.*;
import com.biolab.invoice.entity.*;
import com.biolab.invoice.repository.InvoiceRepository;
import com.biolab.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository repo;

    @Transactional(readOnly = true)
    public Page<InvoiceDto> listBySupplier(UUID orgId, Pageable p) {
        return repo.findBySupplierOrgId(orgId, p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceDto> listByBuyer(UUID orgId, Pageable p) {
        return repo.findByBuyerOrgId(orgId, p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceDto> listAll(Pageable p) {
        return repo.findAll(p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public InvoiceDto get(UUID id) {
        return toDto(repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id)));
    }

    @Transactional(readOnly = true)
    public InvoiceDto getByNumber(String num) {
        return toDto(repo.findByInvoiceNumber(num)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceNumber", num)));
    }

    public InvoiceDto create(CreateInvoiceRequest req, UUID supplierOrgId) {
        int seq = repo.findMaxInvoiceSeq() + 1;
        Invoice inv = Invoice.builder()
            .invoiceNumber("INV-" + seq)
            .supplierOrgId(supplierOrgId)
            .buyerOrgId(req.buyerOrgId())
            .projectId(req.projectId())
            .dueDate(req.dueDate() != null ? req.dueDate() : LocalDate.now().plusDays(30))
            .notes(req.notes())
            .taxRate(req.taxRate() != null ? req.taxRate() : BigDecimal.ZERO)
            .taxLabel(req.taxLabel() != null ? req.taxLabel() : "Tax")
            .build();

        for (int i = 0; i < req.items().size(); i++) {
            var ir = req.items().get(i);
            InvoiceItem item = InvoiceItem.builder()
                .invoice(inv)
                .description(ir.description())
                .quantity(ir.quantity() != null ? ir.quantity() : 1)
                .unitPrice(ir.unitPrice())
                .amount(ir.unitPrice().multiply(BigDecimal.valueOf(ir.quantity() != null ? ir.quantity() : 1)))
                .sortOrder(i)
                .build();
            inv.getItems().add(item);
        }
        inv.recalculate();
        Invoice saved = repo.save(inv);
        log.info("Invoice created: number={}, subtotal={}, taxRate={}%, taxLabel={}, total={}",
            saved.getInvoiceNumber(), saved.getSubtotal(), saved.getTaxRate(), saved.getTaxLabel(), saved.getTotal());
        return toDto(saved);
    }

    public InvoiceDto updateStatus(UUID id, String status) {
        Invoice inv = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        inv.setStatus(status);
        if ("PAID".equals(status)) inv.setPaidDate(LocalDate.now());
        log.info("Invoice status changed: id={}, status={}", id, status);
        return toDto(repo.save(inv));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> supplierStats(UUID orgId) {
        return Map.of("totalPaid", repo.sumPaidBySupplier(orgId), "invoiceCount", repo.findBySupplierOrgId(orgId, Pageable.unpaged()).getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buyerStats(UUID orgId) {
        return Map.of("outstanding", repo.sumOutstandingByBuyer(orgId));
    }

    private InvoiceDto toDto(Invoice i) {
        List<InvoiceItemDto> items = i.getItems().stream()
            .map(it -> new InvoiceItemDto(it.getId(), it.getDescription(), it.getQuantity(), it.getUnitPrice(), it.getAmount()))
            .collect(Collectors.toList());
        return new InvoiceDto(i.getId(), i.getInvoiceNumber(), i.getProjectId(),
            i.getSupplierOrgId(), i.getBuyerOrgId(), i.getStatus(),
            i.getSubtotal(), i.getTaxAmount(), i.getTotal(),
            i.getIssueDate(), i.getDueDate(), i.getPaidDate(), i.getNotes(), items, i.getCreatedAt(),
            i.getTaxRate(), i.getTaxLabel());
    }
}
