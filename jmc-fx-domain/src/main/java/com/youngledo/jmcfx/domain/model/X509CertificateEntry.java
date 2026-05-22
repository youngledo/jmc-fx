package com.youngledo.jmcfx.domain.model;

import java.time.Instant;

/// Immutable data carrier for one jdk.X509Certificate event.
///
/// @param startTime   the event timestamp
/// @param certificateId the X.509 certificate identifier/serial
/// @param algorithm   the signature algorithm
/// @param subject     the certificate subject distinguished name
/// @param issuer      the certificate issuer distinguished name
/// @param serialNumber the certificate serial number
/// @param validFrom   the certificate not-before date, or null
/// @param validTo     the certificate not-after date, or null
/// @param keyLength   the key length in bits, or 0 if unavailable
public record X509CertificateEntry(
        Instant startTime,
        String certificateId,
        String algorithm,
        String subject,
        String issuer,
        String serialNumber,
        Instant validFrom,
        Instant validTo,
        int keyLength) {
}
