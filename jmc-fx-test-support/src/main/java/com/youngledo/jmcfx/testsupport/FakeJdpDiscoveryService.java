package com.youngledo.jmcfx.testsupport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;

public class FakeJdpDiscoveryService implements JdpDiscoveryService {

    private final List<JdpJvmAdvertisement> advertisements = new ArrayList<>();
    private RuntimeException failure;
    private Duration lastTimeout;

    public void add(JdpJvmAdvertisement advertisement) {
        advertisements.add(advertisement);
    }

    public void setAdvertisements(List<JdpJvmAdvertisement> nextAdvertisements) {
        advertisements.clear();
        advertisements.addAll(nextAdvertisements);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public Duration lastTimeout() {
        return lastTimeout;
    }

    @Override
    public List<JdpJvmAdvertisement> discover(Duration timeout) {
        lastTimeout = timeout;
        if (failure != null) {
            throw failure;
        }
        return List.copyOf(advertisements);
    }
}
