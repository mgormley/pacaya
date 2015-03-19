package edu.jhu.nlp.srl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.features.TemplateLanguage;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.features.TemplateWriter;
import edu.jhu.nlp.joint.IGFeatureTemplateSelector;
import edu.jhu.nlp.joint.IGFeatureTemplateSelector.IGFeatureTemplateSelectorPrm;
import edu.jhu.nlp.joint.IGFeatureTemplateSelector.SrlFeatTemplates;
import edu.jhu.nlp.joint.JointNlpEncoder;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpRunner;
import edu.jhu.nlp.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.collections.Lists;

/**
 * Train-time only "annotator" for feature selection. This modifies the feature templates on the
 * given {@link JointNlpFeatureExtractorPrm} in place.
 * 
 * TODO: Deprecate this class as it has static dependencies on JointNlpRunner.
 * 
 * @author mgormley
 */
public class SrlFeatureSelection implements Annotator, Trainable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SrlFeatureSelection.class);
    private transient JointNlpFeatureExtractorPrm fePrm;

    public SrlFeatureSelection(JointNlpFeatureExtractorPrm fePrm) {
        this.fePrm = fePrm;
    }

    @Override
    public void annotate(AnnoSentenceCollection sents) {
        // Do nothing. This only runs at training time.
    }

    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold,
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        featureSelection(trainInput, trainGold, fePrm);
    }

    /**
     * Do feature selection and update fePrm with the chosen feature templates.
     */
    private static void featureSelection(AnnoSentenceCollection inputSents, AnnoSentenceCollection goldSents, JointNlpFeatureExtractorPrm fePrm)  {
        SrlFeatureExtractorPrm srlFePrm = fePrm.srlFePrm;
        // Remove annotation types from the features which are explicitly excluded.
        removeAts(fePrm);
        if (JointNlpRunner.useTemplates && JointNlpRunner.featureSelection) {
            CorpusStatisticsPrm csPrm = JointNlpRunner.getCorpusStatisticsPrm();
            
            IGFeatureTemplateSelectorPrm prm = JointNlpRunner.getInformationGainFeatureSelectorPrm();
            SrlFeatTemplates sft = new SrlFeatTemplates(srlFePrm.fePrm.soloTemplates, srlFePrm.fePrm.pairTemplates, null);
            IGFeatureTemplateSelector ig = new IGFeatureTemplateSelector(prm);
            sft = ig.getFeatTemplatesForSrl(inputSents, goldSents, csPrm, sft);
            fePrm.srlFePrm.fePrm.soloTemplates = sft.srlSense;
            fePrm.srlFePrm.fePrm.pairTemplates = sft.srlArg;
        }
        if (CorpusHandler.getGoldOnlyAts().contains(AT.SRL) && JointNlpRunner.acl14DepFeats) {
            fePrm.dpFePrm.firstOrderTpls = srlFePrm.fePrm.pairTemplates;
        }
        if (JointNlpRunner.useTemplates) {
            log.info("Num sense feature templates: " + srlFePrm.fePrm.soloTemplates.size());
            log.info("Num arg feature templates: " + srlFePrm.fePrm.pairTemplates.size());
            if (JointNlpRunner.senseFeatTplsOut != null) {
                TemplateWriter.write(JointNlpRunner.senseFeatTplsOut, srlFePrm.fePrm.soloTemplates);
            }
            if (JointNlpRunner.argFeatTplsOut != null) {
                TemplateWriter.write(JointNlpRunner.argFeatTplsOut, srlFePrm.fePrm.pairTemplates);
            }
        }
    }

    private static void removeAts(JointNlpEncoder.JointNlpFeatureExtractorPrm fePrm) {
        Set<AT> ats = Sets.union(CorpusHandler.getRemoveAts(), CorpusHandler.getPredAts());
        if (JointNlpRunner.brownClusters == null) {
            // Filter out the Brown cluster features.
            log.warn("Filtering out Brown cluster features.");
            ats.add(AT.BROWN);
        }
        for (AT at : ats) {
            fePrm.srlFePrm.fePrm.soloTemplates = TemplateLanguage.filterOutRequiring(fePrm.srlFePrm.fePrm.soloTemplates, at);
            fePrm.srlFePrm.fePrm.pairTemplates   = TemplateLanguage.filterOutRequiring(fePrm.srlFePrm.fePrm.pairTemplates, at);
            fePrm.dpFePrm.firstOrderTpls = TemplateLanguage.filterOutRequiring(fePrm.dpFePrm.firstOrderTpls, at);
            fePrm.dpFePrm.secondOrderTpls   = TemplateLanguage.filterOutRequiring(fePrm.dpFePrm.secondOrderTpls, at);
        }
    }
    
    @Override
    public Set<AT> getAnnoTypes() {
        return Collections.emptySet();
    }
    
}