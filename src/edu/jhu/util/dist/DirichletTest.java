package edu.jhu.util.dist;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import edu.jhu.util.math.Vectors;


public class DirichletTest {

    @Test
    public void testSmallDraw() {
        // Test small draw.
        Dirichlet dir = new Dirichlet(new double[]{0.1, 0.1, 0.1});
        double[] props = dir.draw();
        System.out.println(Arrays.toString(props));
        Assert.assertEquals(1.0, Vectors.sum(props), 1e-13);
    }
    

    @Test
    public void testLargeDraw() {
        Dirichlet dir = new Dirichlet(0.01, 1000);
        double[] props = dir.draw();
        System.out.println(Arrays.toString(props));
        Assert.assertEquals(1.0, Vectors.sum(props), 1e-13);
    }
    

    @Test
    public void testLargeUniform() {
        Dirichlet dir = new Dirichlet(1000, 100);
        double[] props = dir.draw();
        System.out.println(Arrays.toString(props));
        Assert.assertEquals(1.0, Vectors.sum(props), 1e-13);
    }
    
}
