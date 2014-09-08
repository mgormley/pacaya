package edu.jhu.nlp.joint;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage;
import edu.jhu.gm.app.Encoder;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.depparse.DepParseEncoder;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.nlp.srl.SrlEncoder;
import edu.jhu.nlp.srl.SrlFeatureExtractor;
import edu.jhu.nlp.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Prm;

/**
 * Encodes a joint NLP factor graph and its variable assignment.
 * @author mgormley
 */
public class JointNlpEncoder implements Encoder<AnnoSentence, AnnoSentence> {

    private static final Logger log = Logger.getLogger(JointNlpEncoder.class);

    public static class JointNlpEncoderPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public JointFactorGraphPrm fgPrm = new JointFactorGraphPrm();
        public JointNlpFeatureExtractorPrm fePrm = new JointNlpFeatureExtractorPrm();
    }
    
    public static class JointNlpFeatureExtractorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public DepParseFeatureExtractorPrm dpFePrm = new DepParseFeatureExtractorPrm();
        public SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();        
    }

    private JointNlpEncoderPrm prm;
    private CorpusStatistics cs;
    private ObsFeatureConjoiner ofc;
    
    public JointNlpEncoder(JointNlpEncoderPrm prm, CorpusStatistics cs, ObsFeatureConjoiner ofc) {
        this.prm = prm;
        this.cs = cs;
        this.ofc = ofc;
    }

    @Override
    public LFgExample encode(AnnoSentence input, AnnoSentence gold) {
        return getExample(input, gold, true);
    }

    @Override
    public UFgExample encode(AnnoSentence input) {
        return getExample(input, null, false);
    }

    private LFgExample getExample(AnnoSentence sent, AnnoSentence gold, boolean labeledExample) {
        // Create a feature extractor for this example.
        ObsFeatureExtractor obsFe = new SrlFeatureExtractor(prm.fePrm.srlFePrm, sent, cs);
        obsFe = new ObsFeatureCache(obsFe);
        
        FeatureExtractor fe = new DepParseFeatureExtractor(prm.fePrm.dpFePrm, sent, cs, ofc.getFeAlphabet());
        fe = new FeatureCache(fe);
        
        // Construct the factor graph.
        JointNlpFactorGraph fg = new JointNlpFactorGraph(prm.fgPrm, sent, cs, obsFe, ofc, fe);
        log.trace("Number of variables: " + fg.getNumVars() + " Number of factors: " + fg.getNumFactors() + " Number of edges: " + fg.getNumEdges());

        // Get the variable assignments given in the training data.
        VarConfig vc = new VarConfig();
        if (gold != null && gold.getParents() != null) {
            DepParseEncoder.addDepParseTrainAssignment(gold.getParents(), fg.getDpBuilder(), vc);
        } else if (sent.getParents() != null && prm.fgPrm.includeDp && prm.fgPrm.dpPrm.linkVarType == VarType.OBSERVED) {
            // If the dependency tree is given in the input sentence, we might have added OBSERVED variables for it.
            DepParseEncoder.addDepParseTrainAssignment(sent.getParents(), fg.getDpBuilder(), vc);
        }
        if (gold != null && gold.getSrlGraph() != null) {
            SrlEncoder.addSrlTrainAssignment(sent, gold.getSrlGraph(), fg.getSrlBuilder(), vc, prm.fgPrm.srlPrm.predictSense, prm.fgPrm.srlPrm.predictPredPos);
        }
        if (gold != null && gold.getRelations() != null) {
            RelationsEncoder.addRelVarAssignments(sent, gold.getRelations(), fg.getRelBuilder(), vc);
        }
        
        // Create the example.
        LFgExample ex;
        FactorTemplateList fts = ofc.getTemplates();
        if (labeledExample) {
            ex = new LabeledFgExample(fg, vc, obsFe, fts);
        } else {
            ex = new UnlabeledFgExample(fg, vc, obsFe, fts);
        }
        fe.init(ex);
        return ex;
    }

    public static void checkForRequiredAnnotations(JointNlpEncoderPrm prm, AnnoSentenceCollection sents) {
        // Check that the first sentence has all the required annotation
        // types for the specified feature templates.
        AnnoSentence sent = sents.get(0);
        if (prm.fePrm.srlFePrm.fePrm.useTemplates) {
            if (prm.fgPrm.includeSrl) {
                TemplateLanguage.assertRequiredAnnotationTypes(sent, prm.fePrm.srlFePrm.fePrm.soloTemplates);
                TemplateLanguage.assertRequiredAnnotationTypes(sent, prm.fePrm.srlFePrm.fePrm.pairTemplates);
            }
        }
        if (prm.fgPrm.includeDp) {
            TemplateLanguage.assertRequiredAnnotationTypes(sent, prm.fePrm.dpFePrm.firstOrderTpls);
            if (prm.fgPrm.dpPrm.grandparentFactors || prm.fgPrm.dpPrm.siblingFactors) {
                TemplateLanguage.assertRequiredAnnotationTypes(sent, prm.fePrm.dpFePrm.secondOrderTpls);
            }
        }
        if (prm.fgPrm.includeRel) {
            TemplateLanguage.assertRequiredAnnotationTypes(sent, prm.fgPrm.relPrm.templates);
        }
    }
    
}
