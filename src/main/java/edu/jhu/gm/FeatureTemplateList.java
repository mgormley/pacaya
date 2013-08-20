package edu.jhu.gm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.util.Alphabet;

public class FeatureTemplateList implements Serializable {

    private static final long serialVersionUID = 5880428795836008068L;
    private List<FeatureTemplate> fts;
    private boolean isGrowing;  
    private Alphabet<Object> templateKeyAlphabet;

    public FeatureTemplateList() {
        fts = new ArrayList<FeatureTemplate>();
        isGrowing = true;
        templateKeyAlphabet = new Alphabet<Object>();
    }

    public void update(FactorGraph fg) {
        for (Factor f : fg.getFactors()) {
            int index = templateKeyAlphabet.lookupIndex(f.getTemplateKey());
            if (index >= fts.size()) {
                // Add the template.
                fts.add(new FeatureTemplate(f.getVars(), new Alphabet<Feature>(), f.getTemplateKey()));
            } else if (index == -1) {
                throw new RuntimeException("Unable to update feature template list for factor: " + f.getTemplateKey());
            } else if (fts.get(index).getNumConfigs() != f.getVars().calcNumConfigs()) {
                // TODO: This is a bare-minimum check that the user defined the
                // template keys properly. Eventually we should probably define
                // some notion of variable type and check that a template has
                // the correct variable types.
                throw new IllegalStateException("Mismatch between number of variable configurations and the feature template.");
            }
        }
    }
    
    public void add(FeatureTemplate ft) {
        int index = templateKeyAlphabet.lookupIndex(ft.getKey());
        if (index >= fts.size()) {
            fts.add(ft);
        } else if (index == -1) {
            throw new RuntimeException("Unable to update feature template list for factor: " + ft.getKey());
        }
    }
    
    public FeatureTemplate get(int i) {
        return fts.get(i);
    }
    
    public int size() {
        return fts.size();
    }

    /** Gets the number of observation function features. */
    public int getNumObsFeats() {
        int count = 0;
        for (FeatureTemplate ft : fts) {
            count += ft.getAlphabet().size();
        }
        return count;
    }

    public void stopGrowth() {
        for (FeatureTemplate ft : fts) {
            ft.getAlphabet().stopGrowth();
        }
        isGrowing = false;
    }

    public FeatureTemplate lookupTemplate(Factor f) {
        return lookupTemplateByKey(f.getTemplateKey());
    }
    
    public int lookupTemplateId(Factor f) {
        return lookupTemplateIdByKey(f.getTemplateKey());
    }

    public FeatureTemplate lookupTemplateByKey(Object templateKey) {
        return fts.get(lookupTemplateIdByKey(templateKey));
    }

    public int lookupTemplateIdByKey(Object templateKey) {
        // TODO: This might be too slow.
        return templateKeyAlphabet.lookupIndex(templateKey);
    }
    
}
