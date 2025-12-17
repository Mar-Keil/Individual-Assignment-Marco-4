package project.matMul;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.map.IMap;

import project.Rnd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class Distribution implements IMatrix {

    private static final String MAP_A = "matmul_A_rows";
    private static final String MAP_B = "matmul_B_rows";

    private final int size;
    private final double[][] a;
    private final double[][] b;
    private final double[][] c;

    private final HazelcastInstance hz;

    public Distribution(HazelcastInstance hz, Rnd rnd, int size) {
        this.hz = hz;
        this.size = size;

        this.a = new double[size][];
        this.b = new double[size][];
        this.c = new double[size][size];

        for (int r = 0; r < size; r++) {
            this.a[r] = rnd.fill(size);
            this.b[r] = rnd.fill(size);
        }

        IMap<Integer, double[]> aMap = hz.getMap(MAP_A);
        IMap<Integer, double[]> bMap = hz.getMap(MAP_B);

        aMap.clear();
        bMap.clear();

        for (int r = 0; r < size; r++) {
            aMap.put(r, a[r]);
            bMap.put(r, b[r]);
        }
    }

    @Override
    public void multiply() {
        IExecutorService exec = hz.getExecutorService("matmul_exec");

        int members = hz.getCluster().getMembers().size();
        int tasks = Math.max(1, Math.min(members, size));
        int rowsPerTask = (size + tasks - 1) / tasks;

        List<Future<RowBlockResult>> futures = new ArrayList<>(tasks);

        for (int t = 0; t < tasks; t++) {
            int startRow = t * rowsPerTask;
            int endRow = Math.min(size, startRow + rowsPerTask);
            if (startRow >= endRow) break;

            futures.add(exec.submit(new RowBlockTask(size, startRow, endRow)));
        }

        try {
            for (Future<RowBlockResult> f : futures) {
                RowBlockResult r = f.get();
                int rowCount = r.endRow - r.startRow;

                for (int i = 0; i < rowCount; i++) {
                    System.arraycopy(r.block[i], 0, c[r.startRow + i], 0, size);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Distributed MatMul failed", e);
        }
    }

    @Override
    public void clearC() {
        for (int i = 0; i < size; i++) {
            Arrays.fill(c[i], 0.0);
        }
    }

    @Override
    public double peek() {
        return c[0][0];
    }

    public static class RowBlockResult implements Serializable {
        public final int startRow;
        public final int endRow;
        public final double[][] block;

        public RowBlockResult(int startRow, int endRow, double[][] block) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.block = block;
        }
    }

    public static class RowBlockTask implements java.util.concurrent.Callable<RowBlockResult>, Serializable, HazelcastInstanceAware {

        private transient HazelcastInstance hz;

        private final int size;
        private final int startRow;
        private final int endRow;

        public RowBlockTask(int size, int startRow, int endRow) {
            this.size = size;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hz = hazelcastInstance;
        }

        @Override
        public RowBlockResult call() {
            IMap<Integer, double[]> aMap = hz.getMap(MAP_A);
            IMap<Integer, double[]> bMap = hz.getMap(MAP_B);

            double[][] bLocal = new double[size][];
            for (int k = 0; k < size; k++) {
                bLocal[k] = bMap.get(k);
            }

            int rows = endRow - startRow;
            double[][] block = new double[rows][size];

            for (int r = startRow; r < endRow; r++) {
                double[] aRow = aMap.get(r);
                double[] cRow = block[r - startRow];

                for (int j = 0; j < size; j++) {
                    double s = 0.0;
                    for (int k = 0; k < size; k++) {
                        s += aRow[k] * bLocal[k][j];
                    }
                    cRow[j] = s;
                }
            }

            return new RowBlockResult(startRow, endRow, block);
        }
    }
}