public class ARPPacket {

    public enum PacketType {
        REQUEST,
        REPLY
    }

    private PacketType packetType;
    private String senderIP;
    private String senderMAC;
    private String targetIP;
    private String targetMAC;

    public ARPPacket(
            PacketType packetType,
            String senderIP,
            String senderMAC,
            String targetIP,
            String targetMAC) {

        this.packetType = packetType;
        this.senderIP = senderIP;
        this.senderMAC = senderMAC;
        this.targetIP = targetIP;
        this.targetMAC = targetMAC;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public String getSenderIP() {
        return senderIP;
    }

    public String getSenderMAC() {
        return senderMAC;
    }

    public String getTargetIP() {
        return targetIP;
    }

    public String getTargetMAC() {
        return targetMAC;
    }

    @Override
    public String toString() {
        return "ARP " + packetType
                + " | Sender IP: " + senderIP
                + " | Sender MAC: " + senderMAC
                + " | Target IP: " + targetIP
                + " | Target MAC: "
                + (targetMAC == null ? "Unknown" : targetMAC);
    }
}