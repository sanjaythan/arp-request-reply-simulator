
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ARPManager {

    private static final int MAX_DEVICES = 5;
    private static final int MIN_DEVICES = 2;

    private static final Pattern MAC_PATTERN = Pattern.compile(
        "(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$"
    );

    private final List<NetworkDevice> devices;
    private final Map<NetworkDevice, ARPCache> caches;
    private final NetworkStatistics statistics;
    private final List<String> eventLog;

    private int ttlSeconds = 60;

    // Constructor
    public ARPManager() {
        devices = new ArrayList<>();
        caches = new HashMap<>();
        statistics = new NetworkStatistics();
        eventLog = new ArrayList<>();
        addInitialDevices();
}

    // Initialize the network with two default devices
    private void addInitialDevices() {
        NetworkDevice device1 = new NetworkDevice(
            "Device 1",
            "192.168.1.10",
            "00:AA:00:11:22:33"
        );

        NetworkDevice device2 = new NetworkDevice(
            "Device 2",
            "192.168.1.20",
            "00:BB:00:44:55:66"
        );

        devices.add(device1);
        devices.add(device2);

        caches.put(device1, new ARPCache());
        caches.put(device2, new ARPCache());

        log("Network initialized with two devices.");
    }

    // Validate IPv4 address
    public boolean isValidIP(String ip) {
        if (ip == null) {
            return false;
        }

        String[] parts = ip.split("\\.", -1);

        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            if (part.isEmpty() || !part.matches("\\d{1,3}")) {
                return false;
            }

            try {
                int value = Integer.parseInt(part);

                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    // Validate MAC address
    public boolean isValidMAC(String mac) {
        return mac != null && MAC_PATTERN.matcher(mac).matches();
    }

    // Check for duplicate IP addresses
    private boolean isDuplicateIP(String ip, NetworkDevice excluded) {
        for (NetworkDevice device : devices) {
            if (device != excluded &&
                device.getIpAddress().equals(ip)) {
                return true;
            }
        }

        return false;
    }

    // Check for duplicate MAC addresses
    private boolean isDuplicateMAC(String mac, NetworkDevice excluded) {
        for (NetworkDevice device : devices) {
            if (device != excluded &&
                device.getMacAddress().equalsIgnoreCase(mac)) {
                return true;
            }
        }

        return false;
    }

    // Add a new device
    public boolean addDevice(String name, String ip, String mac) {
        if (devices.size() >= MAX_DEVICES) {
            log("Cannot add device: maximum of five devices reached.");
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            log("Cannot add device: name is required.");
            return false;
        }

        if (!isValidIP(ip) || !isValidMAC(mac)) {
            log("Cannot add device: invalid IP or MAC address.");
            return false;
        }

        if (isDuplicateIP(ip, null) || isDuplicateMAC(mac, null)) {
            log("Cannot add device: duplicate IP or MAC address.");
            return false;
        }

        NetworkDevice device = new NetworkDevice(
            name.trim(),
            ip,
            mac.toUpperCase()
        );

        devices.add(device);
        caches.put(device, new ARPCache());

        log("Added " + device.getDeviceName() + ".");

        return true;
    }

    // Update an existing device
    public boolean updateDevice(
            int index,
            String name,
            String ip,
            String mac) {

        if (index < 0 || index >= devices.size()) {
            log("Cannot edit device: invalid selection.");
            return false;
        }

        if (name == null || name.trim().isEmpty()) {
            log("Cannot edit device: name is required.");
            return false;
        }

        if (!isValidIP(ip) || !isValidMAC(mac)) {
            log("Cannot edit device: invalid IP or MAC address.");
            return false;
        }

        NetworkDevice device = devices.get(index);

        if (isDuplicateIP(ip, device) ||
            isDuplicateMAC(mac, device)) {
            log("Cannot edit device: duplicate IP or MAC address.");
            return false;
        }

        device.setDeviceName(name.trim());
        device.setIpAddress(ip);
        device.setMacAddress(mac.toUpperCase());

        // Prevent stale mappings after network changes.
        clearAllCaches();

        log("Updated device: " + device.getDeviceName() + ".");

        return true;
    }

    // Remove a device while retaining at least two devices
    public boolean removeDevice(int index) {
        if (devices.size() <= MIN_DEVICES) {
            log("Cannot remove device: at least two devices are required.");
            return false;
        }

        if (index < 0 || index >= devices.size()) {
            log("Cannot remove device: invalid selection.");
            return false;
        }

        NetworkDevice removed = devices.remove(index);
        caches.remove(removed);

        clearAllCaches();

        log("Removed " + removed.getDeviceName() + ".");

        return true;
    }

    // Resolve a target IP address using simulated ARP
    public String resolve(String senderIP, String targetIP) {
        NetworkDevice sender = findDeviceByIP(senderIP);

        if (sender == null) {
            log("Resolution failed: sender device not found.");
            statistics.incrementFailedResolutions();
            return null;
        }

        ARPCache senderCache = caches.get(sender);

        // Check the sender's cache first
        String cachedMAC = senderCache.lookup(targetIP, ttlSeconds);

        if (cachedMAC != null) {
            statistics.incrementCacheHits();
            log("CACHE HIT: " + targetIP + " -> " + cachedMAC);
            return cachedMAC;
        }

        statistics.incrementCacheMisses();
        log("CACHE MISS: " + targetIP
            + ". Broadcasting ARP request.");

        // Create a simulated ARP request
        ARPPacket request = new ARPPacket(
            ARPPacket.PacketType.REQUEST,
            sender.getIpAddress(),
            sender.getMacAddress(),
            targetIP,
            null
        );

        statistics.incrementRequests();
        log(request.toString());

        // Find the device that owns the target IP
        NetworkDevice target = findDeviceByIP(targetIP);

        if (target == null || target == sender) {
            statistics.incrementFailedResolutions();

            log("ARP resolution failed: no other device owns "
                + targetIP + ".");

            return null;
        }

        // Generate a simulated unicast ARP reply
        ARPPacket reply = new ARPPacket(
            ARPPacket.PacketType.REPLY,
            target.getIpAddress(),
            target.getMacAddress(),
            sender.getIpAddress(),
            sender.getMacAddress()
        );

        statistics.incrementReplies();
        log("Unicast ARP reply received: " + reply);

        // Cache the resolved address
        senderCache.addEntry(
            target.getIpAddress(),
            target.getMacAddress()
        );

        log("Cache updated: " + target.getIpAddress()
            + " -> " + target.getMacAddress());

        return target.getMacAddress();
    }

    // Find a device using its IP address
    private NetworkDevice findDeviceByIP(String ip) {
        for (NetworkDevice device : devices) {
            if (device.getIpAddress().equals(ip)) {
                return device;
            }
        }

        return null;
    }

    // Configure cache TTL (5 to 3600 seconds)
    public void setTTL(int ttlSeconds) {
        if (ttlSeconds < 5 || ttlSeconds > 3600) {
            throw new IllegalArgumentException(
                "TTL must be between 5 and 3600 seconds."
            );
        }

        this.ttlSeconds = ttlSeconds;
        log("Cache TTL set to " + ttlSeconds + " seconds.");
    }

    public int getTTL() {
        return ttlSeconds;
    }

    // Clear every device's ARP cache
    public void clearAllCaches() {
        for (ARPCache cache : caches.values()) {
            cache.clear();
        }

        log("All ARP caches cleared.");
    }

    // Reset network counters
    public void resetStatistics() {
        statistics.reset();
        log("Network statistics reset.");
    }

    // Return a copy of the device list
    public List<NetworkDevice> getDevices() {
        return new ArrayList<>(devices);
    }
    public NetworkDevice getDevice(int index) {
    if (index < 0 || index >= devices.size()) {
        throw new IndexOutOfBoundsException("Invalid device index.");
    }

    return devices.get(index);
}
    // Retrieve a device's ARP cache by its list index
    public ARPCache getCacheForDevice(int index) {
        if (index < 0 || index >= devices.size()) {
            throw new IndexOutOfBoundsException("Invalid device index.");
        }

        return caches.get(devices.get(index));
    }

    public NetworkStatistics getStatistics() {
        return statistics;
    }

    // Return a copy of the event log
    public List<String> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    // Record an event with a timestamp
    private void log(String message) {
        eventLog.add(
            java.time.LocalTime.now().withNano(0)
                + " | " + message
        );
    }
}
