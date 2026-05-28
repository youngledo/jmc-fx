package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

public final class JmcJdpDiscoveryService implements JdpDiscoveryService {

    static final String DEFAULT_MULTICAST_GROUP = "224.0.23.178";
    static final int DEFAULT_MULTICAST_PORT = 7095;
    static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(750);

    private static final int MAX_DATAGRAM_BYTES = 64 * 1024;
    private static final Pattern SERVICE_URL_PORT_PATTERN = Pattern.compile(":(\\d{1,5})(?:/|$)");
    private static final Comparator<JdpJvmAdvertisement> ADVERTISEMENT_ORDER =
            Comparator.comparing(JdpJvmAdvertisement::displayName)
                    .thenComparing(JdpJvmAdvertisement::serviceUrl)
                    .thenComparing(JdpJvmAdvertisement::id);

    private final JdpPayloadReceiver receiver;

    public JmcJdpDiscoveryService() {
        this(DEFAULT_MULTICAST_GROUP, DEFAULT_MULTICAST_PORT);
    }

    JmcJdpDiscoveryService(String multicastGroup, int multicastPort) {
        this(new MulticastJdpPayloadReceiver(multicastGroup, multicastPort));
    }

    JmcJdpDiscoveryService(JdpPayloadReceiver receiver) {
        this.receiver = Objects.requireNonNull(receiver, "receiver");
    }

    @Override
    public List<JdpJvmAdvertisement> discover(Duration timeout) {
        Duration effectiveTimeout = normalizeTimeout(timeout);
        Map<String, JdpJvmAdvertisement> byId = new HashMap<>();
        Set<String> serviceUrls = new HashSet<>();

        try {
            for (ReceivedJdpPayload payload : receiver.receive(effectiveTimeout)) {
                parseAdvertisement(payload.payload(), payload.senderHost()).ifPresent(advertisement -> {
                    if (!byId.containsKey(advertisement.id()) && !serviceUrls.contains(advertisement.serviceUrl())) {
                        byId.put(advertisement.id(), advertisement);
                        serviceUrls.add(advertisement.serviceUrl());
                    }
                });
            }
        } catch (IOException exception) {
            throw new JmcFxException("JDP discovery failed", exception);
        }

        return byId.values().stream()
                .sorted(ADVERTISEMENT_ORDER)
                .toList();
    }

    static Optional<JdpJvmAdvertisement> parseAdvertisement(String payload, String senderHost) {
        Map<String, String> values = parseKeyValueLines(payload);
        String serviceUrl = value(values, "JMX_SERVICE_URL");
        if (serviceUrl.isBlank()) {
            return Optional.empty();
        }

        String id = firstNonBlank(value(values, "DISCOVERABLE_SESSION_UUID"), serviceUrl);
        String displayName = firstNonBlank(value(values, "INSTANCE_NAME"), value(values, "MAIN_CLASS"), serviceUrl);
        String javaVersion = value(values, "JVM_VERSION");
        return Optional.of(new JdpJvmAdvertisement(
                id,
                displayName,
                serviceUrl,
                senderHost,
                parseServiceUrlPort(serviceUrl),
                javaVersion));
    }

    static Duration normalizeTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return DEFAULT_TIMEOUT;
        }
        return timeout;
    }

    private static List<ReceivedJdpPayload> receiveMulticastPayloads(
            String multicastGroup, int multicastPort, Duration timeout) throws IOException {
        InetAddress group = InetAddress.getByName(multicastGroup);
        InetSocketAddress groupAddress = new InetSocketAddress(group, multicastPort);
        NetworkInterface networkInterface = multicastInterface();
        List<ReceivedJdpPayload> payloads = new ArrayList<>();
        byte[] buffer = new byte[MAX_DATAGRAM_BYTES];
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            socket.joinGroup(groupAddress, networkInterface);
            try {
                while (true) {
                    long remainingMillis = remainingMillis(deadlineNanos);
                    if (remainingMillis <= 0) {
                        break;
                    }
                    socket.setSoTimeout((int) Math.min(remainingMillis, Integer.MAX_VALUE));
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketTimeoutException exception) {
                        break;
                    }
                    String payload = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                            StandardCharsets.UTF_8);
                    String senderHost = packet.getAddress() == null ? "" : packet.getAddress().getHostAddress();
                    payloads.add(new ReceivedJdpPayload(payload, senderHost));
                }
            } finally {
                socket.leaveGroup(groupAddress, networkInterface);
            }
        }

        return payloads;
    }

    private static NetworkInterface multicastInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        NetworkInterface loopback = null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || !networkInterface.supportsMulticast()) {
                continue;
            }
            if (!networkInterface.isLoopback()) {
                return networkInterface;
            }
            loopback = networkInterface;
        }
        if (loopback != null) {
            return loopback;
        }
        throw new SocketException("No multicast-capable network interface found");
    }

    private static long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return 0;
        }
        long remainingMillis = Duration.ofNanos(remainingNanos).toMillis();
        return Math.max(1, remainingMillis);
    }

    private static Map<String, String> parseKeyValueLines(String payload) {
        Map<String, String> values = new HashMap<>();
        if (payload == null || payload.isBlank()) {
            return values;
        }

        for (String line : payload.lines().toList()) {
            int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "");
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static int parseServiceUrlPort(String serviceUrl) {
        Matcher matcher = SERVICE_URL_PORT_PATTERN.matcher(serviceUrl);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        if (matches.isEmpty()) {
            return -1;
        }
        try {
            int port = Integer.parseInt(matches.getLast());
            return port <= 65535 ? port : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    @FunctionalInterface
    interface JdpPayloadReceiver {
        List<ReceivedJdpPayload> receive(Duration timeout) throws IOException;
    }

    record ReceivedJdpPayload(String payload, String senderHost) {
        ReceivedJdpPayload {
            payload = Objects.requireNonNullElse(payload, "");
            senderHost = Objects.requireNonNullElse(senderHost, "");
        }
    }

    private record MulticastJdpPayloadReceiver(String multicastGroup, int multicastPort) implements JdpPayloadReceiver {

        @Override
        public List<ReceivedJdpPayload> receive(Duration timeout) throws IOException {
            return receiveMulticastPayloads(multicastGroup, multicastPort, timeout);
        }
    }
}
