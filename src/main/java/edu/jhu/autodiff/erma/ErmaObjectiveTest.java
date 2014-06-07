package edu.jhu.autodiff.erma;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.autodiff.erma.MeanSquaredError.MeanSquaredErrorFactory;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.data.simple.AnnoSentenceReader;
import edu.jhu.data.simple.AnnoSentenceReader.AnnoSentenceReaderPrm;
import edu.jhu.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.maxent.LogLinearXY;
import edu.jhu.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.gm.maxent.LogLinearXYData;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.train.AvgBatchObjective;
import edu.jhu.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfTrainer.Trainer;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.JUnitUtils;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.JointNlpFgExamplesBuilder;
import edu.jhu.srl.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.util.collections.Lists;

public class ErmaObjectiveTest {
    
    @Test
    public void testSimpleGradient() {
        double[] params = new double[] {0.0, 0.0, 0.0, 0.0 };
 
        helpSimpleGradient(params, new ExpectedRecallFactory(), Trainer.ERMA, 
                new double[]{-0.125, 0.125, 0.125, -0.125});  
        helpSimpleGradient(params, new MeanSquaredErrorFactory(), Trainer.ERMA, 
                new double[]{-0.25, 0.25, 0.25, -0.25}); 
        helpSimpleGradient(params, null, Trainer.CLL, 
                new double[]{0.25, -0.25, -0.25, 0.25});
        
        params = new double[] {1.0, 2.0, 3.0, 4.0 };
        helpSimpleGradient(params, new ExpectedRecallFactory(), Trainer.ERMA, 
                new double[]{-0.0983, 0.0983, 0.0983, -0.0983}); 
        helpSimpleGradient(params, new MeanSquaredErrorFactory(), Trainer.ERMA, 
                new double[]{-0.2875, 0.2875, 0.1058, -0.1058}); 
        helpSimpleGradient(params, null, Trainer.CLL, 
                new double[]{0.3655, -0.3655, -0.1345, 0.1345});
    }

    private void helpSimpleGradient(double[] params, DlFactory dl, Trainer trainer, double[] expected) {
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
            exObj = new ErmaObjective(data, getErmaBpPrm(false), dl);
        } else {
            exObj = new CrfObjective(data, getErmaBpPrm(false));
        }
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        double[] grad = obj.getGradient(model.getParams()).toNativeArray();
        System.out.println(xyData.getFeatAlphabet());
        System.out.println(Arrays.toString(grad));
        System.out.println(DoubleArrays.toString(grad, "%.4f"));
        
        JUnitUtils.assertArrayEquals(expected, grad, 1e-4);
    }
    
    @Test
    public void testDpData() throws IOException {
        helpDpDataErma(new ExpectedRecallFactory());
        helpDpDataErma(new MeanSquaredErrorFactory());
    }

    private void helpDpDataErma(DlFactory dl) throws IOException {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);

        FgExampleList data = getDpData(ofc, 10);
        
        System.out.println("Num features: " + ofc.getNumParams());
        FgModel model = new FgModel(ofc.getNumParams());
        model.zero();
        
        ErmaObjective exObj = new ErmaObjective(data, getErmaBpPrm(false), dl);
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);

        System.out.println(DoubleArrays.toString(obj.getGradient(model.getParams()).toNativeArray(), "%.4g"));
                
        model.setRandomStandardNormal();        
        ModuleTestUtils.assertFdAndAdEqual(obj, model.getParams(), 1e-5, 1e-8);
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

    @Test
    public void testDpDataOnCrfObjective() throws IOException {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);

        FgExampleList data = getDpData(ofc, 10);
        
        System.out.println("Num features: " + ofc.getNumParams());
        FgModel model = new FgModel(ofc.getNumParams());
        model.setRandomStandardNormal();

        CrfObjective exObj = new CrfObjective(data, getErmaBpPrm(false));
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        ModuleTestUtils.assertFdAndAdEqual(obj, model.getParams(), 1e-5, 1e-8);
    }
    
    public static FgExampleList getDpData(ObsFeatureConjoiner ofc, int featureHashMod) throws IOException {
        AnnoSentenceReaderPrm rPrm = new AnnoSentenceReaderPrm();
        rPrm.maxNumSentences = 3;
        rPrm.maxSentenceLength = 7;
        rPrm.useCoNLLXPhead = true;
        AnnoSentenceReader r = new AnnoSentenceReader(rPrm);
        //r.loadSents(CrfObjectiveTest.class.getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example), DatasetType.CONLL_2009);
        r.loadSents(new File("/Users/mgormley/research/pacaya/data/conllx/CoNLL-X/train/data/bulgarian/bultreebank/train/bulgarian_bultreebank_train.conll"), DatasetType.CONLL_X);
        
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        AnnoSentenceCollection sents = r.getData();
        cs.init(sents);
        
        JointNlpFgExampleBuilderPrm prm = new JointNlpFgExampleBuilderPrm();
        prm.fgPrm.includeSrl = false;
        prm.fgPrm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.fgPrm.dpPrm.useProjDepTreeFactor = true;
        prm.exPrm.cacheType = CacheType.NONE;
        prm.fePrm.dpFePrm.featureHashMod = featureHashMod;
        
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm, ofc, cs);
        FgExampleList data = builder.getData(sents);
        return data;
    }
    
}
