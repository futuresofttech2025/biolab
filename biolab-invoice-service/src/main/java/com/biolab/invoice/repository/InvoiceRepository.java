package com.biolab.invoice.repository;

import com.biolab.invoice.entity.Invoice;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String number);
    Page<Invoice> findBySupplierOrgId(UUID orgId, Pageable pageable);
    Page<Invoice> findByBuyerOrgId(UUID orgId, Pageable pageable);
    Page<Invoice> findByStatus(String status, Pageable pageable);
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS int)), 1000) FROM Invoice i")
    int findMaxInvoiceSeq();
    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.status = 'PAID' AND i.supplierOrgId = :orgId")
    java.math.BigDecimal sumPaidBySupplier(UUID orgId);
    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.status IN ('SENT','VIEWED','OVERDUE') AND i.buyerOrgId = :orgId")
    java.math.BigDecimal sumOutstandingByBuyer(UUID orgId);
}
