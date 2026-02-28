package com.biolab.document.service;

import com.biolab.document.dto.DocumentDto;
import com.biolab.document.entity.Document;
import com.biolab.document.repository.DocumentRepository;
import com.biolab.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @InjectMocks private DocumentService service;
    @Mock private DocumentRepository repo;

    private Document doc;
    private final UUID docId = UUID.randomUUID(), projId = UUID.randomUUID(), userId = UUID.randomUUID();

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "storagePath", "/tmp/biolab-test-files");
        doc = new Document(); doc.setId(docId); doc.setProjectId(projId);
        doc.setUploadedBy(userId); doc.setFileName("report.pdf");
        doc.setFileType("PDF"); doc.setFileSize(1024L);
        doc.setStorageKey("key_report.pdf"); doc.setMimeType("application/pdf"); doc.setVersion(1);
    }

    @Test @DisplayName("[TC-DOC-001] ✅ List documents by project") void listByProject_Ok() {
        when(repo.findByProjectIdOrderByCreatedAtDesc(projId)).thenReturn(List.of(doc));
        List<DocumentDto> result = service.listByProject(projId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fileName()).isEqualTo("report.pdf");
    }

    @Test @DisplayName("[TC-DOC-002] ✅ List documents empty for project") void listByProject_Empty() {
        when(repo.findByProjectIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        assertThat(service.listByProject(UUID.randomUUID())).isEmpty();
    }

    @Test @DisplayName("[TC-DOC-003] ✅ Get document metadata") void getMetadata_Ok() {
        when(repo.findById(docId)).thenReturn(Optional.of(doc));
        DocumentDto dto = service.getMetadata(docId);
        assertThat(dto.fileName()).isEqualTo("report.pdf");
        assertThat(dto.fileType()).isEqualTo("PDF");
    }

    @Test @DisplayName("[TC-DOC-004] ❌ Get metadata not found") void getMetadata_404() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMetadata(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("[TC-DOC-005] ❌ Download not found") void download_404() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.download(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("[TC-DOC-006] ✅ Upload stores document metadata") void upload_Ok() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "a,b,c".getBytes());
        ArgumentCaptor<Document> cap = ArgumentCaptor.forClass(Document.class);
        doc.setFileName("data.csv"); doc.setFileType("CSV");
        when(repo.save(cap.capture())).thenReturn(doc);

        DocumentDto dto = service.upload(projId, userId, file);

        assertThat(dto).isNotNull();
        assertThat(cap.getValue().getFileName()).isEqualTo("data.csv");
        assertThat(cap.getValue().getFileType()).isEqualTo("CSV");
        assertThat(cap.getValue().getProjectId()).isEqualTo(projId);
        assertThat(cap.getValue().getChecksum()).isNotNull();
    }

    @Test @DisplayName("[TC-DOC-007] ✅ Upload extracts file type from extension") void upload_ExtractType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});
        doc.setFileType("PNG");
        when(repo.save(any())).thenReturn(doc);

        ArgumentCaptor<Document> cap = ArgumentCaptor.forClass(Document.class);
        when(repo.save(cap.capture())).thenReturn(doc);
        service.upload(projId, userId, file);
        assertThat(cap.getValue().getFileType()).isEqualTo("PNG");
    }
}
