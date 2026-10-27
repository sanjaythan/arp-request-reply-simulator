
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ARPWebServer {

    private static final int PORT = 8080;
    private static final ARPManager manager = new ARPManager();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", PORT), 0);

        server.setExecutor(Executors.newCachedThreadPool());

        // List network devices
        server.createContext("/api/devices", exchange -> {
            if (!requireMethod(exchange, "GET")) return;

            List<NetworkDevice> devices = manager.getDevices();
            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < devices.size(); i++) {
                NetworkDevice d = devices.get(i);

                if (i > 0) json.append(",");

                json.append("{")
                    .append("\"index\":").append(i).append(",")
                    .append("\"name\":\"").append(escape(d.getDeviceName())).append("\",")
                    .append("\"ip\":\"").append(escape(d.getIpAddress())).append("\",")
                    .append("\"mac\":\"").append(escape(d.getMacAddress())).append("\"")
                    .append("}");
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        });

        // Add a device
        server.createContext("/api/devices/add", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            Map<String, String> form = readForm(exchange);
            boolean success = manager.addDevice(
                    form.get("name"), form.get("ip"), form.get("mac"));

            if (success) {
                sendJson(exchange, 200, result(true, "Device added successfully."));
            } else {
                sendJson(exchange, 400, result(false,
                        "Could not add device. Check the event log for details."));
            }
        });

        // Edit a device
        server.createContext("/api/devices/edit", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            try {
                Map<String, String> form = readForm(exchange);
                int index = Integer.parseInt(form.getOrDefault("index", "-1"));

                boolean success = manager.updateDevice(
                        index, form.get("name"), form.get("ip"), form.get("mac"));

                sendJson(exchange, success ? 200 : 400,
                        result(success, success
                                ? "Device updated successfully."
                                : "Could not update device. Check the event log."));
            } catch (Exception e) {
                sendJson(exchange, 400,
                        result(false, "Invalid device selection or details."));
            }
        });

        // Remove a device
        server.createContext("/api/devices/remove", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            try {
                Map<String, String> form = readForm(exchange);
                int index = Integer.parseInt(form.getOrDefault("index", "-1"));

                boolean success = manager.removeDevice(index);

                sendJson(exchange, success ? 200 : 400,
                        result(success, success
                                ? "Device removed successfully."
                                : "Removal failed. At least two devices must remain."));
            } catch (Exception e) {
                sendJson(exchange, 400,
                        result(false, "Invalid device selection."));
            }
        });

        // Resolve an IP address using ARP
        server.createContext("/api/resolve", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            try {
                Map<String, String> form = readForm(exchange);
                int senderIndex = Integer.parseInt(
                        form.getOrDefault("senderIndex", "-1"));
                String targetIP = form.getOrDefault("targetIP", "").trim();

                List<NetworkDevice> devices = manager.getDevices();

                if (senderIndex < 0 || senderIndex >= devices.size()) {
                    sendJson(exchange, 400,
                            result(false, "Select a valid sender device."));
                    return;
                }

                if (!manager.isValidIP(targetIP)) {
                    sendJson(exchange, 400,
                            result(false, "Enter a valid IPv4 address."));
                    return;
                }

                String senderIP = devices.get(senderIndex).getIpAddress();
                String mac = manager.resolve(senderIP, targetIP);

                if (mac == null) {
                    sendJson(exchange, 200,
                            "{\"success\":false,\"mac\":null,"
                            + "\"message\":\"Resolution failed. Check the event log.\"}");
                } else {
                    sendJson(exchange, 200,
                            "{\"success\":true,\"mac\":\"" + escape(mac)
                            + "\",\"message\":\"MAC address resolved successfully.\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 400,
                        result(false, "Invalid resolution request."));
            }
        });

        // Read network statistics
        server.createContext("/api/stats", exchange -> {
            if (!requireMethod(exchange, "GET")) return;

            NetworkStatistics s = manager.getStatistics();

            String json = "{"
                    + "\"totalRequests\":" + s.getTotalRequests() + ","
                    + "\"totalReplies\":" + s.getTotalReplies() + ","
                    + "\"cacheHits\":" + s.getCacheHits() + ","
                    + "\"cacheMisses\":" + s.getCacheMisses() + ","
                    + "\"failedResolutions\":" + s.getFailedResolutions()
                    + "}";

            sendJson(exchange, 200, json);
        });

        // Reset network statistics
        server.createContext("/api/stats/reset", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            manager.resetStatistics();
            sendJson(exchange, 200,
                    result(true, "Network statistics reset."));
        });

        // View a device's ARP cache
        server.createContext("/api/cache", exchange -> {
            if (!requireMethod(exchange, "GET")) return;

            try {
                Map<String, String> params = parseForm(
                        exchange.getRequestURI().getRawQuery());

                int index = Integer.parseInt(
                        params.getOrDefault("index", "0"));

                if (index < 0 || index >= manager.getDevices().size()) {
                    sendJson(exchange, 400,
                            "{\"error\":\"Invalid device index.\"}");
                    return;
                }

                ARPCache cache = manager.getCacheForDevice(index);
                Map<String, ARPEntry> entries = cache.getEntries();

                // lookup() also removes an expired entry.
                for (String ip : entries.keySet()) {
                    cache.lookup(ip, manager.getTTL());
                }

                entries = cache.getEntries();
                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                for (ARPEntry entry : entries.values()) {
                    if (!first) json.append(",");
                    first = false;

                    long age = Math.max(0,
                            (System.currentTimeMillis() - entry.getCreatedAt()) / 1000);

                    json.append("{")
                        .append("\"ip\":\"").append(escape(entry.getIpAddress())).append("\",")
                        .append("\"mac\":\"").append(escape(entry.getMacAddress())).append("\",")
                        .append("\"ageSeconds\":").append(age)
                        .append("}");
                }

                json.append("]");
                sendJson(exchange, 200, json.toString());

            } catch (Exception e) {
                sendJson(exchange, 400,
                        "{\"error\":\"Could not read the ARP cache.\"}");
            }
        });

        // Clear all ARP caches
        server.createContext("/api/cache/clear", exchange -> {
            if (!requireMethod(exchange, "POST")) return;

            manager.clearAllCaches();
            sendJson(exchange, 200,
                    result(true, "All ARP caches cleared."));
        });

        // Read the event log
        server.createContext("/api/events", exchange -> {
            if (!requireMethod(exchange, "GET")) return;

            StringBuilder json = new StringBuilder("[");
            List<String> events = manager.getEventLog();

            for (int i = 0; i < events.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escape(events.get(i))).append("\"");
            }

            json.append("]");
            sendJson(exchange, 200, json.toString());
        });

        // Get or configure cache TTL
        server.createContext("/api/ttl", exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 200,
                        "{\"ttlSeconds\":" + manager.getTTL() + "}");
                return;
            }

            if (!requireMethod(exchange, "POST")) return;

            try {
                Map<String, String> form = readForm(exchange);
                int ttl = Integer.parseInt(
                        form.getOrDefault("ttlSeconds", ""));

                manager.setTTL(ttl);

                sendJson(exchange, 200,
                        "{\"success\":true,\"message\":\"TTL updated successfully.\","
                        + "\"ttlSeconds\":" + manager.getTTL() + "}");

            } catch (NumberFormatException e) {
                sendJson(exchange, 400,
                        result(false, "Enter a whole-number TTL."));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400,
                        result(false, "TTL must be between 5 and 3600 seconds."));
            }
        });

        // Serve the frontend
        Path webRoot = Path.of(
                "src", "main", "resources", "static")
                .toAbsolutePath().normalize();

        server.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath.startsWith("/api/")) {
                sendText(exchange, 404, "API endpoint not found.");
                return;
            }

            if (requestPath.equals("/")) requestPath = "/index.html";

            Path file = webRoot.resolve(requestPath.substring(1)).normalize();

            if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
                sendText(exchange, 404, "File not found.");
                return;
            }

            String type = getContentType(file);
            byte[] bytes = Files.readAllBytes(file);

            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        server.start();

        System.out.println("==================================");
        System.out.println(" ARP Simulator Web Server Started");
        System.out.println(" URL: http://localhost:" + PORT);
        System.out.println(" Press Ctrl+C to stop the server");
        System.out.println("==================================");
    }

    private static boolean requireMethod(
            HttpExchange exchange, String method) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Allow", method);
            sendText(exchange, 405, "Method not allowed.");
            return false;
        }
        return true;
    }

    private static Map<String, String> readForm(
            HttpExchange exchange) throws IOException {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        return parseForm(body);
    }

    private static Map<String, String> parseForm(String input) {
        Map<String, String> map = new HashMap<>();
        if (input == null || input.isEmpty()) return map;

        for (String pair : input.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            map.put(key, value);
        }

        return map;
    }

    private static String result(boolean success, String message) {
        return "{\"success\":" + success
                + ",\"message\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) return "";

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"'  -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }

        return out.toString();
    }

    private static void sendJson(
            HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendText(
            HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String getContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();

        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
        return "application/octet-stream";
    }
}
