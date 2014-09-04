package edu.jhu.data.concrete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.SituationMention;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.util.ConcreteUUIDFactory;

/**
 * Writer of Concrete files from {@link AnnoSentence}s.
 * 
 * @author Travis Wolfe
 */
public class ConcreteWriter {

    private static ConcreteUUIDFactory uuidFactory = new ConcreteUUIDFactory();
    
    private final long timestamp;     // time that every annotation that is processed will get
    private boolean careful;    // throw exceptions when things aren't unambiguously correct
    private final boolean srlIsSyntax;

    /**
     * @param srlIsSyntax
     * If true, we put SRL annotations in as dependency parses.
     * Dependency edges from root (gov=-1) represent predicates,
     * with the edge type giving the predicate sense. Arguments
     * are dependents of their predicate token, with the dependency
     * label capturing the argument label (e.g. "ARG0" and "ARG1").
     * 
     * Otherwise, we create a SituationMention for every predicate,
     * which have proper Arguments, each of which includes an EntityMention
     * that is added to its own EntityMentionSet (all EntityMentions created
     * by this tool in a document are unioned before making an EntityMentionSet).
     */
    public ConcreteWriter(boolean srlIsSyntax) {
        timestamp = System.currentTimeMillis();
        careful = true;
        this.srlIsSyntax = srlIsSyntax;
    }
    
    /**
     * Adds a dependency parse to the first concrete.Tokenization.
     */
    public void addDependencyParse(
            AnnoSentenceCollection containsDepParses,
            Communication addTo) {        

        List<Tokenization> ts = getTokenizationsCorrespondingTo(containsDepParses, addTo);
        for(int i=0; i<ts.size(); i++) {
            Tokenization t = ts.get(i);
            AnnoSentence s = containsDepParses.get(i);
            List<String> depTypes = s.getDeprels();
            int[] parents = s.getParents();
            t.addToDependencyParseList(makeDepParse(parents, depTypes));
        }
    }

    private DependencyParse makeDepParse(int[] parents, List<String> depRels) {
        if(depRels != null && parents.length != depRels.size())
            throw new IllegalArgumentException();
        
        DependencyParse p = new DependencyParse();
        p.uuid = uuidFactory.getConcreteUUID();
        p.metadata = new AnnotationMetadata();
        p.metadata.confidence = 1d;
        p.metadata.tool = "Pacaya dependency parser";
        p.metadata.timestamp = timestamp;
        p.dependencyList = new ArrayList<Dependency>();
        for(int i=0; i<parents.length; i++) {
            Dependency d = new Dependency();
            d.dep = i;
            d.gov = parents[i];
            d.edgeType = (depRels == null) ? "NO_LABEL" : depRels.get(i);
            p.dependencyList.add(d);
        }
        return p;
    }
    
    /**
     * behavior depends on {@code this.srlIsSyntax}
     */
    public void addSrlAnnotations(
            AnnoSentenceCollection containsSrl,
            edu.jhu.hlt.concrete.Communication addTo) {
        
        AnnotationMetadata meta = new AnnotationMetadata();
        meta = new AnnotationMetadata();
        meta.tool = "pacaya SRL";
        meta.confidence = 1d;
        meta.timestamp = timestamp;
        
        List<Tokenization> tokenizations = getTokenizationsCorrespondingTo(containsSrl, addTo);
        
        if(srlIsSyntax) {
            // make a dependency parse for every sentence / SRL
            for(int i=0; i<tokenizations.size(); i++) {
                AnnoSentence sent = containsSrl.get(i);
                Tokenization at = tokenizations.get(i);
                DependencyParse p = makeDependencyParse(sent.getSrlGraph(), sent, meta);
                at.dependencyParseList.add(p);
            }
        }
        else {
            // make a SituationMention for every sentence / SRL
            EntityMentionSet ems = new EntityMentionSet();
            ems.uuid = uuidFactory.getConcreteUUID();
            ems.metadata = meta;
            SituationMentionSet sms = new SituationMentionSet();
            sms.uuid = uuidFactory.getConcreteUUID();
            sms.metadata = meta;
            sms.mentionList = new ArrayList<SituationMention>();
            for(int i=0; i<containsSrl.size(); i++) {
                AnnoSentence sent = containsSrl.get(i);
                Tokenization t = tokenizations.get(i);
                sms.mentionList.addAll(makeSitutationMention(sent.getSrlGraph(), sent, t, ems));
            }
            addTo.entityMentionSets.add(ems);
            addTo.situationMentionSets.add(sms);
        }
    }
    
    private DependencyParse makeDependencyParse(SrlGraph srl, AnnoSentence from, AnnotationMetadata meta) {
        DependencyParse p = new DependencyParse();
        p.uuid = uuidFactory.getConcreteUUID();
        p.metadata = meta;
        p.dependencyList = new ArrayList<Dependency>();
        for(SrlPred pred : srl.getPreds()) {
            Dependency d = new Dependency();
            d.gov = -1;
            d.dep = pred.getPosition();
            d.edgeType = pred.getLabel();
            p.dependencyList.add(d);
            for(SrlEdge e : pred.getEdges()) {
                Dependency ed = new Dependency();
                ed.gov = pred.getPosition();
                ed.dep = e.getArg().getPosition();
                ed.edgeType = e.getLabel();
                p.dependencyList.add(ed);
            }
        }
        return p;
    }
    
    private List<SituationMention> makeSitutationMention(SrlGraph srl, AnnoSentence from, Tokenization useUUID, EntityMentionSet addEntityMentionsTo) {
        List<SituationMention> mentions = new ArrayList<SituationMention>();
        for(SrlPred p : srl.getPreds()) {
            SituationMention sm = new SituationMention();
            sm.text = from.getWord(p.getPosition());
            sm.confidence = 1d;
            sm.argumentList = new ArrayList<>();
            for(SrlEdge child : p.getEdges()) {
                int ai = child.getArg().getPosition();
                MentionArgument a = new MentionArgument();
                a.roleLabel = child.getLabel();
                
                // make an EntityMention
                EntityMention em = new EntityMention();
                em.uuid = uuidFactory.getConcreteUUID();
                em.confidence = 1d;
                em.entityType = "UNKNOWN";
                em.phraseType = "OTHER";
                em.text = from.getWord(ai);
                em.tokens = new TokenRefSequence();
                em.tokens.anchorTokenIndex = ai;
                em.tokens.tokenIndexList = Arrays.asList(ai);
                em.tokens.tokenizationId = useUUID.uuid;
                
                a.entityMentionId = em.uuid;
                addEntityMentionsTo.mentionSet.add(em);
            }
        }
        return mentions;
    }
    
    private List<Tokenization> getTokenizationsCorrespondingTo(AnnoSentenceCollection sentences, Communication from) {
        List<Tokenization> ts = new ArrayList<Tokenization>();
        if(careful && from.getSectionSegmentationsSize() != 1)
            throw new RuntimeException();
        SectionSegmentation ss = from.getSectionSegmentations().get(0);
        for(Section s : ss.getSectionList()) {
            if(careful && s.getSentenceSegmentationSize() != 1)
                throw new RuntimeException();
            SentenceSegmentation sentseg = s.getSentenceSegmentation().get(0);
            for(Sentence sent : sentseg.getSentenceList()) {
                if(careful && sent.getTokenizationListSize() != 1)
                    throw new RuntimeException();
                ts.add(sent.getTokenizationList().get(0));
            }
        }
        if(careful) {   // make sure that the sentences line up
            if(ts.size() != sentences.size())
                throw new RuntimeException();
            for(int i=0; i<ts.size(); i++) {
                if(ts.get(i).getTokenList().getTokensSize() != sentences.get(i).size())
                    throw new RuntimeException();
            }
        }
        return ts;
    }

}
