package edu.jhu.gm;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;
import edu.jhu.gm.Var.VarType;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.SrlFgExamplesBuilder;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;

public class CrfObjectiveTest {
    
    /**
     * A description of a collection of identical log-linear model examples.
     * 
     * @author mgormley
     * @author mmitchell
     */
    public static class LogLinearExDesc {
        private int count;
        private FeatureVector features;
        public LogLinearExDesc(int count, FeatureVector features) {
            this.count = count;
            this.features = features;
        }
        public int getCount() {
            return count;
        }
        public FeatureVector getFeatures() {
            return features;
        }         
    }
    
    /**
     * A factor for FgExamples constructed from LogLinearExDesc objects.
     * @author mgormley
     */
    public static class LogLinearEDs {

        private static final Object TEMPLATE_KEY = "loglin";
        private final Alphabet<Feature> alphabet = new Alphabet<Feature>();
        private ArrayList<LogLinearExDesc> descList = new ArrayList<LogLinearExDesc>();

        public void addEx(int count, String... featNames) {
            FeatureVector features = new FeatureVector();
            for (String featName : featNames) {
                features.put(alphabet.lookupIndex(new Feature(featName)), 1.0);
            }
            LogLinearExDesc ex = new LogLinearExDesc(count, features);
            descList.add(ex);
        }
        
        public List<String> getStateNames() {
            List<String> names = new ArrayList<String>();
            for (LogLinearExDesc desc : descList) {
                StringBuilder sb = new StringBuilder();
                for (IntDoubleEntry entry : desc.getFeatures()) {
                    sb.append(entry.index());
                    sb.append(":");
                }
                names.add(sb.toString());
            }
            return names;
        }
        
        public FgExamples getData() {
            FeatureTemplateList fts = new FeatureTemplateList();
            Var v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
            fts.add(new FeatureTemplate(new VarSet(v0), alphabet, TEMPLATE_KEY));
            
            FgExamples data = new FgExamples(fts);
            int state=0;
            for (final LogLinearExDesc desc : descList) {
                for (int i=0; i<desc.getCount(); i++) {
                    final VarConfig trainConfig = new VarConfig();
                    trainConfig.put(v0, state);
                    
                    FactorGraph fg = new FactorGraph();
                    v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
                    ExpFamFactor f0 = new ExpFamFactor(new VarSet(v0), TEMPLATE_KEY);
                    fg.addFactor(f0);
                    ObsFeatureExtractor featExtractor = new ObsFeatureExtractor() {
                        @Override
                        public FeatureVector calcObsFeatureVector(int factorId) {
                            // TODO: This doesn't do the right thing...we
                            // actually want features of the predicted state,
                            // which isn't possible to set when only looking at
                            // the observations.
                            // Instead we need to be aware of the VarConfig of the predicted vars.
                            return desc.getFeatures();
                        }
                        public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred,
                                VarConfig goldConfig, FeatureTemplateList fts) {             
                            // Do nothing.               
                        }
                        public void clear() {
                            // Do nothing.
                        }
                    };
                    data.add(new FgExample(fg, trainConfig, featExtractor, fts));
                }
                state++;
            }
            return data;
        }

        public Alphabet<Feature> getAlphabet() {
            return alphabet;
        }
    }
    
    @Test
    public void testLogLinearModelShapesLogProbs() {
        // Test with inference in the log-domain.
        boolean logDomain = true;        
        testLogLinearModelShapesHelper(logDomain);
    }
    
    @Test
    public void testLogLinearModelShapesProbs() {
        // Test with inference in the prob-domain.
        boolean logDomain = false;        
        testLogLinearModelShapesHelper(logDomain);
    }

    private void testLogLinearModelShapesHelper(boolean logDomain) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(exs.getData().getTemplates());
        model.updateModelFromDoubles(params);
        
        // Test log-likelihood.
        CrfObjective obj = new CrfObjective(model, exs.getData(), getInfFactory(logDomain));
        obj.setPoint(params);
        
        // Test log-likelihood.
        double ll = obj.getValue();
        System.out.println(ll);
        assertEquals(-95.531, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertEquals(45, obsFeats.get(0), 1e-13);
        assertEquals(40, obsFeats.get(1), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertEquals(57.15444760934599, expFeats.get(0), 1e-3);
        assertEquals(52.84782467867294, expFeats.get(1), 1e-3);
        
        // Test gradient.        
        double[] gradient = new double[params.length]; 
        obj.getGradient(gradient);        
        JUnitUtils.assertArrayEquals(new double[]{-12.154447609345993, -12.847824678672943}, gradient, 1e-3);
    }
    
    @Test
    public void testLogLinearModelShapesOneExampleLogProbs() {
        boolean logDomain = true;
        testLogLinearModelShapesOneExampleHelper(logDomain);
    }

    @Test
    public void testLogLinearModelShapesOneExampleProbs() {
        boolean logDomain = false;
        testLogLinearModelShapesOneExampleHelper(logDomain);
    }
    
    private void testLogLinearModelShapesOneExampleHelper(boolean logDomain) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(1, "circle");
        exs.addEx(0, "solid");
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(exs.getData().getTemplates());
        model.updateModelFromDoubles(params);
        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(logDomain); 
        infFactory = getInfFactory(logDomain);
        CrfObjective obj = new CrfObjective(model, exs.getData(), infFactory);
        obj.setPoint(params);        
        
        assertEquals(2, exs.getAlphabet().size());

        // Test log-likelihood.
        double ll = obj.getValue();
        System.out.println(ll);
        assertEquals(3*1 - Math.log(Math.exp(3*1) + Math.exp(2*1)), ll, 1e-2);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertEquals(1, obsFeats.get(0), 1e-13);
        assertEquals(0, obsFeats.get(1), 1e-13);        
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertEquals(0.7310, expFeats.get(0), 1e-3);
        assertEquals(0.2689, expFeats.get(1), 1e-3);
        
        // Test gradient.         
        double[] gradient = new double[params.length]; 
        obj.getGradient(gradient);        
        JUnitUtils.assertArrayEquals(new double[]{0.2689, -0.2689}, gradient, 1e-3);
    }
    
    @Test
    public void testSrlLogLikelihood() throws Exception {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", new ArrayList<String>()));
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
//        tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "subj", "_", false, "_", getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "_", "V", "_", getList("feat"), getList("feat") , 0, 0, "v", "_", true, "ate.1", getList("_")));
        //tokens.add(new CoNLL09Token(4, "food", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
                
        System.out.println("Done reading.");
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        List<SimpleAnnoSentence> sents = getList(sent.toSimpleAnnoSentence(csPrm));
        cs.init(sents);
        
        FeatureTemplateList fts = new FeatureTemplateList();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fgPrm.makeUnknownPredRolesLatent = false;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.fePrm.biasOnly = true;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExamples data = builder.getData(sents);
        
        System.out.println("Num features: " + fts.getNumObsFeats());
        FgModel model = new FgModel(fts);

        boolean logDomain = false;
        FgInferencerFactory infFactory = getInfFactory(logDomain);        
        FgExample ex = data.get(0);
        
        FgInferencer infLat = infFactory.getInferencer(ex.getFgLat());
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();        
        assertEquals(2, infLat.getPartition(), 2);
        // Check that the partition function is computed identically for each variable.
        for (Var v : fgLat.getVars()) {
            double partition = ((BeliefPropagation)infLat).getPartitionFunctionAtVarNode(fgLat.getNode(v));
            assertEquals(2, logDomain ? Utilities.exp(partition) : partition, 1e-3);
        }
        
        System.out.println("-------- Running LatPred Inference-----------");
        
        FgInferencer infLatPred = infFactory.getInferencer(ex.getFgLatPred());
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();        
        // 2 trees, and 3 different roles (including argUNK)
        assertEquals(2*3, infLatPred.getPartition(), 2);         

        // Print schedule:
        BfsBpSchedule schedule = new BfsBpSchedule(fgLatPred);        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        System.out.println();
        // Print factors
        for (Factor f : fgLatPred.getFactors()) {
            System.out.println(f);
        }
        // Check that the partition function is computed identically for each variable.
        for (Var v : fgLatPred.getVars()) {
            double partition = ((BeliefPropagation)infLatPred).getPartitionFunctionAtVarNode(fgLatPred.getNode(v));
            System.out.format("Var=%s partition=%.4f\n", v.toString(), partition);
            assertEquals(2*3, logDomain ? Utilities.exp(partition) : partition, 1e-3);
        }
        
        CrfObjective obj = new CrfObjective(model, data, infFactory);
        obj.setPoint(FgModelTest.getParams(model));
        double ll = obj.getValue();        
        assertEquals(2./6., Utilities.exp(ll), 1e-13);
    }

    public FgInferencerFactory getInfFactory(boolean logDomain) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = logDomain;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }
    
}
