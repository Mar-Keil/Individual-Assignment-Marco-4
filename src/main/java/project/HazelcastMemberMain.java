package project;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastMemberMain {

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 5701;
        String clusterName = args.length >= 2 ? args[1] : "matmul-cluster";

        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig net = config.getNetworkConfig();
        net.setPort(port).setPortAutoIncrement(true);

        JoinConfig join = net.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        join.getTcpIpConfig()
                .setEnabled(true)
                .addMember("127.0.0.1:5701")
                .addMember("127.0.0.1:5702")
                .addMember("127.0.0.1:5703");

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);

        System.out.printf("Hazelcast member started  cluster=%s  port=%d%n", clusterName, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { hz.shutdown(); } catch (Exception ignored) {}
        }));

        Thread.currentThread().join();
    }
}