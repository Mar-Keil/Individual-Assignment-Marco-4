package project;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import com.sun.management.OperatingSystemMXBean;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import project.matMul.*;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public class Benchmarking {

    private IMatrix matrix;
    private HazelcastInstance hz;

    private final OperatingSystemMXBean os;
    private final SystemInfo si;
    private final Rnd rnd;

    private long realBefore;
    private long cpuBefore;

    private final long initUsedMb;
    private final int MB = 1024 * 1024;

    public Benchmarking() {
        this.rnd = new Rnd();
        this.os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.si = new SystemInfo();

        GlobalMemory memory = si.getHardware().getMemory();
        var proc = si.getOperatingSystem().getCurrentProcess();

        initUsedMb = (proc == null ? 0L : proc.getResidentSetSize() / MB);
        long totalMb = memory.getTotal() / MB;

        System.out.printf(
                "Initial memory  initial used=%d MB  total physical=%d MB%n%n",
                initUsedMb,
                totalMb
        );
    }

    @Param({"512", "1024", "2048", "4096", "8192"})
    int size;

    @Param({"DISTRIBUTION"})
    MatrixType type;

    @Setup(Level.Trial)
    public void setupTrial() {
        ClientConfig cfg = new ClientConfig();
        cfg.setClusterName("matmul-cluster");
        cfg.getNetworkConfig().addAddress(
                "127.0.0.1:5701",
                "127.0.0.1:5702",
                "127.0.0.1:5703"
        );

        hz = HazelcastClient.newHazelcastClient(cfg);

        switch (type) {
            case DISTRIBUTION:
                matrix = new Distribution(hz, rnd, size);
                break;
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        if (hz != null) {
            hz.shutdown();
            hz = null;
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        realBefore = System.nanoTime();
        cpuBefore = os.getProcessCpuTime();
    }

    @Benchmark
    public void multiply(Blackhole bh) {
        matrix.multiply();
        bh.consume(matrix.peek());
        matrix.clearC();
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration(ExtraMetrics x) {
        var proc = si.getOperatingSystem().getCurrentProcess();

        long cpuAfter = os.getProcessCpuTime();
        long realAfter = System.nanoTime();

        x.RAM = (proc == null ? 0L : (proc.getResidentSetSize() / MB) - initUsedMb);
        x.CPU = (double) (cpuAfter - cpuBefore) / (double) (realAfter - realBefore);
    }

    @AuxCounters(AuxCounters.Type.EVENTS)
    @State(Scope.Thread)
    public static class ExtraMetrics {
        public long RAM;
        public double CPU;
    }
}