package edu.jhu.gm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.Test;

import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.sort.DoubleSort;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.prim.util.random.Prng;
import edu.jhu.util.JUnitUtils;

public class FgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            // Just test that no exception is thrown.
            FgModel model = new FgModel(10);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(model);
            out.close();
        } catch(java.io.NotSerializableException e) {
            e.printStackTrace();
            fail("FgModel is not serializable: " + e.getMessage());
        }
    }
    
    @Test
    public void testApply() {
        int numParams = (3*2)*2 + 2*1;
        FgModel model = new FgModel(numParams);
        
        assertEquals(numParams, model.getNumParams());
        
        final MutableInt x = new MutableInt(0);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(0.0, v, 1e-13);
                x.v++;
                return 1.0;
            }
        });
        
        assertEquals(numParams, x.v);
        
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(1.0, v, 1e-13);
                return 1.0;
            }
        });
    }

    @Test
    public void testFillAndZero() {
        int numParams = (3*2)*2 + 2*1;
        FgModel model = new FgModel(numParams);
        model.fill(1.0);

        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(1.0, v, 1e-13);
                return v;
            }
        });
        
        model.zero();
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(0.0, v, 1e-13);
                return 1.0;
            }
        });
    }

    @Test
    public void testSetRandomStandardNormal() {
        Prng.seed(1l);

        int numParams = (3*2)*2 + 2*1;
        FgModel model = new FgModel(numParams);
        model.setRandomStandardNormal();

        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);
        System.out.println("sum: " + DoubleArrays.sum(params));
        assertEquals(-2.0817045546109862, DoubleArrays.sum(params), 1e-3);
    }

    @Test
    public void testUpdateDoublesAndModel() {
        int numParams = (3*2)*2 + 2*1;
        FgModel model = new FgModel(numParams);
        final MutableInt x = new MutableInt(0);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                x.v++;
                return x.v;
            }
        });
        
        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);
        JUnitUtils.assertArrayEquals(new double[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14}, params, 1e-13);
        
        DoubleSort.sortDesc(params);
        
        model.updateModelFromDoubles(params);
        
        final MutableInt y = new MutableInt(14);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(y.v, v, 1e-13);
                y.v--;
                return v;
            }
        });

    }

    public static double[] getParams(FgModel model) {
        return model.getParams().toNativeArray();
    }
    
}
