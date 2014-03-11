package edu.jhu.featurize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate0;
import edu.jhu.featurize.TemplateLanguage.JoinTemplate;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;
import edu.jhu.util.collections.Lists;

public class TemplateSets {

    // Semantic Role Labeling feature sets.
    public static final String bjorkelundArgFeatsResource = "/edu/jhu/featurize/bjorkelund-arg-feats.txt";
    public static final String bjorkelundSenseFeatsResource = "/edu/jhu/featurize/bjorkelund-sense-feats.txt";

    public static final String naradowskyArgFeatsResource = "/edu/jhu/featurize/naradowsky-arg-feats.txt";
    public static final String naradowskySenseFeatsResource = "/edu/jhu/featurize/naradowsky-sense-feats.txt";
    
    public static final String zhaoCaArgFeatsResource = "/edu/jhu/featurize/zhao-ca-arg-feats.txt";
    public static final String zhaoEnSenseFeatsResource = "/edu/jhu/featurize/zhao-en-sense-feats.txt";

    // Dependency Parsing feature sets.
    public static final String mcdonaldDepFeatsResource = "/edu/jhu/featurize/mcdonald-dep-feats.txt";
    public static final String kooBasicDepFeatsResource = "/edu/jhu/featurize/koo-basic-dep-feats.txt";
    public static final String kooHybridDepFeatsResource = "/edu/jhu/featurize/koo-hybrid-dep-feats.txt";
    public static final String carreras07Dep2FeatsResource = "/edu/jhu/featurize/carreras07-dep2-feats.txt";
    
    // Constituency Parsing feature sets
    public static final String finkel08FeatsResource = "/edu/jhu/featurize/finkel08-parse-feats.txt";
    
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
                        if (isValidFeatTemplate3(pl, prop, eprop, lmod)) {
                            tpls.add(new FeatTemplate3(pl, prop, eprop, lmod));
                        }
                    }
                }
            }
        }     
        for (OtherFeat feat : OtherFeat.values()) {
            tpls.add(new FeatTemplate0(feat));
        }
        return tpls;
    }
    
    private static boolean isValidFeatTemplate3(PositionList pl, TokProperty prop, EdgeProperty eprop, ListModifier lmod) {
        // This check is rather messy. It'd be better if we just had separate
        // structured templates for extracting TokProperties and EdgeProperties
        // and they were always combined by conjunction (+). 
        if (prop == null && eprop == null) {
            return false;
        } else if (!pl.isPath() && (prop == null || eprop != null)) {
            return false;
        }
        return true;
    }

    public static List<FeatTemplate> getAllBigramFeatureTemplates() {
        List<FeatTemplate> unigrams = getAllUnigramFeatureTemplates();
        return getBigramFeatureTemplates(unigrams);
    }

    public static List<FeatTemplate> getBigramFeatureTemplates(List<FeatTemplate> unigrams) {
        return getBigramFeatureTemplates(unigrams, unigrams);
    }

    public static List<FeatTemplate> getBigramFeatureTemplates(List<FeatTemplate> unigrams1, List<FeatTemplate> unigrams2) {
        ArrayList<FeatTemplate> bs = new ArrayList<FeatTemplate>();
        for (int i=0; i<unigrams1.size(); i++) {
            for (int j=i+1; j<unigrams2.size(); j++) {
                bs.add(new JoinTemplate(unigrams1.get(i), unigrams2.get(j)));
            }
        }
        return bs;
    }

    public static List<FeatTemplate> getFromResource(String resourceName) {
        TemplateReader reader = new TemplateReader();
        try {
            reader.readFromResource(resourceName);
            return reader.getTemplates();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
    
    public static List<FeatTemplate> getBjorkelundSenseUnigramFeatureTemplates() {
        return getFromResource(bjorkelundSenseFeatsResource);
    }
    
    public static List<FeatTemplate> getBjorkelundArgUnigramFeatureTemplates() {
        return getFromResource(bjorkelundArgFeatsResource);
    }

    public static List<FeatTemplate> getNaradowskySenseUnigramFeatureTemplates() {
        return getFromResource(naradowskySenseFeatsResource);
    }
    
    public static List<FeatTemplate> getNaradowskyArgUnigramFeatureTemplates() {
        return getFromResource(naradowskyArgFeatsResource);
    }
    
    public static List<FeatTemplate> getZhaoEnSenseUnigramFeatureTemplates() {
        return getFromResource(zhaoEnSenseFeatsResource);
    }
    
    public static List<FeatTemplate> getZhaoCaArgUnigramFeatureTemplates() {
        return getFromResource(zhaoCaArgFeatsResource);
    }
    
    public static List<FeatTemplate> getCustomArgSet1() {
        Collection<FeatTemplate> tpls = new HashSet<FeatTemplate>();        
        tpls.addAll(TemplateSets.getCoarseUnigramSet1());
        tpls.addAll(TemplateSets.getBjorkelundArgUnigramFeatureTemplates());
        tpls.addAll(TemplateSets.getZhaoCaArgUnigramFeatureTemplates());
        tpls.addAll(TemplateSets.getNaradowskyArgUnigramFeatureTemplates());
        return new ArrayList<FeatTemplate>(tpls);
    }

    public static List<FeatTemplate> getCustomSenseSet1() {
        Collection<FeatTemplate> tpls = new HashSet<FeatTemplate>();        
        tpls.addAll(TemplateSets.getCoarseUnigramSet1());
        for (TokProperty prop : TokProperty.values()) {
            tpls.add(new FeatTemplate1(Position.PARENT, PositionModifier.IDENTITY, prop));
        }
        tpls.addAll(TemplateSets.getBjorkelundSenseUnigramFeatureTemplates());
        tpls.addAll(TemplateSets.getZhaoEnSenseUnigramFeatureTemplates());
        tpls.addAll(TemplateSets.getNaradowskySenseUnigramFeatureTemplates());
        return TemplateLanguage.filterOutFeats(new ArrayList<FeatTemplate>(tpls), TokProperty.UNK);
    }
    
    public static List<FeatTemplate> getCoarseUnigramSet1() {
        TokProperty[] coarseTokProps = new TokProperty[] { TokProperty.POS, TokProperty.DEPREL, TokProperty.BC0};
        TokPropList[] coarseTokPropList = new TokPropList[]{ };
        PositionList[] simplePosLists = new PositionList[] { PositionList.LINE_P_C, PositionList.CHILDREN_P, PositionList.PATH_P_C };
        ListModifier[] listModifiers = new ListModifier[]{ ListModifier.SEQ };

        TokProperty[] fineTokProps = TokProperty.values();
        TokPropList[] fineTokPropLists = TokPropList.values();
        OtherFeat[] otherFeats = OtherFeat.values();
        PositionModifier[] positionModifiers = PositionModifier.values();
        Position[] positions = Position.values();

        return coarseUnigramSetCreator(coarseTokProps, coarseTokPropList, simplePosLists, listModifiers, otherFeats, positionModifiers,
                positions, fineTokProps, fineTokPropLists);
    }
    
    public static List<FeatTemplate> getCoarseUnigramSet2() {
        TokProperty[] coarseTokProps = new TokProperty[] { TokProperty.POS, TokProperty.DEPREL, TokProperty.BC0}; //, TokProperty.BC1, TokProperty.WORD_TOP_N };
        TokPropList[] coarseTokPropList = new TokPropList[]{ };
        PositionList[] simplePosLists = new PositionList[] { PositionList.LINE_P_C, PositionList.CHILDREN_P, PositionList.PATH_P_C };
        ListModifier[] listModifiers = new ListModifier[]{ ListModifier.SEQ };
        
        TokProperty[] fineTokProps = new TokProperty[] {  TokProperty.BC1, TokProperty.WORD, TokProperty.LEMMA, TokProperty.CHPRE5, TokProperty.CAPITALIZED, TokProperty.POS, TokProperty.DEPREL, TokProperty.BC0 };
        TokPropList[] fineTokPropLists = new TokPropList[]{ TokPropList.EACH_MORPHO };
        
        OtherFeat[] otherFeats = OtherFeat.values();
        PositionModifier[] positionModifiers = PositionModifier.values();
        Position[] positions = Position.values();

        return coarseUnigramSetCreator(coarseTokProps, coarseTokPropList, simplePosLists, listModifiers, otherFeats, positionModifiers,
                positions, fineTokProps, fineTokPropLists);
    }

    private static List<FeatTemplate> coarseUnigramSetCreator(TokProperty[] coarseTokProps, TokPropList[] coarseTokPropList,
            PositionList[] simplePosLists, ListModifier[] listModifiers, OtherFeat[] otherFeats,
            PositionModifier[] positionModifiers, Position[] positions, TokProperty[] fineTokProps, TokPropList[] fineTokPropLists) {
        ArrayList<FeatTemplate> tpls = new ArrayList<FeatTemplate>();
        for (Position pos : positions) {
            for (PositionModifier mod : positionModifiers) {
                if (mod == PositionModifier.IDENTITY) {
                    for (TokProperty prop : fineTokProps) {
                        tpls.add(new FeatTemplate1(pos, mod, prop));
                    }
                    for (TokPropList prop : fineTokPropLists) {
                        tpls.add(new FeatTemplate2(pos, mod, prop));
                    }
                } else {
                    for (TokProperty prop : coarseTokProps) {
                        tpls.add(new FeatTemplate1(pos, mod, prop));
                    }
                    for (TokPropList prop : coarseTokPropList) {
                        tpls.add(new FeatTemplate2(pos, mod, prop));
                    }
                }
            }
        }
        for (PositionList pl : simplePosLists) {
            for (ListModifier lmod : listModifiers) {
                for (EdgeProperty eprop : Lists.getList(null, EdgeProperty.DIR)) {
                    for (TokProperty prop : coarseTokProps) {
                        if (isValidFeatTemplate3(pl, prop, eprop, lmod)) {
                            tpls.add(new FeatTemplate3(pl, prop, eprop, lmod));
                        }
                    }
                }
            }
        }

        for (OtherFeat feat : otherFeats) {
            tpls.add(new FeatTemplate0(feat));
        }
        
        // TODO: UNK is currently only supported for English and Spanish, so we filter those feature out.
        tpls = TemplateLanguage.filterOutFeats(tpls, TokProperty.UNK);
        
        return tpls;
    }
        
}
