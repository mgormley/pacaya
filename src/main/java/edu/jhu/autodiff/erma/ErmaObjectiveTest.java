package edu.jhu.autodiff.erma;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.train.AvgBatchObjective;
import edu.jhu.gm.train.CrfObjectiveTest;

public class ErmaObjectiveTest {
    
    @Test
    public void testDpData() throws IOException {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);

        FgExampleList data = CrfObjectiveTest.getDpData(ofc);
        
        System.out.println("Num features: " + ofc.getNumParams());
        FgModel model = new FgModel(ofc.getNumParams());
        model.setRandomStandardNormal();

        ExpectedRecallFactory dl = new ExpectedRecallFactory();
        ErmaObjective exObj = new ErmaObjective(data, getErmaBpPrm(false), dl);
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        ModuleTestUtils.assertFdAndAdEqual(obj, model.getParams(), 1e-8, 1e-8);
        //assertEquals(15.27, obj.getValue(model.getParams()), 1e-2);
        //assertEquals(1, obj.getGradient(model.getParams()).get(0), 1e-13);
    }


    public static ErmaBpPrm getErmaBpPrm(boolean logDomain) {
        ErmaBpPrm bpPrm = new ErmaBpPrm();
        bpPrm.logDomain = logDomain;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }
}
