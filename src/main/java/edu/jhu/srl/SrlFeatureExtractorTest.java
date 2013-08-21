package edu.jhu.srl;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.Var.VarType;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
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
                VarConfig vc =vars.getVarConfig(c);
                featExt.calcFeatureVector(a, vc);    
            }            
        }
        
        assertEquals(1, obsAlphabet.size());
        assertEquals(3*2 + 2 + 3, alphabet.size());
    }
    
    @Test
    public void testCorrectNumExpandedFeatures() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1, 20);
        CorpusStatistics.normalizeRoleNames(sents);

        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);

        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm.biasOnly = false;
        prm.fePrm.useDepPathFeats = false;
        prm.fePrm.useNaradFeats = true;
        prm.fePrm.useSimpleFeats = false;
        prm.fePrm.useZhaoFeats = false;   
        
        prm.srlFePrm.featureHashMod = -1;
        
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.linkVarType = VarType.OBSERVED;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        
        prm.includeUnsupportedFeatures = false;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet, cs);
        FgExamples data = builder.getData(sents);
        
        FgModel model = new FgModel(data.getAlphabet());
        System.out.println("Num tokens: " + sents.get(0).size());
        //System.out.println(model);
        // If we included all features we would get: 6*2 + 2 + 6
        //For biasOnly=true: 
        //assertEquals(17, model.getAlphabet().size());
        //For useNaradFeats=true: 
        assertEquals(972, model.getAlphabet().size());
    }
    

    @Test
    public void testCorrectNumExpandedFeaturesForSmallSent() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
        // tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "subj", "_", false, "_", getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "_", "V", "_", getList("feat"), getList("feat") , 0, 0, "v", "_", true, "ate.1", getList("_")));
        //tokens.add(new CoNLL09Token(4, "food", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        List<CoNLL09Sentence> sents = getList(sent);
        
        CorpusStatistics.normalizeRoleNames(sents);

        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);

        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm.biasOnly = false;
        prm.fePrm.useDepPathFeats = false;
        prm.fePrm.useNaradFeats = true;
        prm.fePrm.useSimpleFeats = false;
        prm.fePrm.useZhaoFeats = false;   
        
        prm.srlFePrm.featureHashMod = -1;
        
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.linkVarType = VarType.OBSERVED;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        
        prm.includeUnsupportedFeatures = false;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet, cs);
        FgExamples data = builder.getData(sents);
        
        FgModel model = new FgModel(data.getAlphabet());
        System.out.println("Num tokens: " + sents.get(0).size());
        System.out.println(model);
        // If we included all features we would get: 6*2 + 2 + 6
        //For biasOnly=true: 
        //assertEquals(17, model.getAlphabet().size());
        //For useNaradFeats=true: 
        assertEquals(36, model.getAlphabet().size());
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
                VarConfig vc =vars.getVarConfig(c);
                featExt.calcFeatureVector(a, vc);    
            }            
        }
        
        assertTrue(10 < obsAlphabet.size());
        System.out.println(alphabet);
        assertEquals(10, alphabet.size());
    }

}
