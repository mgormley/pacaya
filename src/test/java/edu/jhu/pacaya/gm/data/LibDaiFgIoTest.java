package edu.jhu.pacaya.gm.data;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import org.junit.Test;

import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.stanford.nlp.io.StringOutputStream;


public class LibDaiFgIoTest {

    @Test
    public void testWriteFactorGraphBufferedWriter() throws Exception {
        FactorGraph fg = FactorGraphsForTests.getOneVarFg();
        
        StringOutputStream sos = new StringOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sos));
        LibDaiFgIo.write(fg, out);
        String fgfile = sos.toString();
        System.out.println(fgfile);
        String expected = "# Num factors = 1, Num variables = 1, Num edges = 2\n"
                + "1\n"
                + "\n"
                + "1\n"
                + "0\n"
                + "2\n"
                + "2\n"
                + "0 0.09531017980432493\n"
                + "1 0.6418538861723947\n\n";
        assertEquals(expected, fgfile);        
    }
    
    @Test
    public void testWriteFactorGraphBufferedWriter2() throws Exception {
        FactorGraph fg = FactorGraphsForTests.getLinearChainGraph();
        
        StringOutputStream sos = new StringOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sos));
        LibDaiFgIo.write(fg, out);
        String fgfile = sos.toString();
        System.out.println(fgfile);
        String expected = "# Num factors = 5, Num variables = 3, Num edges = 14\n"
                + "5\n"
                + "\n"
                + "1\n"
                + "0\n"
                + "2\n"
                + "2\n"
                + "0 -2.3025850929940455\n"
                + "1 -0.10536051565782628\n"
                + "\n"
                + "1\n"
                + "1\n"
                + "2\n"
                + "2\n"
                + "0 -1.2039728043259361\n"
                + "1 -0.35667494393873245\n"
                + "\n"
                + "1\n"
                + "2\n"
                + "2\n"
                + "2\n"
                + "0 -0.6931471805599453\n"
                + "1 -0.6931471805599453\n"
                + "\n"
                + "2\n"
                + "1 0\n"
                + "2 2\n"
                + "4\n"
                + "0 -1.6094379124341003\n"
                + "1 -0.916290731874155\n"
                + "2 -1.2039728043259361\n"
                + "3 -0.6931471805599453\n"
                + "\n"
                + "2\n"
                + "2 1\n"
                + "2 2\n"
                + "4\n"
                + "0 0.1823215567939546\n"
                + "1 0.3364722366212129\n"
                + "2 0.26236426446749106\n"
                + "3 0.4054651081081644\n"
                + "\n";
        assertEquals(expected, fgfile);        
    }

}
