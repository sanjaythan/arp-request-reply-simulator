import java.util.HashMap;
import java.util.Map;

public class ARPCache {

    private Map<String, ARPEntry> cache;

    public ARPCache() {
        cache = new HashMap<>();
    }

    public void addEntry(String ipAddress, String macAddress) {
        cache.put(ipAddress, new ARPEntry(ipAddress, macAddress));
    }

    public String lookup(String ipAddress, int ttlSeconds) {

        ARPEntry entry = cache.get(ipAddress);

        if (entry == null) {
            return null;
        }

        if (entry.isExpired(ttlSeconds)) {
            cache.remove(ipAddress);
            return null;
        }

        return entry.getMacAddress();
    }

    public void removeEntry(String ipAddress) {
        cache.remove(ipAddress);
    }

    public void clear() {
        cache.clear();
    }

    public void removeExpiredEntries(int ttlSeconds) {
        cache.entrySet().removeIf(
            entry -> entry.getValue().isExpired(ttlSeconds)
        );
    }

    public Map<String, ARPEntry> getEntries() {
        return new HashMap<>(cache);
    }

    public int size() {
        return cache.size();
    }
}