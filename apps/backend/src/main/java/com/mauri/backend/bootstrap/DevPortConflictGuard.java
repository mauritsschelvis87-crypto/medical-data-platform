package com.mauri.backend.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public final class DevPortConflictGuard {

    private static final String ENABLE_PROPERTY = "app.dev.auto-replace-running-instance";
    private static final String DEFAULT_HEALTH_PATH = "/api/health";
    private static final int DEFAULT_SERVER_PORT = 8081;
    private static final Duration HEALTH_TIMEOUT = Duration.ofMillis(900);
    private static final long PORT_RELEASE_TIMEOUT_MS = 10_000L;
    private static final long PORT_RELEASE_POLL_MS = 200L;

    private DevPortConflictGuard() {
    }

    public static void replaceRunningInstanceIfNeeded(String[] args) {
        if (!isEnabled(args)) {
            return;
        }

        int serverPort = resolveServerPort(args);
        if (!isPortInUse(serverPort)) {
            return;
        }

        Optional<Long> existingPid = findListeningProcessId(serverPort);
        if (existingPid.isEmpty()) {
            return;
        }

        long pid = existingPid.get();
        if (pid == ProcessHandle.current().pid()) {
            return;
        }

        if (!belongsToBackend(serverPort, pid)) {
            return;
        }

        System.out.printf(
                Locale.ROOT,
                "Existing backend instance detected on port %d (PID %d). Replacing it before startup.%n",
                serverPort,
                pid
        );

        stopProcess(pid);
        waitForPortRelease(serverPort);
    }

    private static boolean isEnabled(String[] args) {
        String value = firstNonBlank(
                findArgumentValue(args, ENABLE_PROPERTY),
                System.getProperty(ENABLE_PROPERTY),
                System.getenv(toEnvName(ENABLE_PROPERTY)),
                readPropertyFromClasspath(ENABLE_PROPERTY)
        );

        return Boolean.parseBoolean(value);
    }

    private static int resolveServerPort(String[] args) {
        String rawPort = firstNonBlank(
                findArgumentValue(args, "server.port"),
                System.getProperty("server.port"),
                System.getenv("SERVER_PORT"),
                readPropertyFromClasspath("server.port")
        );

        if (rawPort == null) {
            return DEFAULT_SERVER_PORT;
        }

        try {
            return Integer.parseInt(rawPort.trim());
        } catch (NumberFormatException exception) {
            return DEFAULT_SERVER_PORT;
        }
    }

    private static boolean isPortInUse(int port) {
        try (var socket = new java.net.Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static Optional<Long> findListeningProcessId(int port) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return findListeningProcessIdWindows(port);
        }
        return Optional.empty();
    }

    private static Optional<Long> findListeningProcessIdWindows(int port) {
        ProcessBuilder builder = new ProcessBuilder("netstat", "-ano", "-p", "tcp");
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            List<String> lines = readAllLines(process.getInputStream());
            process.waitFor();

            for (String line : lines) {
                String normalized = line.trim().replaceAll("\\s+", " ");
                if (!normalized.startsWith("TCP ")) {
                    continue;
                }

                String[] parts = normalized.split(" ");
                if (parts.length < 5) {
                    continue;
                }

                String localAddress = parts[1];
                String state = parts[3];
                String pidToken = parts[4];

                if (!"LISTENING".equalsIgnoreCase(state)) {
                    continue;
                }

                if (!localAddress.endsWith(":" + port)) {
                    continue;
                }

                return Optional.of(Long.parseLong(pidToken));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | NumberFormatException exception) {
        }

        return Optional.empty();
    }

    private static boolean belongsToBackend(int port, long pid) {
        return isBackendHealthEndpointActive(port) || commandLineLooksLikeBackend(pid);
    }

    private static boolean isBackendHealthEndpointActive(int port) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(HEALTH_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + DEFAULT_HEALTH_PATH))
                .timeout(HEALTH_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            String body = response.body();
            return body.contains("\"service\":\"backend\"") && body.contains("\"status\":\"UP\"");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static boolean commandLineLooksLikeBackend(long pid) {
        return ProcessHandle.of(pid)
                .flatMap(handle -> handle.info().commandLine())
                .map(commandLine -> commandLine.toLowerCase(Locale.ROOT))
                .map(commandLine ->
                        commandLine.contains("com.mauri.backend.backendapplication")
                                || (commandLine.contains("spring-boot:run")
                                && commandLine.contains("medical-data-platform")
                                && commandLine.contains("backend"))
                                || commandLine.contains("\\apps\\backend\\")
                )
                .orElse(false);
    }

    private static void stopProcess(long pid) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            stopProcessWindows(pid);
            return;
        }

        ProcessHandle.of(pid).ifPresent(handle -> {
            handle.destroy();
            try {
                handle.onExit().get();
            } catch (Exception ignored) {
                handle.destroyForcibly();
            }
        });
    }

    private static void stopProcessWindows(long pid) {
        ProcessBuilder builder = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid), "/T", "/F");
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void waitForPortRelease(int port) {
        long deadline = System.currentTimeMillis() + PORT_RELEASE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!isPortInUse(port)) {
                return;
            }

            try {
                Thread.sleep(PORT_RELEASE_POLL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static String findArgumentValue(String[] args, String key) {
        String prefix = "--" + key + "=";
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static String readPropertyFromClasspath(String key) {
        Properties properties = new Properties();
        try (InputStream inputStream = DevPortConflictGuard.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                return null;
            }
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException exception) {
            return null;
        }
    }

    private static String toEnvName(String propertyName) {
        return propertyName.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> readAllLines(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
