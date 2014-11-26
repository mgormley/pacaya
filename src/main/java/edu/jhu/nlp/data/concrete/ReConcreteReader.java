package edu.jhu.nlp.data.concrete;

import java.util.Collections;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.nlp.relations.RelationsOptions;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 */
public class ReConcreteReader extends ConcreteReader {

    public static class ReConcreteReaderPrm extends ConcreteReaderPrm {
        private static final long serialVersionUID = 1L;
        public boolean makeSingletons = true;
    }
    
    private static final Logger log = LoggerFactory.getLogger(ReConcreteReader.class);
    private ReConcreteReaderPrm prm;
    
    public ReConcreteReader(ReConcreteReaderPrm prm) {
        super(prm);
        this.prm = prm;
    }

    /**
     * Converts each sentence in communication to a {@link AnnoSentence}
     * and adds it to annoSents.
     */
    @Override
    public void addSentences(Communication comm, AnnoSentenceCollection aSents) {
        AnnoSentenceCollection tmpSents = new AnnoSentenceCollection(); 
        super.addSentences(comm, tmpSents);
        
        if (comm.getEntityMentionSetListSize() > 0 && comm.getSituationMentionSetListSize() > 0) {                
            // TODO: This is a hack to replicate the PM13 setting. Think of a better way to incorporate this.
            for (AnnoSentence aSent : tmpSents) {     
                if (RelationsOptions.shortenEntityMentions) {
                    for (NerMention m : aSent.getNamedEntities()) {
                        // Set the end of the span to be the head token.
                        m.getSpan().setEnd(m.getHead()+1);
                    }
                    aSent.getNamedEntities().sort();
                }
                // Add the named entity pairs.
                RelationsEncoder.addNePairsAndRelLabels(aSent);
            }
            if (prm.makeSingletons) {
                tmpSents = RelationsEncoder.getSingletons(tmpSents);
            }
            // Deterministically shuffle the positive and negative examples for this communication.
            Collections.shuffle(tmpSents, new Random(1234567890));
        }
        
        aSents.addAll(tmpSents);
    }
    
}
