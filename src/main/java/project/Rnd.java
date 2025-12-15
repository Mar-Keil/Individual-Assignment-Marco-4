package project;

import java.util.Random;

public class Rnd {

    private final Random rnd;

    public Rnd(){
        this.rnd = new Random();
    }

    public double[] fill(int size) {
        double[] line = new double[size];
        for (int i = 0; i < size; i++) {
            line[i] = rnd.nextDouble();
        }
        return line;
    }
}

