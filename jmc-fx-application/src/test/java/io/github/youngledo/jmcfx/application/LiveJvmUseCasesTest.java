package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.SavedJvmTarget;
import io.github.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.JmxConnectionService;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringService;
import io.github.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;
import org.junit.jupiter.api.Test;

class LiveJvmUseCasesTest {

    @Test
    void createsFacadeFromLiveJvmServices() {
        var services = new LiveJvmApplicationServices(
                new FakeJvmDiscoveryService(),
                new FakeJmxConnectionService(),
                null, null, null, null, null,
                new FakeJmxMonitoringService(),
                new FakeJmxMonitoringRepository(),
                new FakeSavedJvmTargetRepository(),
                new FakeJdpDiscoveryService());

        LiveJvmUseCases useCases = LiveJvmUseCases.from(services);

        assertNotNull(useCases.discovery());
        assertNotNull(useCases.connection());
        assertNotNull(useCases.monitoring());
        assertNotNull(useCases.persistence());
    }

    private static final class FakeJvmDiscoveryService implements JvmDiscoveryService {
        @Override
        public List<JvmConnection> discoverLocalJvms() {
            return List.of();
        }
    }

    private static final class FakeJmxConnectionService implements JmxConnectionService {
        @Override
        public JvmConnection connect(String connectionUrl) {
            return null;
        }

        @Override
        public void disconnect(JvmConnection connection) {
        }
    }

    private static final class FakeJmxMonitoringService implements JmxMonitoringService {
    }

    private static final class FakeJmxMonitoringRepository implements JmxMonitoringRepository {
    }

    private static final class FakeSavedJvmTargetRepository implements SavedJvmTargetRepository {
        @Override
        public List<SavedJvmTarget> findAll() {
            return List.of();
        }

        @Override
        public SavedJvmTarget save(SavedJvmTarget target) {
            return target;
        }

        @Override
        public void deleteById(String id) {
        }

        @Override
        public void markConnected(String id, java.time.Instant connectedAt) {
        }
    }

    private static final class FakeJdpDiscoveryService implements JdpDiscoveryService {
        @Override
        public List<io.github.youngledo.jmcfx.domain.model.JdpJvmAdvertisement> discover(java.time.Duration timeout) {
            return List.of();
        }
    }
}
