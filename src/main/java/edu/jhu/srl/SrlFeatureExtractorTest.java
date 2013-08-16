package edu.jhu.srl;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Utilities;

/**
 * Unit tests for {@link SrlFeatureExtractorTest}.
 * @author mgormley
 */
public class SrlFeatureExtractorTest {

    @Test
    public void testCorrectNumFeatures() throws Exception {
        SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        fgPrm.alwaysIncludeLinkVars = true;
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(fgPrm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));

        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);
        
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;        
        SentFeatureExtractor sentFeatExt= new SentFeatureExtractor(fePrm, sents.get(0), cs, obsAlphabet);
        SrlFeatureExtractorPrm prm = new SrlFeatureExtractorPrm();
        prm.featureHashMod = -1; // Disable feature hashing.
        SrlFeatureExtractor featExt = new SrlFeatureExtractor(prm, sfg, alphabet, sentFeatExt);
        for (int a=0; a<sfg.getNumFactors(); a++) {
            VarSet vars = sfg.getFactor(a).getVars();
            int numConfigs = vars.calcNumConfigs();
            for (int c=0; c<numConfigs; c++) {                
                featExt.calcFeatureVector(a, c);    
            }            
        }
        
        assertEquals(0, obsAlphabet.size());
        assertEquals(3*2 + 2 + 3, alphabet.size());
    }
    
    @Test
    public void testCorrectNumFeaturesWithFeatureHashing() throws Exception {
        SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(fgPrm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));

        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);
        
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.useNaradFeats = true;
        fePrm.useSimpleFeats = false;
        fePrm.useZhaoFeats = false;
        SentFeatureExtractor sentFeatExt= new SentFeatureExtractor(fePrm, sents.get(0), cs, obsAlphabet);
        SrlFeatureExtractorPrm prm = new SrlFeatureExtractorPrm();
        prm.featureHashMod = 10; // Enable feature hashing
        SrlFeatureExtractor featExt = new SrlFeatureExtractor(prm, sfg, alphabet, sentFeatExt);
        for (int a=0; a<sfg.getNumFactors(); a++) {
            VarSet vars = sfg.getFactor(a).getVars();
            int numConfigs = vars.calcNumConfigs();
            for (int c=0; c<numConfigs; c++) {                
                featExt.calcFeatureVector(a, c);    
            }            
        }
        
        assertTrue(10 < obsAlphabet.size());
        System.out.println(alphabet);
        assertEquals(10, alphabet.size());
    }

}
