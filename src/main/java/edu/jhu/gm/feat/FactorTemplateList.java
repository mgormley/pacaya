package edu.jhu.gm.feat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.GlobalFactor;
import edu.jhu.gm.model.UnsupportedFactorTypeException;
import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;

public class FactorTemplateList implements Serializable {

    private static final long serialVersionUID = 5880428795836008068L;
    private List<FactorTemplate> fts;
    private boolean isGrowing;  
    private Alphabet<Object> templateKeyAlphabet;
    private boolean useCountingAlphabets;

    public FactorTemplateList() {
        this(false);
    }
    
    public FactorTemplateList(boolean useCountingAlphabets) {
        fts = new ArrayList<FactorTemplate>();
        isGrowing = true;
        templateKeyAlphabet = new Alphabet<Object>();
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
            throw new RuntimeException("Unable to update feature template list for factor: " + ft.getKey());
        }
    }

    public void update(FactorGraph fg) {
        for (Factor f : fg.getFactors()) {
            if (f instanceof GlobalFactor) {
                continue;
            } else if (f instanceof ExpFamFactor) {
                lookupTemplateId(f);
            } else {
                throw new UnsupportedFactorTypeException(f);
            }            
        }
    }

    private FactorTemplate lookupTemplate(Factor f) {
        return fts.get(getTemplateId(f));
    }
    
    private int lookupTemplateId(Factor f) {
        int index = templateKeyAlphabet.lookupIndex(f.getTemplateKey());
        if (index >= fts.size()) {
            // Add the template.
            Alphabet<Feature> alphabet = useCountingAlphabets ? new CountingAlphabet<Feature>() : new Alphabet<Feature>();
            fts.add(new FactorTemplate(f.getVars(), alphabet, f.getTemplateKey()));
        } else if (index == -1) {
            throw new RuntimeException("Unable to update feature template list for factor: " + f.getTemplateKey());
        } else if (fts.get(index).getNumConfigs() != f.getVars().calcNumConfigs()) {
            // TODO: This is a bare-minimum check that the user defined the
            // template keys properly. Eventually we should probably define
            // some notion of variable type and check that a template has
            // the correct variable types.
            throw new IllegalStateException(String.format(
                    "Expected %d variable assignments, but factor has %d. key = %s.", fts.get(index).getNumConfigs(), f
                            .getVars().calcNumConfigs(), f.getTemplateKey()));
        }
        return index;
    }

    public FactorTemplate getTemplate(Factor f) {
        return fts.get(getTemplateId(f));
    }
    
    public int getTemplateId(Factor f) {
        // Try to get the cached id, and only lookup the id in the hash map if it's not there.
        if (f.getTemplateId() != -1) {
            return f.getTemplateId();
        } else {
            int id = getTemplateIdByKey(f.getTemplateKey());
            f.setTemplateId(id);
            return id;
        }
    }

    public FactorTemplate getTemplateByKey(Object templateKey) {
        return fts.get(getTemplateIdByKey(templateKey));
    }

    public int getTemplateIdByKey(Object templateKey) {
        // TODO: This might be too slow.
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
