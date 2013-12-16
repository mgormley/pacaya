package edu.jhu.gm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.Test;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.SlowObsFeatureExtractor;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.sort.DoubleSort;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.srl.MutableInt;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Prng;
import edu.jhu.util.collections.Lists;

public class FgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            FactorTemplateList fts = getFtl();
            // Just test that no exception is thrown.
            FgModel model = new FgModel(fts);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(model);
            out.close();
        } catch(java.io.NotSerializableException e) {
            e.printStackTrace();
            fail("FgModel is not serializable: " + e.getMessage());
        }
    }

    @Test
    public void testNumParams() {
        FactorTemplateList fts = getFtl();
        FgModel model = new FgModel(fts);
        assertEquals((3*2)*2 + 2*1, model.getNumParams());
    }
    
    @Test
    public void testApply() {
        FactorTemplateList fts = getFtl();
        FgModel model = new FgModel(fts);
        
        assertEquals(3*2*2+2*1, model.getNumParams());
        
        final MutableInt x = new MutableInt(0);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(0.0, v, 1e-13);
                x.increment();
                return 1.0;
            }
        });
        
        assertEquals(3*2*2+2*1, x.get());
        
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(1.0, v, 1e-13);
                return 1.0;
            }
        });
    }

    @Test
    public void testFillAndZero() {
        FactorTemplateList fts = getFtl();
        FgModel model = new FgModel(fts);
        model.fill(1.0);

        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(1.0, v, 1e-13);
                return v;
            }
        });
        
        model.zero();
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(0.0, v, 1e-13);
                return 1.0;
            }
        });
    }

    @Test
    public void testSetRandomStandardNormal() {
        Prng.seed(1l);
        
        FactorTemplateList fts = getFtl();
        FgModel model = new FgModel(fts);
        model.setRandomStandardNormal();

        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);
        System.out.println("sum: " + DoubleArrays.sum(params));
        assertEquals(-2.0817045546109862, DoubleArrays.sum(params), 1e-3);
    }

    @Test
    public void testUpdateDoublesAndModel() {
        FactorTemplateList fts = getFtl();
        FgModel model = new FgModel(fts);
        final MutableInt x = new MutableInt(0);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                x.increment();
                return x.get();
            }
        });
        
        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);
        JUnitUtils.assertArrayEquals(new double[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14}, params, 1e-13);
        
        DoubleSort.sortDesc(params);
        
        model.updateModelFromDoubles(params);
        
        final MutableInt y = new MutableInt(14);
        model.apply(new LambdaUnaryOpDouble() {
            public double call(double v) {
                assertEquals(y.get(), v, 1e-13);
                y.decrement();
                return v;
            }
        });

    }

    @Test
    public void testExcludeUnsupportedFeaturesWithLatentVars() {
        boolean useLat = true;
        FactorTemplateList fts = getFtl(useLat);
        
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        data.add(getExForFts("1a", "2a", fts, useLat));
        data.add(getExForFts("1a", "2c", fts, useLat));
        data.add(getExForFts("1b", "2b", fts, useLat));
        data.add(getExForFts("1b", "2c", fts, useLat));
        
        FgModel model1 = new FgModel(data, true);        
        System.out.println("\n"+model1);
        assertEquals(20, model1.getNumParams());
        
        FgModel model2 = new FgModel(data, false);        
        System.out.println("\n"+model2);
        // 6 bias features, and 6 other features.
        assertEquals(6+6, model2.getNumParams());
    }
    
    @Test
    public void testExcludeUnsupportedFeatures() {
        boolean useLat = false;
        FactorTemplateList fts = getFtl(useLat);
        
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        data.add(getExForFts("1a", "2a", fts, useLat));
        data.add(getExForFts("1a", "2c", fts, useLat));
        data.add(getExForFts("1b", "2b", fts, useLat));
        data.add(getExForFts("1b", "2c", fts, useLat));
        
        FgModel model1 = new FgModel(data, true);        
        System.out.println("\n"+model1);
        assertEquals(20, model1.getNumParams());
        
        FgModel model2 = new FgModel(data, false);        
        System.out.println("\n"+model2);
        // 6 bias features, and 4 other features.
        assertEquals(6+4, model2.getNumParams());
    }

    public static class MockFeatureExtractor extends SlowObsFeatureExtractor {

        public MockFeatureExtractor() {
            super();
        }
        
        @Override
        public FeatureVector calcObsFeatureVector(int factorId, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();
            Alphabet<Feature> alphabet = fts.getTemplate(fg.getFactor(factorId)).getAlphabet();

            int featIdx = alphabet.lookupIndex(new Feature("BIAS_FEATURE", true));
            fv.set(featIdx, 1.0);
            featIdx = alphabet.lookupIndex(new Feature("feat2a"));
            fv.set(featIdx, 1.0);
            
            return fv;
        }
    }
    
    private FgExample getExForFts(String state1, String state2, FactorTemplateList fts, boolean useLat) {
        Var v1 = new Var(VarType.PREDICTED, 2, "1", Lists.getList("1a", "1b"));
        Var v2 = new Var(useLat ? VarType.LATENT : VarType.PREDICTED, 3, "2", Lists.getList("2a", "2b", "2c"));
        FactorGraph fg = new FactorGraph();
        fg.addFactor(new ExpFamFactor(new VarSet(v1, v2), "key2"));
        
        VarConfig vc = new VarConfig();
        vc.put(v1, state1);
        vc.put(v2, state2);
        
        return new FgExample(fg, vc, new MockFeatureExtractor(), fts);
    }

    public static FactorTemplateList getFtl() {
        return getFtl(false);
    }
    
    public static FactorTemplateList getFtl(boolean useLat) {
        FactorTemplateList fts = new FactorTemplateList();
        Var v1 = new Var(VarType.PREDICTED, 2, "1", Lists.getList("1a", "1b"));
        Var v2 = new Var(useLat ? VarType.LATENT : VarType.PREDICTED, 3, "2", Lists.getList("2a", "2b", "2c"));
        {
            Alphabet<Feature> alphabet = new Alphabet<Feature>();
            alphabet.lookupIndex(new Feature("feat1"));
            fts.add(new FactorTemplate(new VarSet(v1), alphabet, "key1"));
        }
        {
            Alphabet<Feature> alphabet = new Alphabet<Feature>();
            alphabet.lookupIndex(new Feature("feat2a"));
            alphabet.lookupIndex(new Feature("feat2b"));
            fts.add(new FactorTemplate(new VarSet(v1, v2), alphabet, "key2"));
        }
        return fts;
    }
        
    public static double[] getParams(FgModel model) {
        double[] params = new double[model.getNumParams()];
        model.updateDoublesFromModel(params);
        return params;
    }
    
}
