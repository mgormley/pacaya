package edu.jhu.srl;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.gm.data.AbstractFgExampleList;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder;
import edu.jhu.gm.data.FgExampleListBuilder.FgExamplesBuilderPrm;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.srl.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.srl.JointNlpFeatureExtractor.JointNlpFeatureExtractorPrm;

/**
 * Factory for NLP FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class JointNlpFgExamplesBuilder {

    public static class JointNlpFgExampleBuilderPrm {
        public JointFactorGraphPrm fgPrm = new JointFactorGraphPrm();
        public JointNlpFeatureExtractorPrm srlFePrm = new JointNlpFeatureExtractorPrm();
        public FgExamplesBuilderPrm exPrm = new FgExamplesBuilderPrm();
    }
    
    private static final Logger log = Logger.getLogger(JointNlpFgExamplesBuilder.class);

    private ObsFeatureConjoiner ofc;
    private JointNlpFgExampleBuilderPrm prm;
    private CorpusStatistics cs;
    private FactorTemplateList fts;

    public JointNlpFgExamplesBuilder(JointNlpFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs) {
        this.prm = prm;
        this.ofc = ofc;
        this.cs = cs;
        this.fts = ofc.getTemplates();
        //this.sents = sents;
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
            ObsFeatureExtractor obsFe = new JointNlpFeatureExtractor(prm.srlFePrm, sent, cs);
            obsFe = new ObsFeatureCache(obsFe);
            
            // Construct the factor graph.
            JointNlpFactorGraph sfg = new JointNlpFactorGraph(prm.fgPrm, sent, cs, obsFe, ofc);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, sfg);
            
            // Create the example.
            FgExample ex = new FgExample(sfg, trainConfig, obsFe, fts);
            
            return ex;
        }
        
        public int size() {
            return sents.size();
        }

    }

    private static VarConfig getTrainAssignment(SimpleAnnoSentence sent, JointNlpFactorGraph sfg) {
        VarConfig vc = new VarConfig();
        DepParseEncoder.getDepParseTrainAssignment(sent, sfg, vc);       
        SrlEncoder.getSrlTrainAssignment(sent, sfg, vc);
        return vc;
    }
    
}
