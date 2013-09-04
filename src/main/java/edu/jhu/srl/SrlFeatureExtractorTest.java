package edu.jhu.srl;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
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
        SrlFactorGraph sfg = getSrlFg(fgPrm);

        FeatureTemplateList fts = new FeatureTemplateList();
        
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        List<SimpleAnnoSentence> sents = cr.readSentsToSimple(1, csPrm);
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);
        
        fts.update(sfg);
        
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;        
        SentFeatureExtractor sentFeatExt= new SentFeatureExtractor(fePrm, sents.get(0), cs);
        SrlFeatureExtractorPrm prm = new SrlFeatureExtractorPrm();
        prm.featureHashMod = -1; // Disable feature hashing.
        SrlFeatureExtractor featExt = new SrlFeatureExtractor(prm, sentFeatExt);
        featExt.init(sfg, null, null, new VarConfig(), fts);
        for (int a=0; a<sfg.getNumFactors(); a++) {
            featExt.calcObsFeatureVector(a);
        }
        
        System.out.println(fts);
        
        //assertEquals(3*2 + 2 + 3, fts.getNumObsFeats());
        assertEquals(3, fts.getNumObsFeats());
    }
    
    @Test
    public void testCorrectNumExpandedFeatures() throws Exception {
        // What's up with this one?
        FeatureTemplateList fts = new FeatureTemplateList();

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        List<CoNLL09Sentence> conllSents = cr.readSents(1, 20);
        List<SimpleAnnoSentence> sents = new ArrayList<SimpleAnnoSentence>();
        for (CoNLL09Sentence sent : conllSents) {
            sent.normalizeRoleNames();
            SimpleAnnoSentence simpleSent = sent.toSimpleAnnoSentence(csPrm);
            sents.add(simpleSent);
        }
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
                
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExamples data = builder.getData(sents);
        
        FgModel model = new FgModel(data, false);
        System.out.println("Num tokens: " + sents.get(0).size());
        System.out.println(model);
        
        // If we included all features we would get: 6*2 + 2 + 6
        // For biasOnly=true: 
        //assertEquals(17, model.getAlphabet().size());
        
        // For useNaradFeats=true: 
        // Correct number of obs feats 358, and seeing 358 after bad commit.
        // Correct number is 972, but seeing 932 after bad commit.
        assertEquals(983, model.getNumParams());
    }
    
    @Test
    public void testCorrectNumExpandedFeaturesForSmallSent() throws Exception {
        FeatureTemplateList fts = new FeatureTemplateList();
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
        //tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "_", "N", "_", getList("feat"), getList("feat") , 3, 3, "subj", "_", false, "_", getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "_", "V", "_", getList("feat"), getList("feat") , 0, 0, "v", "_", true, "ate.1", getList("_")));
        tokens.add(new CoNLL09Token(4, "food", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        List<CoNLL09Sentence> sents = getList(sent);
        List<SimpleAnnoSentence> simpleSents = new ArrayList<SimpleAnnoSentence>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm));
        }
        cs.init(simpleSents);

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
        
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExamples data = builder.getData(simpleSents);
        
        FgModel model = new FgModel(data, false);
        System.out.println("Num tokens: " + sents.get(0).size());
        System.out.println(model);
        // If we included all features we would get: 6*2 + 2 + 6
        // For biasOnly=true: 
        // assertEquals(17, model.getAlphabet().size());
        
        // For useNaradFeats=true: 
        // Correct number is 72, and seeing 72 after bad commit.
        assertEquals(84, model.getNumParams());
    }
    
    @Test
    public void testCorrectNumFeaturesWithFeatureHashing() throws Exception {
        SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        SrlFactorGraph sfg = getSrlFg(fgPrm);

        FeatureTemplateList fts = new FeatureTemplateList();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);

        List<SimpleAnnoSentence> simpleSents = new ArrayList<SimpleAnnoSentence>();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        for (CoNLL09Sentence s : sents) {
            s.normalizeRoleNames();
            simpleSents.add(s.toSimpleAnnoSentence(csPrm));
        }
        cs.init(simpleSents);
        
        
        fts.update(sfg);
        
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.useNaradFeats = true;
        fePrm.useSimpleFeats = false;
        fePrm.useZhaoFeats = false;
        fePrm.useDepPathFeats = false;
        SentFeatureExtractor sentFeatExt= new SentFeatureExtractor(fePrm, simpleSents.get(0), cs);
        SrlFeatureExtractorPrm prm = new SrlFeatureExtractorPrm();
        prm.featureHashMod = 10; // Enable feature hashing
        SrlFeatureExtractor featExt = new SrlFeatureExtractor(prm, sentFeatExt);
        featExt.init(sfg, null, null, new VarConfig(), fts);
        for (int a=0; a<sfg.getNumFactors(); a++) {
            featExt.calcObsFeatureVector(a);    
        }
        
        System.out.println(fts);
        assertEquals(10, fts.getNumObsFeats());
    }

    private static SrlFactorGraph getSrlFg(SrlFactorGraphPrm prm) {
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        List<String> words = Utilities.getList("w1", "w2", "w3");
        return new SrlFactorGraph(prm, words, words, knownPreds, Utilities.getList("A1", "A2", "A3"), null);
    }

}
