public class ARPEntry {

    private String ipAddress;
    private String macAddress;
    private long createdAt;

    public ARPEntry(String ipAddress, String macAddress) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.createdAt = System.currentTimeMillis();
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(int ttlSeconds) {
        long currentTime = System.currentTimeMillis();
        long age = currentTime - createdAt;

        return age >= ttlSeconds * 1000L;
    }

    @Override
    public String toString() {
        return ipAddress + " -> " + macAddress;
    }
}