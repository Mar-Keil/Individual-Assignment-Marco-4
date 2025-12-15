package project.matMul;

import com.hazelcast.core.HazelcastInstance;
import project.Rnd;

import java.util.Arrays;

public class Distribution implements IMatrix {

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
    }

    @Override
    public void multiply() {
        // kommt in Schritt 2
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
}