public class NetworkDevice {

    private String deviceName;
    private String ipAddress;
    private String macAddress;

    public NetworkDevice(String deviceName, String ipAddress, String macAddress) {
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @Override
    public String toString() {
        return deviceName + " | IP: " + ipAddress + " | MAC: " + macAddress;
    }
}