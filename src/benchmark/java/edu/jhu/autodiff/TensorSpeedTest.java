package edu.jhu.autodiff;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.DimIter;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.Timer;


public class TensorSpeedTest {

    private Algebra s = new RealAlgebra();
    
    @Test
    public void testSpeedOfIndexOperations() {
        Tensor[] arr = new Tensor[1000];
        int[] dims = new int[]{31, 5, 7, 11};
        for (int i=0; i<arr.length; i++) {
            arr[i] = new Tensor(s, dims);
        }
        {
            Timer timer = new Timer();
            timer.start();
            for (int i=0; i<arr.length; i++) {
                Tensor tensor = arr[i];                
                for (int c=0; c < tensor.size(); c++) {
                    tensor.addValue(c, c);
                }
            }
            timer.stop();
            System.out.println("tot(ms) direct iteration: " + timer.totMs());
        }
        {
            Timer timer = new Timer();
            timer.start();
            for (int i=0; i<arr.length; i++) {
                Tensor tensor = arr[i];  
                DimIter iter = new DimIter(tensor.getDims());                
                //for (int c=0; c < tensor.size(); c++) {
                int c = 0;
                while (iter.hasNext()) {
                    tensor.add(c++, iter.next());
                }
            }
            timer.stop();
            System.out.println("tot(ms) index iteration: " + timer.totMs());
        }
    }

}
