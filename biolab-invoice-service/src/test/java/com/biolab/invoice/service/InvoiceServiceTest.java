package com.biolab.invoice.service;

import com.biolab.invoice.dto.*;
import com.biolab.invoice.entity.*;
import com.biolab.invoice.repository.InvoiceRepository;
import com.biolab.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Unit Tests")
class InvoiceServiceTest {

    @InjectMocks private InvoiceService service;
    @Mock private InvoiceRepository repo;

    private Invoice invoice;
    private final UUID invId = UUID.randomUUID(), suppOrgId = UUID.randomUUID(), buyerOrgId = UUID.randomUUID(), projId = UUID.randomUUID();

    @BeforeEach void setUp() {
        invoice = Invoice.builder().invoiceNumber("INV-1").projectId(projId).supplierOrgId(suppOrgId).buyerOrgId(buyerOrgId)
                .status("DRAFT").subtotal(BigDecimal.valueOf(1000)).taxRate(BigDecimal.valueOf(18))
                .taxAmount(BigDecimal.valueOf(180)).taxLabel("GST").total(BigDecimal.valueOf(1180))
                .issueDate(LocalDate.now()).dueDate(LocalDate.now().plusDays(30)).items(new ArrayList<>()).build();
        invoice.setId(invId); invoice.setCreatedAt(Instant.now());
    }

    // ── List ──
    @Test @DisplayName("[TC-INV-001] ✅ List all invoices") void listAll_Ok() { when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(invoice))); assertThat(service.listAll(PageRequest.of(0,10)).getTotalElements()).isEqualTo(1); }
    @Test @DisplayName("[TC-INV-002] ✅ List by supplier") void listBySupplier_Ok() { when(repo.findBySupplierOrgId(eq(suppOrgId), any())).thenReturn(new PageImpl<>(List.of(invoice))); assertThat(service.listBySupplier(suppOrgId, PageRequest.of(0,10)).getContent()).hasSize(1); }
    @Test @DisplayName("[TC-INV-003] ✅ List by buyer") void listByBuyer_Ok() { when(repo.findByBuyerOrgId(eq(buyerOrgId), any())).thenReturn(new PageImpl<>(List.of(invoice))); assertThat(service.listByBuyer(buyerOrgId, PageRequest.of(0,10)).getContent()).hasSize(1); }

    // ── Get ──
    @Test @DisplayName("[TC-INV-004] ✅ Get invoice by ID") void get_Ok() { when(repo.findById(invId)).thenReturn(Optional.of(invoice)); InvoiceDto dto = service.get(invId); assertThat(dto.invoiceNumber()).isEqualTo("INV-1"); assertThat(dto.taxLabel()).isEqualTo("GST"); }
    @Test @DisplayName("[TC-INV-005] ❌ Get invoice not found") void get_404() { when(repo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class); }
    @Test @DisplayName("[TC-INV-006] ✅ Get invoice by number") void getByNumber_Ok() { when(repo.findByInvoiceNumber("INV-1")).thenReturn(Optional.of(invoice)); assertThat(service.getByNumber("INV-1").id()).isEqualTo(invId); }
    @Test @DisplayName("[TC-INV-007] ❌ Get invoice by non-existent number") void getByNumber_404() { when(repo.findByInvoiceNumber("INV-999")).thenReturn(Optional.empty()); assertThatThrownBy(()->service.getByNumber("INV-999")).isInstanceOf(ResourceNotFoundException.class); }

    // ── Create ──
    @Test @DisplayName("[TC-INV-008] ✅ Create invoice with tax") void create_Ok() {
        List<CreateInvoiceItemRequest> items = List.of(new CreateInvoiceItemRequest("HPLC Analysis", 2, BigDecimal.valueOf(500)));
        CreateInvoiceRequest req = new CreateInvoiceRequest(
                buyerOrgId, projId, items, LocalDate.now().plusDays(30), BigDecimal.valueOf(18), "GST", "Net 30");
        when(repo.findMaxInvoiceSeq()).thenReturn(0);
        when(repo.save(any())).thenReturn(invoice);

        InvoiceDto dto = service.create(req, suppOrgId);
        assertThat(dto).isNotNull();
        assertThat(dto.taxLabel()).isEqualTo("GST");
        verify(repo).save(any());
    }

    @Test @DisplayName("[TC-INV-009] ✅ Create invoice defaults tax to zero and label to 'Tax'") void create_DefaultTax() {
        List<CreateInvoiceItemRequest> items = List.of(new CreateInvoiceItemRequest("Item", 1, BigDecimal.valueOf(100)));
        CreateInvoiceRequest req = new CreateInvoiceRequest(buyerOrgId, projId, items, null, null, null, null);
        when(repo.findMaxInvoiceSeq()).thenReturn(5);
        ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
        when(repo.save(cap.capture())).thenReturn(invoice);

        service.create(req, suppOrgId);

        assertThat(cap.getValue().getTaxRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cap.getValue().getTaxLabel()).isEqualTo("Tax");
        assertThat(cap.getValue().getInvoiceNumber()).isEqualTo("INV-6");
    }

    @Test @DisplayName("[TC-INV-010] ✅ Create invoice auto-calculates due date when null") void create_DefaultDueDate() {
        List<CreateInvoiceItemRequest> items = List.of(new CreateInvoiceItemRequest("Item", 1, BigDecimal.TEN));
        CreateInvoiceRequest req = new CreateInvoiceRequest(buyerOrgId, projId, items, null, null, null, null);
        when(repo.findMaxInvoiceSeq()).thenReturn(0);
        ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
        when(repo.save(cap.capture())).thenReturn(invoice);

        service.create(req, suppOrgId);
        assertThat(cap.getValue().getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test @DisplayName("[TC-INV-011] ✅ Create invoice with multiple items calculates amounts") void create_MultiItem() {
        List<CreateInvoiceItemRequest> items = List.of(
                new CreateInvoiceItemRequest("Item A", 2, BigDecimal.valueOf(100)),
                new CreateInvoiceItemRequest("Item B", 3, BigDecimal.valueOf(200))
        );
        CreateInvoiceRequest req = new CreateInvoiceRequest(buyerOrgId, projId, items, null, BigDecimal.valueOf(10), "VAT", null);
        when(repo.findMaxInvoiceSeq()).thenReturn(0);
        ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
        when(repo.save(cap.capture())).thenReturn(invoice);

        service.create(req, suppOrgId);
        assertThat(cap.getValue().getItems()).hasSize(2);
    }

    // ── Update Status ──
    @Test @DisplayName("[TC-INV-012] ✅ Send invoice updates status to SENT") void send_Ok() {
        when(repo.findById(invId)).thenReturn(Optional.of(invoice)); when(repo.save(any())).thenReturn(invoice);
        service.updateStatus(invId, "SENT"); verify(repo).save(any());
    }

    @Test @DisplayName("[TC-INV-013] ✅ Pay invoice sets paidDate") void pay_Ok() {
        when(repo.findById(invId)).thenReturn(Optional.of(invoice));
        ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
        when(repo.save(cap.capture())).thenReturn(invoice);

        service.updateStatus(invId, "PAID");
        assertThat(cap.getValue().getStatus()).isEqualTo("PAID");
        assertThat(cap.getValue().getPaidDate()).isEqualTo(LocalDate.now());
    }

    @Test @DisplayName("[TC-INV-014] ❌ Update status for non-existent invoice") void updateStatus_404() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.updateStatus(UUID.randomUUID(), "SENT")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Stats ──
    @Test @DisplayName("[TC-INV-015] ✅ Supplier stats") void supplierStats_Ok() {
        when(repo.sumPaidBySupplier(suppOrgId)).thenReturn(BigDecimal.valueOf(50000));
        when(repo.findBySupplierOrgId(eq(suppOrgId), any())).thenReturn(new PageImpl<>(List.of(invoice)));
        Map<String, Object> s = service.supplierStats(suppOrgId);
        assertThat(s).containsEntry("totalPaid", BigDecimal.valueOf(50000));
    }

    @Test @DisplayName("[TC-INV-016] ✅ Buyer stats") void buyerStats_Ok() {
        when(repo.sumOutstandingByBuyer(buyerOrgId)).thenReturn(BigDecimal.valueOf(12000));
        Map<String, Object> s = service.buyerStats(buyerOrgId);
        assertThat(s).containsEntry("outstanding", BigDecimal.valueOf(12000));
    }
}
