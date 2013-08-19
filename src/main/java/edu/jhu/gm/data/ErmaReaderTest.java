package edu.jhu.gm.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import data.DataSample;
import data.FeatureFile;
import data.RV;
import data.VariableSet;
import dataParser.DataParser;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureCache;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarSet;
import edu.jhu.util.Alphabet;
import featParser.FeatureFileParser;

public class ErmaReaderTest {

    // TODO: remove dependence on hard-coded paths.
    private static final String ERMA_TUTORIAL_DIR = "/Users/mgormley/research/erma/tmp/tutorial";
    public static final String ERMA_TOY_TRAIN_DATA_FILE = ERMA_TUTORIAL_DIR + "/toy.train.data.ff";
    public static final String ERMA_TOY_TEST_DATA_FILE = ERMA_TUTORIAL_DIR + "/toy.test.data.ff";
    public static final String ERMA_TOY_FEATURE_FILE = ERMA_TUTORIAL_DIR + "/toy.template.ff";
    
    @Test
    public void testErmaReader() {
        // Read the ERMA files to get ERMA objects.
        SimpleErmaReader ser = new SimpleErmaReader();
        ser.read(ERMA_TOY_FEATURE_FILE, ERMA_TOY_TRAIN_DATA_FILE);
        List<DataSample> samples = ser.getDataSamples();
        FeatureFile ff = ser.getFeatureFile();
        System.out.println(ff);
        
        // Read the ERMA files to get our objects.
        boolean includeUnsupportedFeatures = true;
        ErmaReader er = new ErmaReader(includeUnsupportedFeatures);
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FgExamples data = er.read(ERMA_TOY_FEATURE_FILE, ERMA_TOY_TRAIN_DATA_FILE, alphabet);

        // Just test that we can construct these without error.
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
        
        FgModel model = new FgModel(data, includeUnsupportedFeatures);
        if (includeUnsupportedFeatures) {
            assertEquals(ermaAllFeatNames.size(), model.getNumParams());
        } else {
            assertEquals(ermaObsFeatNames.size(), model.getNumParams());
        }
    }
    
    
    /**
     * Reads input files in ERMA format.
     * 
     * @author mgormley
     * 
     */
    public static class SimpleErmaReader {

        private static final Logger log = Logger.getLogger(SimpleErmaReader.class);
        private ArrayList<DataSample> samples;
        private FeatureFile ff;

        /**
         * Constructs an ERMA reader, including all the unsupported features (ERMA's default).
         */
        public SimpleErmaReader() {
        }
        
        public void read(File featureTemplate, File dataFile) {
            read(featureTemplate.getAbsolutePath(), dataFile.getAbsolutePath());
        }
        
        /**
         * Reads a feature file containing templates of features and a data file
         * containing a list of examples.
         * 
         * @param featureTemplate
         *            The path to the feature file.
         * @param dataFile
         *            The path to the data file.
         */
        public void read(String featureTemplate, String dataFile) {
            FeatureFileParser fp;
            log.info("Reading features from " + featureTemplate);
            try {
                fp = FeatureFileParser.createParser(featureTemplate);
                ff = fp.parseFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            log.info("Reading data from " + dataFile);
            DataParser dp;
            try {
                dp = DataParser.createParser(dataFile, ff);
                samples = dp.parseFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Gets the feature templates read from the feature file.
         * 
         * @return An object representing the feature file.
         */
        public FeatureFile getFeatureFile() {
            return ff;
        }

        /**
         * Gets the data read from the data file.
         * 
         * @return A list of data samples.
         */
        public ArrayList<DataSample> getDataSamples() {
            return samples;
        }
    }
}
