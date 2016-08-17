package edu.jhu.pacaya.gm.feat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.TemplateFactor;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.util.CountingFeatureNames;
import edu.jhu.pacaya.util.FeatureNames;
import edu.jhu.prim.bimap.IntObjectBimap;

public class FactorTemplateList implements Serializable {

    private static final long serialVersionUID = 5880428795836008068L;
    private static final Logger log = LoggerFactory.getLogger(FactorTemplateList.class); 
    private List<FactorTemplate> fts;
    private boolean isGrowing;  
    private IntObjectBimap<Object> templateKeyAlphabet;
    private boolean useCountingAlphabets;

    public FactorTemplateList() {
        this(false);
    }
    
    public FactorTemplateList(boolean useCountingAlphabets) {
        fts = new ArrayList<FactorTemplate>();
        isGrowing = true;
        templateKeyAlphabet = new IntObjectBimap<Object>();
        this.useCountingAlphabets = useCountingAlphabets;
    }

    public FactorTemplate get(int i) {
        return fts.get(i);
    }
    
    public int size() {
        return fts.size();
    }

    /** Gets the number of observation function features. */
    public int getNumObsFeats() {
        int count = 0;
        for (FactorTemplate ft : fts) {
            count += ft.getAlphabet().size();
        }
        return count;
    }

    public void startGrowth() {
        for (FactorTemplate ft : fts) {
            ft.getAlphabet().startGrowth();
        }
        templateKeyAlphabet.startGrowth();
        isGrowing = true;
    }

    public void stopGrowth() {
        for (FactorTemplate ft : fts) {
            ft.getAlphabet().stopGrowth();
        }
        templateKeyAlphabet.stopGrowth();
        isGrowing = false;
    }
    
    public void add(FactorTemplate ft) {
        int index = templateKeyAlphabet.lookupIndex(ft.getKey());
        if (index >= fts.size()) {
            fts.add(ft);
        } else if (index == -1) {
            throw new RuntimeException("Unable to update factor template list for factor: " + ft.getKey());
        }
    }

    public void lookupTemplateIds(FactorGraph fg) {
        for (Factor f : fg.getFactors()) {
            if (f instanceof TemplateFactor) {
                lookupTemplateId((TemplateFactor) f);
            }
        }
    }
    
    public void getTemplateIds(FactorGraph fg) {
        for (Factor f : fg.getFactors()) {
            if (f instanceof TemplateFactor) {
                getTemplateId((TemplateFactor) f);
            }
        }
    }

    public FactorTemplate getTemplate(TemplateFactor f) {
        // getTemplateId might return -1 if the template factor hasn't been
        // seen before
        return fts.get(getTemplateId(f));
    }
    
    public int getTemplateId(TemplateFactor f) {
        // Try to get the cached id, and only lookup the id in the hash map if it's not there.
        if (f.getTemplateId() != -1) {
            return f.getTemplateId();
        } else {
            int id = getTemplateIdByKey(f.getTemplateKey());
            f.setTemplateId(id);
            return id;
        }
    }

    private int lookupTemplateId(TemplateFactor f) {
        int index = templateKeyAlphabet.lookupIndex(f.getTemplateKey());
        if (index >= fts.size()) {
            // Add the template.
            FeatureNames alphabet = useCountingAlphabets ? new CountingFeatureNames() : new FeatureNames();
            fts.add(new FactorTemplate(f.getVars(), alphabet, f.getTemplateKey()));
        } else if (index == -1) {
            log.warn(String.format("Tried to look up unknown feature template %s", f.getTemplateKey()));
        } else {
            FactorTemplate ft = fts.get(index);
            if (ft.getNumConfigs() != f.getVars().calcNumConfigs()) {
                // TODO: This is a bare-minimum check that the user defined the
                // template keys properly. Eventually we should probably define
                // some notion of variable type and check that a template has
                // the correct variable types.
                String msg1 = String.format(
                        "Expected %d variable assignments, but factor has %d. key = %s.", ft.getNumConfigs(), f
                                .getVars().calcNumConfigs(), f.getTemplateKey());
                log.error(msg1);
                String msg2 = "Expected var names: ";
                for (Var v : ft.getVars()) {
                    msg2 += v.getStateNames();
                    msg2 += ", ";
                }
                log.error(msg2);
                String msg3 = "Actual var names: ";
                for (Var v : f.getVars()) {
                    msg3 += v.getStateNames();
                    msg3 += ", ";
                }
                log.error(msg3);
                throw new IllegalStateException(msg1);
            }
        }
        f.setTemplateId(index);
        return index;
    }

    public FactorTemplate getTemplateByKey(Object templateKey) {
        return fts.get(getTemplateIdByKey(templateKey));
    }

    public int getTemplateIdByKey(Object templateKey) {
        return templateKeyAlphabet.lookupIndex(templateKey);
    }

    @Override
    public String toString() {
        return "FeatureTemplateList [isGrowing=" + isGrowing + ", fts=" + fts + "]";
    }

    public boolean isGrowing() {
        return isGrowing;
    }    
    
}
