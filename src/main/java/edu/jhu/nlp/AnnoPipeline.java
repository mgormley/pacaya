package edu.jhu.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

/**
 * Pipeline of Annotators each of which is optionally trainable.
 * 
 * @author mgormley
 */
public class AnnoPipeline implements Trainable, Annotator {

    private static final long serialVersionUID = 1L;
    private List<Annotator> pipeline = new ArrayList<Annotator>();
    
    public void add(Annotator anno) {
        pipeline.add(anno);
    }
    
    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold, 
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        for (Annotator anno : pipeline) {
            if (anno instanceof Trainable) {
                ((Trainable) anno).train(trainInput, trainGold, devInput, devGold);
            }
            anno.annotate(trainInput);
            if (devInput != null) {
                anno.annotate(devInput);
            }
        }
    }
    
    @Override
    public void annotate(AnnoSentenceCollection sents) {
        for (Annotator anno : pipeline) {
            anno.annotate(sents);
        }
    }
    
    @Override
    public Set<AT> getAnnoTypes() {
        HashSet<AT> ats = new HashSet<>();
        for (Annotator anno : pipeline) {
            ats.addAll(anno.getAnnoTypes());
        }
        return ats;
    }

}
