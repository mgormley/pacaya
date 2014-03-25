package edu.jhu.gm.maxent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.gm.maxent.LogLinearXYData.LogLinearExample;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.JUnitUtils;

public class LogLinearXYTest {
    
    @Test
    public void testLogLinearModelTrainDecode2() {
        LogLinearXYData exs = new LogLinearXYData(2);
        exs.getFeatAlphabet().lookupIndex("BIAS");
        exs.getFeatAlphabet().lookupIndex("circle");
        exs.getFeatAlphabet().lookupIndex("solid");
        exs.getXAlphabet().lookupIndex("x=0");
        exs.getYAlphabet().lookupIndex("y=A");
        exs.getYAlphabet().lookupIndex("y=B");
        //exs.getYAlphabet().lookupIndex("y=C");
        //exs.getYAlphabet().lookupIndex("y=D");
        
        FeatureVector[] fvs = new FeatureVector[4];
        for (int i=0; i<4; i++) {
            fvs[i] = new FeatureVector();            
        }
        fvs[0].add(0, 1);
        fvs[0].add(1, 1);
        fvs[0].add(2, 1);
        fvs[1].add(0, 1);
        fvs[1].add(1, 1);
        fvs[2].add(0, 1);
        fvs[2].add(2, 1);
        fvs[3].add(0, 1);
        fvs[3].add(1, 1);
        exs.addEx(30, "x=0", "y=A", Arrays.copyOfRange(fvs, 0, 2));
        exs.addEx(15, "x=0", "y=B", Arrays.copyOfRange(fvs, 0, 2));
        exs.addEx(5, "x=1", "y=A", Arrays.copyOfRange(fvs, 2, 4));
        exs.addEx(21,  "x=1", "y=B", Arrays.copyOfRange(fvs, 2, 4));
                
        List<LogLinearExample> data = exs.getData();
        
        LogLinearXYPrm prm = new LogLinearXYPrm();
        LogLinearXY td = new LogLinearXY(prm); 
        FgModel model = td.train(exs);
        System.out.println(model);
        {
            Pair<String,DenseFactor> p = td.decode(model, data.get(0));
            String predLabel = p.get1();
            DenseFactor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=A", predLabel);
            JUnitUtils.assertArrayEquals(new double[] {-0.56869180843596, -0.8353232329420138
                    }, dist.getValues(), 1e-3);
        }
        {
            Pair<String,DenseFactor> p = td.decode(model, data.get(2));
            String predLabel = p.get1();
            DenseFactor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=B", predLabel);
            JUnitUtils.assertArrayEquals(new double[] {-0.8277652102139692, -0.5745197983563534 }, dist.getValues(), 1e-3);
        }
    }
    
}
