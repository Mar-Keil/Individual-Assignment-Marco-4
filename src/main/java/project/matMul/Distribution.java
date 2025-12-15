package project.matMul;

import java.util.Arrays;

public class Distribution implements IMatrix{

    private final int size;

    public Distribution(Rnd rnd, int size){
        this.size = size;
    }

    @Override
    public void multiply() {

    }

    @Override
    public void clearC(){
        for(int i = 0; i < size; i++){
            Arrays.fill(c[i], 0.0);
        }
    }

    @Override
    public double peek() {
        return c[0][0];
    }
}
