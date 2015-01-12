package edu.jhu.nlp.joint;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.data.AbstractFgExampleList;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder;
import edu.jhu.gm.data.FgExampleListBuilder.FgExamplesBuilderPrm;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.simple.AlphabetStore;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpEncoderPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Prm;

/**
 * Factory for NLP FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class JointNlpFgExamplesBuilder {

    public static class JointNlpFgExampleBuilderPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public FgExamplesBuilderPrm exPrm = new FgExamplesBuilderPrm();
        // TODO: Switch fgPrm and fePrm to: public JointNlpEncoderPrm jePrm = new JointNlpEncoderPrm();
        public JointFactorGraphPrm fgPrm = new JointFactorGraphPrm();
        public JointNlpFeatureExtractorPrm fePrm = new JointNlpFeatureExtractorPrm();
    }
    
    private static final Logger log = LoggerFactory.getLogger(JointNlpFgExamplesBuilder.class);

    private ObsFeatureConjoiner ofc;
    private JointNlpFgExampleBuilderPrm prm;
    private JointNlpEncoderPrm jePrm;
    private CorpusStatistics cs;
    private boolean labeledExamples;

    public JointNlpFgExamplesBuilder(JointNlpFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs) {
        this(prm, ofc, cs, true);
    }
    
    public JointNlpFgExamplesBuilder(JointNlpFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs, boolean labeledExamples) {
        this.prm = prm;
        this.ofc = ofc;
        this.cs = cs;
        this.labeledExamples = labeledExamples;
        this.jePrm = new JointNlpEncoderPrm();
        jePrm.fePrm = prm.fePrm;
        jePrm.fgPrm = prm.fgPrm;
    }
    
    @Deprecated
    public FgExampleList getData(AnnoSentenceCollection sents) {
        return getData(sents, sents);
    }
    
    public FgExampleList getData(AnnoSentenceCollection inputSents, AnnoSentenceCollection goldSents) {
        if (!cs.isInitialized()) {
            // TODO: This seems like an odd place to initialize CorpusStatistics. Should this live in JointNlpAnnotator?
            log.info("Initializing corpus statistics.");
            cs.init(goldSents, false);
            cs.store = new AlphabetStore(inputSents);
        }
        JointNlpEncoder.checkForRequiredAnnotations(jePrm, inputSents);
        
        log.info("Building factor graphs and extracting features.");
        FgExampleListBuilder builder = new FgExampleListBuilder(prm.exPrm);
        FgExampleList data = builder.getInstance(new JointNlpFgExampleFactory(inputSents, goldSents, ofc));
        
        // Special case: we somehow need to be able to create test examples
        // where we've never seen the predicate.
        FactorTemplateList fts = ofc.getTemplates();
        if ((jePrm.fgPrm.srlPrm.predictSense || jePrm.fgPrm.srlPrm.predictPredPos) && fts.isGrowing()) {
            // TODO: This should have a bias feature.
            List<String> senses = CorpusStatistics.PRED_POSITION_STATE_NAMES;
            Var v = new Var(VarType.PREDICTED, senses.size(), CorpusStatistics.UNKNOWN_SENSE, senses);
            fts.add(new FactorTemplate(new VarSet(v), new FeatureNames(), SrlFactorGraphBuilder.TEMPLATE_KEY_FOR_UNKNOWN_SENSE));
            fts.add(new FactorTemplate(new VarSet(v), new FeatureNames(), SrlFactorGraphBuilder.TEMPLATE_KEY_FOR_UNKNOWN_SENSE_ROLE));
        }
        
        if (!ofc.isInitialized()) {
            log.info("Initializing the observation function conjoiner.");
            ofc.init(data);
        }
                
        log.info(String.format("Num examples: %d", data.size()));
        // TODO: log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        // TODO: log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num factor/clique templates: %d", fts.size()));
        log.info(String.format("Num observation function features: %d", fts.getNumObsFeats()));
        return data;
    }
    
    /** 
     * This class is read-only and thread-safe.
     */
    private class JointNlpFgExampleFactory extends AbstractFgExampleList implements FgExampleList {

        private AnnoSentenceCollection inputSents;
        private AnnoSentenceCollection goldSents;
        private ObsFeatureConjoiner ofc;
        
        public JointNlpFgExampleFactory(
                AnnoSentenceCollection inputSents, 
                AnnoSentenceCollection goldSents, 
                ObsFeatureConjoiner ofc) {
            this.inputSents = inputSents;
            this.goldSents = goldSents;
            this.ofc = ofc;
        }
        
        public LFgExample get(int i) {
            log.trace("Getting example: " + i);
            JointNlpEncoder encoder = new JointNlpEncoder(jePrm, cs, ofc);
            if (labeledExamples) {
                return encoder.encode(inputSents.get(i), goldSents.get(i));
            } else {
                return (LFgExample) encoder.encode(inputSents.get(i));
            }
        }
        
        public int size() {
            return inputSents.size();
        }

    }
    
}
