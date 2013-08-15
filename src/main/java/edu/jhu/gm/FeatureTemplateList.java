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
    }
    
    public void add(FeatureTemplate ft) {
        fts.add(ft);
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
        return fts.get(lookupTemplateId(f));
    }
    
    public int lookupTemplateId(Factor f) {
        // TODO: This might be too slow.
        return templateKeyAlphabet.lookupIndex(f.getTemplateKey());
    }
}
