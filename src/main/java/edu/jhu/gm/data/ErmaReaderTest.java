package edu.jhu.gm.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import data.DataSample;
import data.FeatureFile;
import data.RV;
import data.VariableSet;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureCache;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarSet;
import edu.jhu.util.Alphabet;

public class ErmaReaderTest {

    // TODO: remove dependence on hard-coded paths.
    public static final String ERMA_TOY_TRAIN_DATA_FILE = "/Users/mgormley/research/erma/src/main/resources/tutorial/toy.train.data.ff";
    public static final String ERMA_TOY_TEST_DATA_FILE = "/Users/mgormley/research/erma/src/main/resources/tutorial/toy.test.data.ff";
    public static final String ERMA_TOY_FEATURE_FILE = "/Users/mgormley/research/erma/src/main/resources/tutorial/toy.template.ff";
    
    @Test
    public void testErmaReader() {
        boolean includeUnsupportedFeatures = true;
        ErmaReader er = new ErmaReader(includeUnsupportedFeatures);
        er.read(ERMA_TOY_FEATURE_FILE, ERMA_TOY_TRAIN_DATA_FILE);
        List<DataSample> samples = er.getDataSamples();
        FeatureFile ff = er.getFeatureFile();
        
        System.out.println(ff);
        // Just test that we can construct these without error.
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FgExamples data = er.getDataExs(alphabet);
        assertEquals(samples.size(), data.size());
        for (int i=0; i<samples.size(); i++) {
            DataSample samp = samples.get(i);
            data.FactorGraph ermaFg = samp.toFactorGraph();
            //System.out.println(s);
            //System.out.println(fg);

            FgExample ex = data.get(i);            
            FactorGraph ourFg = ex.getOriginalFactorGraph();
            
            assertEquals(ermaFg.getFactors().size(), ourFg.getNumFactors());
            assertEquals(ermaFg.getVariables().size(), ourFg.getNumVars());
            
            for (int a=0; a<ourFg.getNumFactors(); a++) {
                data.Factor ermaFac = ermaFg.getFactor(a);
                Factor ourFac = ourFg.getFactor(a);
                VariableSet ermaVars = ermaFac.getVars();
                VarSet ourVars = ourFac.getVars();
                assertEquals(ermaVars.size(), ourVars.size());
                Iterator<RV> evIter = ermaVars.iterator();
                Iterator<Var> ovIter = ourVars.iterator();
                while (evIter.hasNext() && ovIter.hasNext()) {
                    RV ermaVar = evIter.next();
                    Var ourVar = ovIter.next();
                    assertEquals(ermaVar.getName(), ourVar.getName());
                }                                
            }

            {
                System.out.println("\n\nFeatures for fgLat: ");
                FeatureCache cacheLat = ex.getFeatCacheLat();
                FactorGraph fgLat = ex.getFgLat(new double[] { data
                        .getAlphabet().size() });
                System.out.println(cacheLat.toString(data.getAlphabet()));
            }
            {
                System.out.println("\n\nFeatures for fgLatPred: ");
                FeatureCache cacheLatPred = ex.getFeatCacheLatPred();
                FactorGraph fgLatPred = ex.getFgLatPred(new double[] { data
                        .getAlphabet().size() });
                System.out.println(cacheLatPred.toString(data.getAlphabet()));
            }
            
            // TODO: test that the features are what we'd expect them to be.
        }
        

        // Same number of features.
        HashSet<String> ermaAllFeatNames = new HashSet<String>();
        for (data.Feature f : ff.getFeatures()) {
            ermaAllFeatNames.add(f.getName());
            //System.out.println(f.getName());
        }
        HashSet<String> ermaObsFeatNames = new HashSet<String>();
        for (DataSample samp : samples) {
            data.FeatureFactorGraph ermaFg = (data.FeatureFactorGraph)samp.toFactorGraph();
            ArrayList<ArrayList<HashMap<data.Feature, Double>>> featureRefs = ermaFg.getFeatureRefs();
            for (ArrayList<HashMap<data.Feature, Double>> featVecList : featureRefs) {
                for (HashMap<data.Feature, Double> featVec : featVecList) {
                    for (data.Feature f : featVec.keySet()) {
                        ermaObsFeatNames.add(f.getName());
                        //System.out.println(f.getName());
                    }
                }
            }
        }
        
        if (includeUnsupportedFeatures) {
            assertEquals(ermaAllFeatNames.size(), data.getAlphabet().size());
        } else {
            assertEquals(ermaObsFeatNames.size(), data.getAlphabet().size());
        }
    }
    
}
