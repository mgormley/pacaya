package edu.jhu.util.matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.sparse.FlexCompColMatrix;

import org.junit.Test;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import edu.emory.mathcs.csparsej.tdouble.Dcs_multiply;
import edu.emory.mathcs.csparsej.tdouble.Dcs_common.Dcs;
import edu.jhu.gridsearch.dr.DimReducer;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.tuple.OrderedPair;
import edu.jhu.util.tuple.PairSampler;

public class MatrixMultComparisonTest {

    // MTJ just crawls to a halt on this multiplication.
    //@Test
    public void testMatrixMultMtj() {
        int rowsS = 300;
        int colsS = 10000;

        int rowsA = 10000;
        int colsA = 9000;

        double propNonZero = 0.1;

        // MTJ
        Timer timer = new Timer();
        timer.start();
        System.out.println("Building S");
        DenseMatrix S = new DenseMatrix(rowsS, colsS);
        for (int i = 0; i < rowsS; i++) {
            for (int j = 0; j < colsS; j++) {
                S.set(i, j, Prng.nextDouble());
            }
        }
        System.out.println("Building A");
        FlexCompColMatrix A = new FlexCompColMatrix(rowsA, colsA);
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                if (Prng.nextDouble() < propNonZero) {
                    A.set(i, j, Prng.nextDouble());
                }
            }
        }
        System.out.println("Building SA");
        DenseMatrix SA = new DenseMatrix(rowsS, colsA);
        System.out.println("Multiplying S and A");
        S.mult(A, SA);
        timer.stop();

        System.out.println("MTJ: " + timer.totMs());
    }

    //@Test
    public void testMatrixMultCSparseJ() {
        // CSparseJ
        // TODO: I'm not even sure how to construct a matrix with this library.
        Dcs dcs1 = new Dcs();
        Dcs dcs2 = new Dcs();
        Dcs dcs3 = Dcs_multiply.cs_multiply(dcs1, dcs2);
    }

    // Output is "Parallel COLT: 6440.0"
    //@Test
    public void testMatrixMultColt() {
        if (true) {
            // pass.
            //return;
        }
        // For 500 sentences, we have as input 82130 variables, 144983 constraints, or 312202 factors. 
        // Also has 1263952 nonzeros in the presolved matrix.
        int rowsS = 300;
        int colsS = 144983; // 312202 factors and 
        int rowsA = colsS;
        int colsA = 82130;
        int numNonZeros = 1263952;
        double propNonZero = 2.0 * numNonZeros / ((double)rowsA * colsA);
        boolean sample = false;
        
        System.out.println(String.format("%.13f",propNonZero));

        // Colt
        System.out.println("Building S");
        DenseDoubleMatrix2D S = new DenseDoubleMatrix2D(rowsS, colsS);
        for (int i = 0; i < rowsS; i++) {
            for (int j = 0; j < colsS; j++) {
                S.set(i, j, Prng.nextDouble());
            }
        }
        
        SparseCCDoubleMatrix2D A = new SparseCCDoubleMatrix2D(rowsA, colsA);
        if (sample) {
            System.out.println("Sampling pairs for A");
            Collection<OrderedPair> pairs = PairSampler.sampleOrderedPairs(0, colsA, 0, rowsA, propNonZero);   
            System.out.println("Sorting pairs for A");
            Collections.sort(new ArrayList<OrderedPair>(pairs));
            System.out.println("Building A");
            for (OrderedPair pair : pairs) {
                A.set(pair.get2(), pair.get1(), Prng.nextDouble());
            }
        } else {
            System.out.println("Building A");
            int count = 0;
            int stride = (int) (1.0 / propNonZero);
            // Note that these matrices must be built column-wise.
            for (int j = 0; j < colsA; j++) {
                for (int i = 0; i < rowsA; i++) {
                    // if (Prng.nextDouble() < propNonZero) {
                    if ((j * colsA + i) % stride == 0) {
                        A.set(i, j, i * j % 7);
                        count++;
                    }
                }
            }
            System.out.println("Num non zeros: " + count);
        }

        System.out.println("Building SA");

        Timer timer = new Timer();
        timer.start();
        System.out.println("Multiplying S and A");
        DimReducer.fastMultiply(S, A);
        timer.stop();

        System.out.println("Parallel COLT: " + timer.totMs());
    }

}
