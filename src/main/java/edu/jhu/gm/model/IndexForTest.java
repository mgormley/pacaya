package edu.jhu.gm.model;

import static edu.jhu.gm.model.IndexForVcTest.getVar;

import java.util.Arrays;

import org.junit.Test;

public class IndexForTest {

    @Test
    public void testGetState() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);

        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        vars1.add(v2);


        VarSet vars2 = new VarSet();
        vars2.add(v1);
        vars2.add(v2);

        IndexFor iter = new IndexFor(vars1, vars2);
        
        // TODO: This doesn't actually test anything.
        while (iter.hasNext()) {
            System.out.println(Arrays.toString(iter.getState()));
            iter.next();
        }
    }
    
}
