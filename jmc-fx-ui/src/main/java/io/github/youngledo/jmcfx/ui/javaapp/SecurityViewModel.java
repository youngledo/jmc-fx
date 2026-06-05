package io.github.youngledo.jmcfx.ui.javaapp;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.X509CertificateEntry;
import io.github.youngledo.jmcfx.application.LoadJavaApplicationUseCase;
import io.github.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the Security page.
///
/// Manages X.509 certificate table data from jdk.X509Certificate events.
public class SecurityViewModel {

    private final LoadJavaApplicationUseCase javaAppService;
    private final ObservableList<X509CertificateEntry> certificates = FXCollections.observableArrayList();
    private final ObjectProperty<X509CertificateEntry> selectedCertificate = new SimpleObjectProperty<>();

    public SecurityViewModel(LoadJavaApplicationUseCase javaAppService) {
        this.javaAppService = javaAppService;
    }

    public ObservableList<X509CertificateEntry> certificatesProperty() {
        return certificates;
    }

    public ObjectProperty<X509CertificateEntry> selectedCertificateProperty() {
        return selectedCertificate;
    }

    /// Loads certificate events for the given recording.
    ///
    /// @param recording the flight recording to analyze
    public void load(RecordingSummary recording) {
        List<X509CertificateEntry> entries = javaAppService.loadCertificates(recording);
        FxDispatch.run(() -> {
            certificates.setAll(entries);
            selectedCertificate.set(null);
        });
    }
}
