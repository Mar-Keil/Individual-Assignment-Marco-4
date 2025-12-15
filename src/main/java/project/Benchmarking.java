package project;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.sun.management.OperatingSystemMXBean;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import project.matMul.*;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)

public class Benchmarking {

    private IMatrix matrix;
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
        initUsedMb  = (proc == null ? 0L : proc.getResidentSetSize() / MB);
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
        switch (type) {
            case DISTRIBUTION:
                matrix = new Distribution(Rnd, size);
                break;
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        realBefore = System.nanoTime();
        cpuBefore  = os.getProcessCpuTime();
    }

    @Benchmark
    public void multiply(Blackhole bh) {
        matrix.multiply();
        matrix.clearC();
        bh.consume(matrix.peek());
    }

    @TearDown(Level.Iteration)
    public void tearDownInvocation(ExtraMetrics x) {

        var proc = si.getOperatingSystem().getCurrentProcess();

        long cpuAfter  = os.getProcessCpuTime();
        long realAfter = System.nanoTime();

        x.RAM = (proc == null ? 0L : (proc.getResidentSetSize() / MB) - initUsedMb);
        x.CPU = (double)(cpuAfter - cpuBefore) / (double)(realAfter - realBefore);
    }
}

// cd IdeaProjects/Individual-Assignment-Marco-4
// mvn -q -DskipTests package
// caffeinate java -jar target/benchmarks.jar project.Benchmarking.multiply -rf csv -rff results.csv