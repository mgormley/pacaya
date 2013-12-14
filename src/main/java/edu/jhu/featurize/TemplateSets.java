package edu.jhu.featurize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.featurize.TemplateLanguage.BigramTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;

public class TemplateSets {

    private static final String bjorkelundArgFeatsResource = "/edu/jhu/featurize/bjorkelund-arg-feats.txt";
    private static final String bjorkelundSenseFeatsResource = "/edu/jhu/featurize/bjorkelund-sense-feats.txt";
    
    private TemplateSets() {
        // Private constructor.
    }
        
    public static List<FeatTemplate> getBjorkelundSenseUnigramFeatureTemplates() {
        // PredWord
        // PredPOS
        // PredDeprel
        // PredFeats
        // PredParentWord
        // PredParentPOS
        // PredParentFeats
        // DepSubCat
        // ChildDepSet
        // ChildWordSet
        // ChildPOSSet
        TemplateReader reader = new TemplateReader();
        try {
            reader.readFromResource(bjorkelundSenseFeatsResource);
            return reader.getTemplates();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    public static List<FeatTemplate> getBjorkelundArgUnigramFeatureTemplates() {
        // PredWord
        // PredPOS
        // PredLemma
        // PredDeprel
        // Sense
        // PredFeats
        // PredParentWord
        // PredParentPOS
        // PredParentFeats
        // DepSubCat
        // ChildDepSet
        // ChildWordSet
        // ChildPOSSet
        // ArgWord
        // ArgPOS
        // ArgFeats
        // ArgDeprel
        // DeprelPath
        // POSPath
        // Position
        // LeftWord
        // LeftPOS
        // LeftFeats
        // RightWord
        // RightPOS
        // RightFeats
        // LeftSiblingWord
        // LeftSiblingPOS
        // LeftSiblingFeats
        // RightSiblingWord
        // RightSiblingPOS
        // RightSiblingFeats
        TemplateReader reader = new TemplateReader();
        try {
            reader.readFromResource(bjorkelundArgFeatsResource);
            return reader.getTemplates();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    public static List<FeatTemplate> getAllUnigramFeatureTemplates() {
        ArrayList<FeatTemplate> tpls = new ArrayList<FeatTemplate>();
        for (Position pos : Position.values()) {
            for (PositionModifier mod : PositionModifier.values()) {
                for (TokProperty prop : TokProperty.values()) {
                    tpls.add(new FeatTemplate1(pos, mod, prop));
                }
                for (TokPropList prop : TokPropList.values()) {
                    tpls.add(new FeatTemplate2(pos, mod, prop));
                }
            }
        }
        for (PositionList pl : PositionList.values()) {
            for (ListModifier lmod : ListModifier.values()) {
                for (boolean includeDir : new boolean[]{true, false}) {
                    for (TokProperty prop : TokProperty.values()) {
                        tpls.add(new FeatTemplate3(pl, prop, includeDir, lmod));
                    }
                }
            }
        }     
        for (OtherFeat feat : OtherFeat.values()) {
            tpls.add(new FeatTemplate4(feat));
        }
        return tpls;
    }
    
    public static List<FeatTemplate> getAllBigramFeatureTemplates() {
        List<FeatTemplate> unigrams = getAllUnigramFeatureTemplates();
        return getBigramFeatureTemplates(unigrams);
    }

    public static List<FeatTemplate> getBigramFeatureTemplates(List<FeatTemplate> unigrams) {
        ArrayList<FeatTemplate> bs = new ArrayList<FeatTemplate>();
        for (int i=0; i<unigrams.size(); i++) {
            for (int j=i+1; j<unigrams.size(); j++) {
                bs.add(new BigramTemplate(unigrams.get(i), unigrams.get(j)));
            }
        }
        return bs;
    }
    
}
