package edu.jhu.srl;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.gm.data.AbstractFgExampleList;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder;
import edu.jhu.gm.data.FgExampleListBuilder.FgExamplesBuilderPrm;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.srl.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.srl.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Prm;

/**
 * Factory for NLP FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class JointNlpFgExamplesBuilder {

    public static class JointNlpFgExampleBuilderPrm {
        public JointFactorGraphPrm fgPrm = new JointFactorGraphPrm();
        public JointNlpFeatureExtractorPrm fePrm = new JointNlpFeatureExtractorPrm();
        public FgExamplesBuilderPrm exPrm = new FgExamplesBuilderPrm();
    }
    
    public static class JointNlpFeatureExtractorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public DepParseFeatureExtractorPrm dpFePrm = new DepParseFeatureExtractorPrm();
        public SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();        
    }
    
    private static final Logger log = Logger.getLogger(JointNlpFgExamplesBuilder.class);

    private ObsFeatureConjoiner ofc;
    private JointNlpFgExampleBuilderPrm prm;
    private CorpusStatistics cs;
    private FactorTemplateList fts;
    private boolean labeledExamples;

    public JointNlpFgExamplesBuilder(JointNlpFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs) {
        this(prm, ofc, cs, true);
    }
    
    public JointNlpFgExamplesBuilder(JointNlpFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs, boolean labeledExamples) {
        this.prm = prm;
        this.ofc = ofc;
        this.cs = cs;
        this.fts = ofc.getTemplates();
        this.labeledExamples = labeledExamples;
    }

    public FgExampleList getData(SimpleAnnoSentenceCollection sents) {
        FgExampleListBuilder builder = new FgExampleListBuilder(prm.exPrm);
        FgExampleList data = builder.getInstance(new SrlFgExampleFactory(sents, ofc));
        return data;
    }
    
    /** 
     * This class is read-only and thread-safe.
     */
    private class SrlFgExampleFactory extends AbstractFgExampleList implements FgExampleList {

        private SimpleAnnoSentenceCollection sents;
        private ObsFeatureConjoiner ofc;
        
        public SrlFgExampleFactory(SimpleAnnoSentenceCollection sents, ObsFeatureConjoiner ofc) {
            this.sents = sents;
            this.ofc = ofc;
        }
        
        public FgExample get(int i) {
            log.trace("Getting example: " + i);
            SimpleAnnoSentence sent = sents.get(i);
            
            // Create a feature extractor for this example.
            ObsFeatureExtractor obsFe = new SrlFeatureExtractor(prm.fePrm.srlFePrm, sent, cs);
            obsFe = new ObsFeatureCache(obsFe);
            
            FeatureExtractor fe = new DepParseFeatureExtractor(prm.fePrm.dpFePrm, sent, cs, ofc.getFeAlphabet());
            fe = new FeatureCache(fe);
            
            // Construct the factor graph.
            JointNlpFactorGraph sfg = new JointNlpFactorGraph(prm.fgPrm, sent, cs, obsFe, ofc, fe);
            log.trace("Number of variables: " + sfg.getNumVars() + " Number of factors: " + sfg.getNumFactors());
            // Get the variable assignments given in the training data.
            VarConfig vc = getVarAssignment(sent, sfg, prm.fgPrm);
            
            // Create the example.
            FgExample ex;
            if (labeledExamples) {
                ex = new LabeledFgExample(sfg, vc, obsFe, fts);
            } else {
                ex = new UnlabeledFgExample(sfg, vc, obsFe, fts);
            }
            fe.init(ex);
            
            return ex;
        }
        
        public int size() {
            return sents.size();
        }

    }

    /** Gets the variable assignment for either all variables or only the observed variables. 
     * @param fgPrm */
    private static VarConfig getVarAssignment(SimpleAnnoSentence sent, JointNlpFactorGraph sfg, JointFactorGraphPrm fgPrm) {
        VarConfig vc = new VarConfig();
        DepParseEncoder.getDepParseTrainAssignment(sent, sfg, vc);       
        SrlEncoder.getSrlTrainAssignment(sent, sfg, vc, fgPrm.srlPrm.predictSense, fgPrm.srlPrm.predictPredPos);
        return vc;
    }
    
}
