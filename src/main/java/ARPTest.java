public class ARPTest {

    public static void main(String[] args) {

        ARPManager manager = new ARPManager();

        System.out.println("=== FIRST LOOKUP ===");

        String mac1 = manager.resolve(
            "192.168.1.10",
            "192.168.1.20"
        );

        System.out.println("Resolved MAC: " + mac1);

        System.out.println("\n=== SECOND LOOKUP ===");

        String mac2 = manager.resolve(
            "192.168.1.10",
            "192.168.1.20"
        );

        System.out.println("Resolved MAC: " + mac2);

        System.out.println("\n=== UNKNOWN TARGET ===");

        String mac3 = manager.resolve(
            "192.168.1.10",
            "192.168.1.99"
        );

        System.out.println("Resolved MAC: " + mac3);

        NetworkStatistics stats = manager.getStatistics();

        System.out.println("\n=== STATISTICS ===");
        System.out.println("Requests: " + stats.getTotalRequests());
        System.out.println("Replies: " + stats.getTotalReplies());
        System.out.println("Cache hits: " + stats.getCacheHits());
        System.out.println("Cache misses: " + stats.getCacheMisses());
        System.out.println("Failures: " + stats.getFailedResolutions());

        System.out.println("\n=== EVENT LOG ===");

        for (String event : manager.getEventLog()) {
            System.out.println(event);
        }
    }
}