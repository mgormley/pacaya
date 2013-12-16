package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.simple.SimpleAnnoSentence;
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
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;

/**
 * Defines a feature template extractor for templates based on a 'little
 * language'.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class TemplateLanguageExtractor {

    private static final Logger log = Logger.getLogger(TemplateLanguageExtractor.class);

    //private final SimpleAnnoSentence sent;
    //private final CorpusStatistics cs;
    private final SrlBerkeleySignatureBuilder sig;
    private final FeaturizedSentence fSent; 
    
    public TemplateLanguageExtractor(SimpleAnnoSentence sent, CorpusStatistics cs) {
        //this.sent = sent;
        //this.cs = cs;
        this.sig = cs.sig;
        this.fSent = new FeaturizedSentence(sent, cs);
    }
    
    public void addFeatures(List<FeatTemplate> tpls, int pidx, int cidx, List<Object> feats) {
        for (FeatTemplate tpl : tpls) {
            addFeatures(tpl, pidx, cidx, feats);
        }
    }
    
    public void addFeatures(FeatTemplate tpl, int pidx, int cidx, List<Object> feats) {
        int idx = pidx;
        if (tpl instanceof FeatTemplate1) {
            addTokenFeature((FeatTemplate1) tpl, pidx, cidx, feats);
        } else if (tpl instanceof FeatTemplate2) {
            addTokenFeatures((FeatTemplate2) tpl, pidx, cidx, feats);            
        } else if (tpl instanceof FeatTemplate3) {
            addListFeature((FeatTemplate3) tpl, pidx, cidx, feats);
        } else if (tpl instanceof FeatTemplate4) {
            addOtherFeature((FeatTemplate4) tpl, pidx, cidx, feats);
        } else if (tpl instanceof BigramTemplate) {
            addBigramFeature((BigramTemplate) tpl, pidx, cidx, feats);
        } else {
            throw new IllegalStateException("Feature not supported: " + tpl);
        }
    }
        
    /**
     * For bigram feature templates of the form:
     *     p.w+c_{-1}.bc0
     *     p.t+c.t
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addBigramFeature(BigramTemplate tpl, int pidx, int cidx, List<Object> feats) {
        ArrayList<Object> feats1 = new ArrayList<Object>();
        ArrayList<Object> feats2 = new ArrayList<Object>();
        addFeatures(tpl.tpl1, pidx, cidx, feats1);
        addFeatures(tpl.tpl2, pidx, cidx, feats2);
        for (Object f1 : feats1) {
            for (Object f2 : feats2) {
                feats.add(toFeat(f1, f2));
            }
        }
    }

    /**
     * Adds features of the form: 
     *     p.bc1
     *     c_{head}.dr
     *     first(t, NOUN, path(p, root)).bc0
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addTokenFeature(FeatTemplate1 tpl, int pidx, int cidx, List<Object> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokProperty prop = tpl.prop;
        int idx = getIndexOfPosition(pidx, cidx, pos);
        idx = getModifiedPosition(mod, idx);
        String val = getTokProp(prop, idx);
        if (val != null) {
            Object feat = toFeat(tpl.getName(), val);
            feats.add(feat);
        }
    }

    private int getIndexOfPosition(int pidx, int cidx, Position pos) {
        int idx;
        switch (pos) {
        case PARENT: idx = pidx; break;
        case CHILD: idx = cidx; break;
        default: throw new IllegalStateException();
        }
        return idx;
    }
    
    /** Same as above except that it permits properties of the token which expand to multiple strings. */
    public void addTokenFeatures(FeatTemplate2 tpl, int pidx, int cidx, List<Object> feats) {
        Position pos = tpl.pos; PositionModifier mod = tpl.mod; TokPropList prop = tpl.prop;
        int idx = getIndexOfPosition(pidx, cidx, pos);
        idx = getModifiedPosition(mod, idx);
        List<String> vals = getTokPropList(prop, idx);
        for (String val : vals) {
            feats.add(toFeat(tpl.getName(), val));
        }
    }

    /**
     * Gets features of the form: 
     *    path(lca(p,c),root).bc0+dir.noDup
     *    children(p).bc0.seq
     *    line(p,c).t.noDup
     *    
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addListFeature(FeatTemplate3 tpl, int pidx, int cidx, List<Object> feats) {
        PositionList pl = tpl.pl; TokProperty prop = tpl.prop; EdgeProperty eprop = tpl.eprop; ListModifier lmod = tpl.lmod;
        
        if (prop == null && eprop == null) {
            throw new IllegalStateException("Feature template extracts nothing. One of prop and eprop must be non-null.");
        }
        
        Object feat;
        Collection<String> vals;
        switch (pl) {
        case CHILDREN_P: case NO_FAR_CHILDREN_P: case LINE_P_C:
            if (eprop != null) {
                throw new IllegalStateException("EdgeProperty " + eprop + " is only supported on paths.");
            } else if (prop == null) {
                throw new IllegalStateException("TokProperty must be non-null for position lists.");
            }
            List<Integer> indices = getPositionList(pl, pidx, cidx);
            vals = getTokPropsForList(prop, indices);
            vals = getModifiedList(lmod, vals);
            feat = toFeat(tpl.getName(), vals);
            feats.add(feat);
            return;
        case PATH_P_C: case PATH_C_LCA: case PATH_P_LCA: case PATH_LCA_ROOT: 
            List<Pair<Integer, Dir>> path = getPath(pl, pidx, cidx);
            vals = getTokPropsForPath(prop, eprop, path);
            vals = getModifiedList(lmod, vals);
            feat = toFeat(tpl.getName(), vals);
            feats.add(feat);
            return;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Gets special features.
     *
     * @param tpl Structured feature template.
     * @param pidx Token to which the parent position refers.
     * @param cidx Token to which the child position refers.
     * @param feats The feature list to which this will be added.
     */
    public void addOtherFeature(FeatTemplate4 tpl, int pidx, int cidx, List<Object> feats) {
        OtherFeat template = tpl.feat;  
        switch (template) {
        case PATH_GRAMS:
            List<Pair<Integer,Dir>> path = getPath(PositionList.PATH_P_C, pidx, cidx);  
            addPathGrams(tpl, path, feats);
            return;
        default:  
            String val = getOtherFeatSingleton(tpl.feat, pidx, cidx);
            feats.add(toFeat(tpl.getName(), val));
            return;
        }
    }

    // TODO: This is a lot of logic...and should probably live elsewhere.
    private void addPathGrams(FeatTemplate4 tpl, List<Pair<Integer, Dir>> path, List<Object> feats) {
        // For each path n-gram, for n in {1,2,3}:
        for (int n=1; n<=3; n++) {
            for (int start = 0; start <= path.size() - n; start++) {
                int end = start + n;
                List<Pair<Integer,Dir>> ngram = path.subList(start, end);
                // For each pattern of length n, comprised of WORD and POS.
                TokProperty[] props = new TokProperty[] { TokProperty.WORD, TokProperty.POS };
                int max = (int) Math.pow(2, n);
                for (int pattern = 0; pattern < max; pattern++) {
                    // Create the feature for this pattern.
                    List<String> vals = new ArrayList<String>(n);
                    for (int i=0; i<n; i++) {
                        // Get the appropriate type for this pattern:
                        // ((pattern>>>i) & 1) is 1 if the i'th bit is one and 0 otherwise.
                        TokProperty prop = props[(pattern>>>i) & 1];
                        vals.addAll(getTokPropsForPath(prop, null, ngram.subList(i, i+1)));
                    }
                    // Add the feature for this pattern.
                    feats.add(toFeat(tpl.getName(), vals));
                }
            }
        }
    }
    
    private String getOtherFeatSingleton(OtherFeat template, int pidx, int cidx) {
        FeaturizedToken ptok = getFeatTok(pidx);
        FeaturizedToken ctok = getFeatTok(cidx);
        FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
        switch (template) {
        case DISTANCE:
            return Integer.toString(Math.abs(pidx - cidx));
        case GENEOLOGY:
            return pair.getGeneologicalRelation();
        case PATH_LEN:            
            return Integer.toString(pair.getDependencyPath().size());
        case RELATIVE:
            return pair.getRelativePosition();
        case CONTINUITY:
            return Integer.toString(pair.getCountOfNonConsecutivesInPath());
        default:
            throw new IllegalStateException();
        }
    }

    private <T> Collection<T> getModifiedList(ListModifier lmod, Collection<T> props) {
        switch (lmod) {
        case SEQ:
            return props;
        case BAG:
            return bag(props);
        case NO_DUP:
            return noDup(props);
        default:
            throw new IllegalStateException();
        }
    }

    private ArrayList<Integer> getPositionList(PositionList pl, int pidx, int cidx) {        
        FeaturizedToken ptok;
        switch (pl) {
        case CHILDREN_P: 
            ptok = getFeatTok(pidx);
            return ptok.getChildren();
        case NO_FAR_CHILDREN_P: 
            ptok = getFeatTok(pidx);
            return ptok.getNoFarChildren();
        case LINE_P_C: 
            FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
            return pair.getLinePath();
        default:
            throw new IllegalStateException();
        }
    }
    
    private List<Pair<Integer, Dir>> getPath(PositionList pl, int pidx, int cidx) {
        FeaturizedTokenPair pair = getFeatTokPair(pidx, cidx);
        switch (pl) {
        case PATH_P_C:
            return pair.getDependencyPath();
        case PATH_C_LCA:
            return pair.getDpPathArg();
        case PATH_P_LCA:
            return pair.getDpPathPred();
        case PATH_LCA_ROOT:
            return pair.getDpPathShare();
        default:
            throw new IllegalStateException();
        }
    }
    
    private int getModifiedPosition(PositionModifier mod, int idx) {
        FeaturizedToken tok = null;
        if (mod != PositionModifier.IDENTITY && mod != PositionModifier.AFTER1 && mod != PositionModifier.BEFORE1) {
            tok = getFeatTok(idx);
        }
        switch(mod) {        
        case LOW_SV: case LOW_SN: case HIGH_SV: case HIGH_SN: 
            log.warn("Assuming Spanish when creating high/low support feature.");
            break;
        default: break;
        }
        
        switch (mod) {
            // --------------------- Word ---------------------  
        case IDENTITY:
            return idx;
        case BEFORE1:
            return idx - 1;
        case AFTER1:
            return idx + 1;
            // --------------------- DepTree ---------------------  
        case HEAD:
            return tok.getParent();
        case LNS:
            return tok.getNearLeftSibling();
        case RNS:
            return tok.getNearRightSibling();
        case LMC:
            return tok.getFarLeftChild();
        case RMC:
            return tok.getFarRightChild();
        case LNC:
            return tok.getNearLeftChild();
        case RNC:
            return tok.getNearRightChild();     
        case LOW_SV:
            return tok.getLowSupportVerb();
        case LOW_SN:
            return tok.getLowSupportNoun();
        case HIGH_SV:
            return tok.getHighSupportVerb();
        case HIGH_SN:
            return tok.getHighSupportNoun();
        default:
            throw new IllegalStateException();
        }
    }
    
    private List<String> getTokPropsForPath(TokProperty prop, EdgeProperty eprop, List<Pair<Integer,Dir>> path) {
        List<String> props = new ArrayList<String>(path.size());
        for (int i=0; i<path.size(); i++) {
            Pair<Integer,Dir> edge = path.get(i);
            if (prop != null) {
                String val = getTokProp(prop, edge.get1());
                if (val != null) {
                    props.add(val);
                }
            }
            if (eprop != null && i < path.size() - 1) {
                switch (eprop) {
                case DIR: props.add(edge.get2().name()); break;
                case EDGEREL:
                    int idx1 = path.get(i).get1();
                    int idx2 = path.get(i+1).get1();
                    Dir d = path.get(i).get2();
                    int idx = (d == Dir.UP) ? idx1 : idx2;
                    props.add(getTokProp(TokProperty.DEPREL, idx));                    
                    break;
                default: throw new IllegalStateException();
                }
                
            }
        }
        return props;
    }

    private List<String> getTokPropsForList(TokProperty prop, List<Integer> indices) {
        List<String> props = new ArrayList<String>(indices.size());
        for (int idx : indices) {
            String val = getTokProp(prop, idx);
            if (val != null) {
                props.add(val);
            }
        }
        return props;
    }

    // package private for testing.
    List<String> getTokPropList(TokPropList prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        switch (prop) {
        case EACH_MORPHO:
            return tok.getFeat();
        default:
            throw new IllegalStateException();
        }
    }
    
    /**
     * @return The property or null if the property is not included.
     */
    // package private for testing.
    String getTokProp(TokProperty prop, int idx) {
        FeaturizedToken tok = getFeatTok(idx);
        switch (prop) {
        case WORD:
            return tok.getForm();
        case LC:
            //TODO: return tok.getFormLc();
            return tok.getForm().toLowerCase();
        case CHPRE5:
            String form = tok.getForm();
            if (form.length() > 5) {
                return form.substring(0, Math.min(form.length(), 5));    
            } else {
                return null;
            }
        case LEMMA:
            return tok.getLemma();
        case POS:
            return tok.getPos();
        case BC0:
            String cluster = tok.getCluster();
            return cluster.substring(0, Math.min(cluster.length(), 5));    
        case BC1:
            return tok.getCluster();
        case DEPREL:
            return tok.getDeprel();
        case MORPHO:
            return tok.getFeatStr();
        case UNK:
            log.warn("Assuming Spanish when creating UNK feature.");            
            return sig.getSignature(tok.getForm(), idx, "es");
        default:
            throw new IllegalStateException();
        }
    }
    
    public static <T> Collection<T> bag(Collection<T> elements) {
        // bag, which removes all duplicated strings and sort the rest
        return new TreeSet<T>(elements);
    }
    
    public static <T> ArrayList<T> noDup(Collection<T> elements) {
        // noDup, which removes all duplicated neighbored strings.
        ArrayList<T> noDupElements = new ArrayList<T>();
        T lastA = null;
        for (T a : elements) {
            if (!a.equals(lastA)) {
                noDupElements.add(a);
            }
            lastA = a;
        }
        return noDupElements;
    }
    
    private FeaturizedToken getFeatTok(int idx) {
        return fSent.getFeatTok(idx);
    }
    
    private FeaturizedTokenPair getFeatTokPair(int pidx, int cidx) {
        return fSent.getFeatTokPair(pidx, cidx);
    }
    
    private Object toFeat(String... vals) {
        return org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private Object toFeat(String name, Collection<String> vals) {
        return name + "_" + org.apache.commons.lang.StringUtils.join(vals, "_");
    }

    private Object toFeat(Object f1, Object f2) {
        if (f1 instanceof String && f2 instanceof String) {
            return f1 + "_" + f2;
        } else {
            return new Pair<Object,Object>(f1, f2);
        }
    }

}
