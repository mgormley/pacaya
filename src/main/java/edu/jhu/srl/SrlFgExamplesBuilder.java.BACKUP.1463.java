package edu.jhu.srl;

import java.util.List;

<<<<<<< Updated upstream
import org.apache.log4j.Logger;

=======
import edu.berkeley.nlp.PCFGLA.smoothing.BerkeleySignatureBuilder;
>>>>>>> Stashed changes
import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

/**
 * Factory for FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SrlFgExamplesBuilder {
    
    private static final Logger log = Logger.getLogger(SrlFgExamplesBuilder.class); 

    private Alphabet<Feature> alphabet;
    private SrlFgExampleBuilderPrm prm;
    private BerkeleySignatureBuilder sig;
    
    public SrlFgExamplesBuilder(SrlFgExampleBuilderPrm prm, Alphabet<Feature> alphabet) {
        this.prm = prm;
        this.alphabet = alphabet;
        this.sig = new BerkeleySignatureBuilder(new Alphabet());
    }
        
    public FgExamples getData(SimpleAnnoSentenceCollection sents) {
        throw new RuntimeException("Not implemented");
    }
    
    public FgExamples getData(CoNLL09FileReader reader) {
        List<CoNLL09Sentence> sents = reader.readAll();
<<<<<<< Updated upstream
        return getData(sents);
    }

    public FgExamples getData(List<CoNLL09Sentence> sents) {
        CorpusStatistics cs = new CorpusStatistics(prm.fePrm);
        cs.init(sents);

        Alphabet<String> obsAlphabet = new Alphabet<String>();
        SrlFgExampleBuilder ps = new SrlFgExampleBuilder(prm, alphabet, cs, obsAlphabet);
=======
        CorpusStatistics cs = new CorpusStatistics(prm);
        cs.init(sents);

        // TODO: set these params.
        SrlFgExampleBuilder ps = new SrlFgExampleBuilder(prm, alphabet, cs, sig);
>>>>>>> Stashed changes

        FgExamples data = new FgExamples(alphabet);
        for (CoNLL09Sentence sent : sents) {
            data.add(ps.getFGExample(sent));
        }
        
        log.info("Num observation features: " + obsAlphabet.size());
        
        return data;
    }

    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
    
}
