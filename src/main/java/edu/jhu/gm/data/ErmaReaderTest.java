package edu.jhu.gm.data;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import data.DataSample;
import data.FeatureFile;

public class ErmaReaderTest {

    // TODO: remove dependence on hard-coded paths.
    private static final String ERMA_TOY_TRAIN_DATA_FILE = "/Users/mgormley/research/ERMA/tutorial/toy.train.data.ff";
    private static final String ERMA_TOY_TEST_DATA_FILE = "/Users/mgormley/research/ERMA/tutorial/toy.test.data.ff";
    private static final String ERMA_TOY_FEATURE_FILE = "/Users/mgormley/research/ERMA/tutorial/toy.template.ff";
    
    @Test
    public void testErmaReader() {
        ErmaReader er = new ErmaReader();
        er.read(ERMA_TOY_FEATURE_FILE, ERMA_TOY_TRAIN_DATA_FILE);
        List<DataSample> data = er.getData();
        FeatureFile ff = er.getFeatureFile();
        
        /*
         * FeatureFile.types contains a map from variable type names (e.g.
         * CHUNK) to a Type object, which contains a list of the states it can
         * take (e.g. CHUNK:= [O,B_PP,B_VP,B_NP,I_NP]).
         * 
         * FeatureFile.features is a map from an expanded name of a variable
         * (e.g. w2_oov_chunk_link*_B_NP_I_NP) to a single Feature object (e.g.
         * w2_oov_chunk_link(CHUNK,CHUNK):= [B_NP,I_NP]).
         * 
         * FeatureFile.featureGroups is a map from a feature name
         * (e.g.wm1_john_chunk_link) to a list of Feature objects (e.g.
         * [wm1_john_chunk_link(CHUNK,CHUNK):= [O,O],
         * wm1_john_chunk_link(CHUNK,CHUNK):= [B_PP,O],
         * wm1_john_chunk_link(CHUNK,CHUNK):= [B_VP,O],...]).
         */

        /*
         * DataSample.variables is a map from variable names (e.g. C4) to RV
         * objects (e.g. C4o).
         * 
         * The RV object describes the type (RV.type) as a Type object, the
         * value (RV.value) as an int--which can be mapped to a string using
         * Type.getTypeString(), the name (RV.name), and the visibility type
         * (RV.visibilityType) which describes whether it is INPUT,OUTPUT, or
         * HIDDEN.
         * 
         * DataSample.featureInstances is a list of FeatureInstance objects.
         * 
         * Each FeatureInstance object contains a feature group (as in the
         * values of FeatureFile.featureGroups), and a list of RVs. 
         * 
         * TODO: It's still not clear exactly how FeatureInstances are used.
         */
        
        /*
         * Factor graphs contain a list of factors. 
         * 
         * Each factor contains a list of its variables.
         */
        
        System.out.println(ff);
        for (DataSample s : data) {
            System.out.println(s);
            data.FactorGraph fg = s.toFactorGraph();
            System.out.println(fg);
        }
                       
        fail("Not yet implemented");
    }

}
