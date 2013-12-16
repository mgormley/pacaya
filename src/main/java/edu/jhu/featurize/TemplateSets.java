package edu.jhu.featurize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.featurize.TemplateLanguage.BigramTemplate;
import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
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
import edu.jhu.util.collections.Lists;

public class TemplateSets {

    private static final String bjorkelundArgFeatsResource = "/edu/jhu/featurize/bjorkelund-arg-feats.txt";
    private static final String bjorkelundSenseFeatsResource = "/edu/jhu/featurize/bjorkelund-sense-feats.txt";
    
    private TemplateSets() {
        // Private constructor.
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
                for (EdgeProperty eprop : Lists.cons(null, EdgeProperty.values())) {                    
                    for (TokProperty prop : Lists.cons(null, TokProperty.values())) {
                        // This check is rather messy. It'd be better if we just had separate
                        // structured templates for extracting TokProperties and EdgeProperties
                        // and they were always combined by conjunction (+). 
                        if (prop == null && eprop == null) {
                            continue;
                        } else if (!pl.isPath() && (prop == null || eprop != null)) {
                            continue;
                        }
                        tpls.add(new FeatTemplate3(pl, prop, eprop, lmod));
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
    
    public static List<FeatTemplate> getBjorkelundSenseUnigramFeatureTemplates() {
        TemplateReader reader = new TemplateReader();
        try {
            reader.readFromResource(bjorkelundSenseFeatsResource);
            return reader.getTemplates();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    public static List<FeatTemplate> getBjorkelundArgUnigramFeatureTemplates() {
        TemplateReader reader = new TemplateReader();
        try {
            reader.readFromResource(bjorkelundArgFeatsResource);
            return reader.getTemplates();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    public static List<FeatTemplate> getCoarseUnigramSet1() {

        //coarse_props = "pos bc0 bc1".split()
        //list_props = "deprel pos bc0".split() #TODO support dir        
        TokProperty[] coarseTokProps = new TokProperty[] { TokProperty.POS, TokProperty.DEPREL, TokProperty.BC0 };
        PositionList[] simplePosLists = new PositionList[] { PositionList.LINE_P_C, PositionList.CHILDREN_P, PositionList.PATH_P_C };

        ArrayList<FeatTemplate> tpls = new ArrayList<FeatTemplate>();
        for (Position pos : Position.values()) {
            for (PositionModifier mod : PositionModifier.values()) {
                if (mod == PositionModifier.IDENTITY) {
                    for (TokProperty prop : TokProperty.values()) {
                        tpls.add(new FeatTemplate1(pos, mod, prop));
                    }
                    for (TokPropList prop : TokPropList.values()) {
                        tpls.add(new FeatTemplate2(pos, mod, prop));
                    }
                } else {
                    for (TokProperty prop : coarseTokProps) {
                        tpls.add(new FeatTemplate1(pos, mod, prop));
                    }
                }
            }
        }
        ListModifier lmod = ListModifier.SEQ;
        for (PositionList pl : simplePosLists) {
            for (EdgeProperty eprop : Lists.getList(null, EdgeProperty.DIR)) {
                for (TokProperty prop : coarseTokProps) {
                    tpls.add(new FeatTemplate3(pl, prop, eprop, lmod));
                }
            }
        }
        for (OtherFeat feat : OtherFeat.values()) {
            tpls.add(new FeatTemplate4(feat));
        }
        return tpls;
    }
        
}
