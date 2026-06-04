package com.youngledo.jmcfx.ui.javaapp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.LoadJavaApplicationUseCase;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.testsupport.FakeJavaAppService;

class SecurityViewModelTest {

    @Test
    void loadPopulatesCertificates() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addCertificate(new X509CertificateEntry(Instant.now(), "cert-1", "SHA256withRSA",
                "CN=test", "CN=ca", "ABC123", null, null, 2048));

        SecurityViewModel vm = new SecurityViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.certificatesProperty().size());
        assertEquals("cert-1", vm.certificatesProperty().getFirst().certificateId());
    }

    @Test
    void loadClearsSelection() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addCertificate(new X509CertificateEntry(Instant.now(), "cert-1", "SHA256withRSA",
                "CN=test", "CN=ca", "ABC123", null, null, 2048));

        SecurityViewModel vm = new SecurityViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedCertificateProperty().set(vm.certificatesProperty().getFirst());

        vm.load(testRecording());
        assertNull(vm.selectedCertificateProperty().get());
    }

    @Test
    void startsWithEmptyDefaults() {
        FakeJavaAppService service = new FakeJavaAppService();
        SecurityViewModel vm = new SecurityViewModel(new LoadJavaApplicationUseCase(service));

        assertEquals(0, vm.certificatesProperty().size());
        assertNull(vm.selectedCertificateProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
