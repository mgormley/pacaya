package edu.jhu.pacaya.autodiff.erma;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.pacaya.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.pacaya.autodiff.erma.MeanSquaredError.MeanSquaredErrorFactory;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.maxent.LogLinearXY;
import edu.jhu.pacaya.gm.maxent.LogLinearXYData;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.train.AvgBatchObjective;
import edu.jhu.pacaya.gm.train.CrfObjective;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.pacaya.gm.train.CrfTrainer.Trainer;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;

public class ErmaObjectiveTest {
    
    @Test
    public void testSimpleGradient() {
        testSimpleGradient(RealAlgebra.getInstance());
        testSimpleGradient(LogSignAlgebra.getInstance());
    }
    
    private void testSimpleGradient(Algebra s) {
        double[] params = new double[] {0.0, 0.0, 0.0, 0.0 };
 
        helpSimpleGradient(params, new ExpectedRecallFactory(), Trainer.ERMA, 
                new double[]{-0.125, 0.125, 0.125, -0.125}, s);  
        helpSimpleGradient(params, new MeanSquaredErrorFactory(), Trainer.ERMA, 
                new double[]{-0.25, 0.25, 0.25, -0.25}, s); 
        helpSimpleGradient(params, null, Trainer.CLL, 
                new double[]{0.25, -0.25, -0.25, 0.25}, s);
        
        params = new double[] {1.0, 2.0, 3.0, 4.0 };
        helpSimpleGradient(params, new ExpectedRecallFactory(), Trainer.ERMA, 
                new double[]{-0.0983, 0.0983, 0.0983, -0.0983}, s); 
        helpSimpleGradient(params, new MeanSquaredErrorFactory(), Trainer.ERMA, 
                new double[]{-0.2875, 0.2875, 0.1058, -0.1058}, s); 
        helpSimpleGradient(params, null, Trainer.CLL, 
                new double[]{0.3655, -0.3655, -0.1345, 0.1345}, s);
    }

    private void helpSimpleGradient(double[] params, DlFactory dl, Trainer trainer, double[] expected, Algebra s) {
        LogLinearXYData xyData = new LogLinearXYData();
        List<String>[] fvs;
        fvs = new List[]{ Lists.getList("x=A,y=A"), Lists.getList("x=A,y=B") };
        xyData.addExStrFeats(1.0, "x=A", "y=A", fvs);
        fvs = new List[]{ Lists.getList("x=B,y=A"), Lists.getList("x=B,y=B") };
        xyData.addExStrFeats(1.0, "x=B", "y=B", fvs);        
        LogLinearXY xy = new LogLinearXY(new LogLinearXYPrm());
        FgExampleList data = xy.getData(xyData);
                
        FgModel model = new FgModel(xyData.getFeatAlphabet().size());
        model.updateModelFromDoubles(params);

        ExampleObjective exObj;
        if (trainer == Trainer.ERMA) {
            exObj = new ErmaObjective(data, getErmaBpPrm(s), dl);
        } else {
            exObj = new CrfObjective(data, getErmaBpPrm(s));
        }
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        double[] grad = obj.getGradient(model.getParams()).toNativeArray();
        System.out.println(xyData.getFeatAlphabet());
        System.out.println(Arrays.toString(grad));
        System.out.println(DoubleArrays.toString(grad, "%.4f"));
        
        JUnitUtils.assertArrayEquals(expected, grad, 1e-4);
    }

    public static ErmaBpPrm getErmaBpPrm(Algebra s) {
        ErmaBpPrm bpPrm = new ErmaBpPrm();
        bpPrm.s = s;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }    
    
}
