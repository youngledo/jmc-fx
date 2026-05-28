package com.youngledo.jmcfx.adapter.jmc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.UTFDataFormatException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int JDP_MAGIC = 0xC0FFEE42;
    private static final int MAX_SUPPORTED_JDP_VERSION = 1;
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
                parseAdvertisement(payload.data(), payload.length(), payload.senderHost()).ifPresent(advertisement -> {
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

    static Optional<JdpJvmAdvertisement> parseAdvertisement(byte[] packet, int length, String senderHost) {
        return parseBinaryAdvertisement(packet, length)
                .flatMap(values -> toAdvertisement(values, senderHost));
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
        List<NetworkInterface> networkInterfaces = multicastInterfaces();
        List<NetworkInterface> joinedInterfaces = new ArrayList<>();
        List<ReceivedJdpPayload> payloads = new ArrayList<>();
        byte[] buffer = new byte[MAX_DATAGRAM_BYTES];
        long deadlineNanos = System.nanoTime() + timeout.toNanos();

        try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
            IOException joinFailure = null;
            for (NetworkInterface networkInterface : networkInterfaces) {
                try {
                    socket.joinGroup(groupAddress, networkInterface);
                    joinedInterfaces.add(networkInterface);
                } catch (IOException exception) {
                    if (joinFailure == null) {
                        joinFailure = new IOException("Could not join JDP multicast group on any interface");
                    }
                    joinFailure.addSuppressed(exception);
                }
            }
            if (joinedInterfaces.isEmpty()) {
                throw joinFailure == null
                        ? new IOException("No multicast-capable network interface found")
                        : joinFailure;
            }

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
                    String senderHost = packet.getAddress() == null ? "" : packet.getAddress().getHostAddress();
                    payloads.add(new ReceivedJdpPayload(packet.getData(), packet.getOffset(), packet.getLength(),
                            senderHost));
                }
            } finally {
                for (NetworkInterface networkInterface : joinedInterfaces) {
                    try {
                        socket.leaveGroup(groupAddress, networkInterface);
                    } catch (IOException exception) {
                        // Discovery should not fail because multicast group cleanup failed.
                    }
                }
            }
        }

        return payloads;
    }

    private static List<NetworkInterface> multicastInterfaces() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        List<NetworkInterface> multicastInterfaces = new ArrayList<>();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || !networkInterface.supportsMulticast()) {
                continue;
            }
            multicastInterfaces.add(networkInterface);
        }
        return multicastInterfaces;
    }

    private static long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            return 0;
        }
        long remainingMillis = Duration.ofNanos(remainingNanos).toMillis();
        return Math.max(1, remainingMillis);
    }

    private static Optional<Map<String, String>> parseBinaryAdvertisement(byte[] packet, int length) {
        if (packet == null || length <= 0) {
            return Optional.empty();
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet, 0,
                Math.min(length, packet.length)))) {
            int magic = input.readInt();
            if (magic != JDP_MAGIC) {
                return Optional.empty();
            }
            int version = input.readUnsignedShort();
            if (version > MAX_SUPPORTED_JDP_VERSION) {
                return Optional.empty();
            }

            Map<String, String> values = new HashMap<>();
            while (true) {
                String key;
                try {
                    key = input.readUTF();
                } catch (EOFException exception) {
                    return Optional.of(values);
                }

                String value;
                try {
                    value = input.readUTF();
                } catch (EOFException | UTFDataFormatException exception) {
                    return Optional.empty();
                }

                if (!key.isBlank()) {
                    values.put(key, value);
                }
            }
        } catch (IOException exception) {
            return Optional.empty();
        }
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

    private static Optional<JdpJvmAdvertisement> toAdvertisement(Map<String, String> values, String senderHost) {
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

    record ReceivedJdpPayload(byte[] data, int length, String senderHost) {
        ReceivedJdpPayload {
            if (data == null || length <= 0) {
                data = new byte[0];
                length = 0;
            } else {
                length = Math.min(length, data.length);
                data = Arrays.copyOf(data, length);
            }
            senderHost = Objects.requireNonNullElse(senderHost, "");
        }

        ReceivedJdpPayload(byte[] data, int offset, int length, String senderHost) {
            this(slice(data, offset, length), length, senderHost);
        }
    }

    private record MulticastJdpPayloadReceiver(String multicastGroup, int multicastPort) implements JdpPayloadReceiver {

        @Override
        public List<ReceivedJdpPayload> receive(Duration timeout) throws IOException {
            return receiveMulticastPayloads(multicastGroup, multicastPort, timeout);
        }
    }

    private static byte[] slice(byte[] data, int offset, int length) {
        if (data == null || length <= 0 || offset < 0 || offset >= data.length) {
            return new byte[0];
        }
        int end = Math.min(data.length, offset + length);
        return Arrays.copyOfRange(data, offset, end);
    }
}
