package edu.jhu.gm;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.vector.IntDoubleEntry;

public class CrfObjectiveTest {
    
    /**
     * A description of a collection of identical log-linear model examples.
     * 
     * @author mgormley
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

        private Alphabet<Feature> alphabet = new Alphabet<Feature>();
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
            FgExamples data = new FgExamples(alphabet);
            int state=0;
            for (LogLinearExDesc desc : descList) {
                for (int i=0; i<desc.getCount(); i++) {
                    FactorGraph fg = new FactorGraph();
                    final Var v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
                    Factor f0 = new Factor(new VarSet(v0));
                    fg.addFactor(f0);
                    FeatureExtractor featExtractor = new FeatureExtractor() {
                        @Override
                        public FeatureVector calcFeatureVector(int factorId,
                                VarConfig varConfig) {
                            return descList.get(varConfig.getState(v0)).getFeatures();
                        }
                    };
                    VarConfig trainConfig = new VarConfig();
                    trainConfig.put(v0, state);
                    data.add(new FgExample(fg, trainConfig, featExtractor));
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
    public void testLogLinearModelShapes() {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{3.0, 2.0};

        // Test log-likelihood.
        CrfObjective obj = new CrfObjective(2, exs.getData(), getInfFactory());
        double ll = obj.getValue(params);
        System.out.println(ll);
        assertEquals(-95.531, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertArrayEquals(new int[]{0, 1}, obsFeats.getIndices());
        JUnitUtils.assertArrayEquals(new double[]{45, 40}, obsFeats.getValues(), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertArrayEquals(new int[]{0, 1}, expFeats.getIndices());
        JUnitUtils.assertArrayEquals(new double[]{57.15444760934599, 52.84782467867294}, expFeats.getValues(), 1e-13);
        
        // Test gradient.        
        double[] gradient = obj.getGradient(params);        
        JUnitUtils.assertArrayEquals(new double[]{-12.154447609345993, -12.847824678672943}, gradient, 1e-10);
    }

    @Test
    public void testLogLinearModelShapesOneExample() {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(1, "circle");
        exs.addEx(0, "solid");
        
        CrfObjective obj = new CrfObjective(2, exs.getData(), getInfFactory());
        double ll = obj.getValue(new double[]{3.0, 2.0});
        System.out.println(ll);
        assertEquals(3*1 - Math.log(Math.exp(3*1) + Math.exp(2*1)), ll, 1e-2);
    }
    
    public FgInferencerFactory getInfFactory() {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = true;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        return bpPrm;
    }
    
}
