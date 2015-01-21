package edu.jhu.nlp.data.concrete;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.hlt.concrete.AnnotationMetadata;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SituationMention;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.util.ConcreteUUIDFactory;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

/**
 * Writer of Concrete files from {@link AnnoSentence}s.
 * 
 * @author Travis Wolfe
 * @author mgormley
 */
public class ConcreteWriter {

    public static class ConcreteWriterPrm {   
        private static final Logger log = LoggerFactory.getLogger(ConcreteWriterPrm.class);
        /* ----- Whether to include each annotation layer ----- */
        /** Whether to add the dependency parses. */
        public boolean addDepParse = true;
        /** Whether to add SRL. */
        public boolean addSrl = true;
        /** Whether to add NER mentions. */
        public boolean addNerMentions = true;
        /** Whether to add relations. */
        public boolean addRelations = true;
        /* ---------------------------------------------------- */
        /** Whether to write out SRL as a labeled dependency tree (i.e. syntax) or as SituationMentions. */
        public boolean srlIsSyntax = false;
        /** Sets the include flag for each annotation type to true, or warns if it's not supported. */
        public void addAnnoTypes(Collection<AT> ats) {
            for (AT at : ats) {
                switch (at) {
                case DEP_TREE: this.addDepParse = true; break;
                case SRL: this.addSrl = true; break;
                case NER: this.addNerMentions = true; break;
                case RELATIONS: this.addRelations = true; break;
                default: log.warn("Annotations of type {} are not supported by ConcreteWriter and will not be added to Concrete Communications.", at);
                }
            }
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(ConcreteWriter.class);

    public static final String DEP_PARSE_TOOL = "Pacaya Dependency Parser";
    public static final String SRL_TOOL = "Pacaya Semantic Role Labeler (SRL)";
    
    private static ConcreteUUIDFactory uuidFactory = new ConcreteUUIDFactory();
    
    private final long timestamp;     // time that every annotation that is processed will get
    private final ConcreteWriterPrm prm;

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
    public ConcreteWriter(ConcreteWriterPrm prm) {
        this.timestamp = System.currentTimeMillis();
        this.prm = prm;
    }

    public void write(AnnoSentenceCollection sents, File out) throws IOException {
        List<Communication> comms = (List<Communication>) sents.getSourceSents();
        if (out.getName().endsWith(".zip")) {
            throw new RuntimeException("Zip file output not yet supported for Concrete");
        } else {
            if (comms.size() > 1) {
                throw new RuntimeException("Multiple Communications in input cannot be written to a single Communication as output.");
            } else if (comms.size() == 0) {
                throw new RuntimeException("No Communication in sourceSents field.");
            }
            Communication comm = comms.get(0);
            comm = comm.deepCopy();
            addAnnotations(sents, comm);
            try {
                CompactCommunicationSerializer ser = new CompactCommunicationSerializer();
                byte[] bytez =ser.toBytes(comm);
                Files.write(Paths.get(out.getAbsolutePath()), bytez);
            } catch (ConcreteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Adds the annotations from the {@link AnnoSentenceCollection} to the {@link Communication}. */
    public void addAnnotations(AnnoSentenceCollection sents, Communication comm) {
        int numSents = TokenizationUtils.getNumSents(comm);
        if (numSents != sents.size()) {
            log.error(String.format("# sents in Communication = %d # sents in AnnoSentenceCollection = %d", numSents, sents.size()));
            log.error("The number of sentences in the Communication do not match the number in the AnnoSentenceCollection." +
                    "This can occur when the maximum sentence length or the total number of sentences is restricted.");
            throw new RuntimeException("The number of sentences in the Communication do not match the number in the AnnoSentenceCollection.");
        }
        if (prm.addDepParse) {
            addDependencyParse(sents, comm);
        }
        if (prm.addSrl) {
            addSrlAnnotations(sents, comm);
        }
        if (prm.addNerMentions || prm.addRelations) {
            addNerMentionsAndRelations(sents, comm);
        }
    }

    /**
     * Adds a dependency parse from each sentence in the {@link AnnoSentenceCollection} to each
     * sentence's concrete.Tokenization.
     */
    public void addDependencyParse(
            AnnoSentenceCollection sents,
            Communication comm) {
        if (!sents.someHaveAt(AT.DEP_TREE)) { return; } 
        List<Tokenization> ts = getTokenizationsCorrespondingTo(sents, comm);
        for(int i=0; i<ts.size(); i++) {
            Tokenization t = ts.get(i);
            AnnoSentence s = sents.get(i);
            List<String> depTypes = s.getDeprels();
            int[] parents = s.getParents();
            if (parents != null) {
                t.addToDependencyParseList(makeDepParse(parents, depTypes));
            }
       }
    }

    private DependencyParse makeDepParse(int[] parents, List<String> depRels) {
        if(depRels != null && parents.length != depRels.size()) {
            throw new IllegalArgumentException("Parents length doesn't match depRels length");
        }
        DependencyParse p = new DependencyParse();
        p.setUuid(uuidFactory.getConcreteUUID());
        AnnotationMetadata metadata = new AnnotationMetadata();
        metadata.setTool(DEP_PARSE_TOOL);
        metadata.setTimestamp(timestamp);
        p.setMetadata(metadata);
        p.setDependencyList(new ArrayList<Dependency>());        
        for(int i=0; i<parents.length; i++) {
            if (parents[i] == -2) { continue; }
            Dependency d = new Dependency();
            d.setDep(i);
            d.setGov(parents[i]);
            if (depRels != null && depRels.get(i) != null) {
                d.setEdgeType(depRels.get(i));
            }
            p.addToDependencyList(d);
        }
        return p;
    }
    
    /**
     * behavior depends on {@code this.srlIsSyntax}
     */
    public void addSrlAnnotations(
            AnnoSentenceCollection sents,
            Communication comm) {    
        if (!sents.someHaveAt(AT.SRL)) { return; }
        
        AnnotationMetadata meta = new AnnotationMetadata();
        meta = new AnnotationMetadata();
        meta.setTool(SRL_TOOL);
        meta.setTimestamp(timestamp);
        
        List<Tokenization> tokenizations = getTokenizationsCorrespondingTo(sents, comm);
        
        if(prm.srlIsSyntax) {
            // make a dependency parse for every sentence / SRL
            for(int i=0; i<tokenizations.size(); i++) {
                AnnoSentence sent = sents.get(i);
                Tokenization at = tokenizations.get(i);
                if (sent.getSrlGraph() != null) {
                    DependencyParse p = makeDependencyParse(sent.getSrlGraph(), sent, meta);
                    at.addToDependencyParseList(p);
                }
            }
        } else {
            // make a SituationMention for every sentence / SRL
            EntityMentionSet ems = new EntityMentionSet();
            ems.setUuid(uuidFactory.getConcreteUUID());
            ems.setMetadata(meta);
            SituationMentionSet sms = new SituationMentionSet();
            sms.setUuid(uuidFactory.getConcreteUUID());
            sms.setMetadata(meta);
            sms.setMentionList(new ArrayList<SituationMention>());
            for(int i=0; i<sents.size(); i++) {
                AnnoSentence sent = sents.get(i);
                Tokenization t = tokenizations.get(i); 
                if (sent.getSrlGraph() != null) {
                    for(SituationMention sm : makeSitutationMentions(sent.getSrlGraph(), sent, t, ems)) {
                        sms.addToMentionList(sm);
                    }
                }
            }
            comm.addToEntityMentionSetList(ems);
            comm.addToSituationMentionSetList(sms);
        }
    }
    
    private DependencyParse makeDependencyParse(SrlGraph srl, AnnoSentence from, AnnotationMetadata meta) {
        DependencyParse p = new DependencyParse();
        p.setUuid(uuidFactory.getConcreteUUID());
        p.setMetadata(meta);
        p.setDependencyList(new ArrayList<Dependency>());
        for(SrlPred pred : srl.getPreds()) {
            {
                Dependency d = new Dependency();
                d.setGov(-1);
                d.setDep(pred.getPosition());
                d.setEdgeType(pred.getLabel());
                p.addToDependencyList(d);
            }
            for(SrlEdge e : pred.getEdges()) {
                Dependency ed = new Dependency();
                ed.setGov(pred.getPosition());
                ed.setDep(e.getArg().getPosition());
                ed.setEdgeType(e.getLabel());
                p.addToDependencyList(ed);
            }
        }
        return p;
    }
    
    private List<SituationMention> makeSitutationMentions(SrlGraph srl, AnnoSentence from, Tokenization useUUID, EntityMentionSet addEntityMentionsTo) {
        List<SituationMention> mentions = new ArrayList<SituationMention>();
        for(SrlPred p : srl.getPreds()) {
            SituationMention sm = new SituationMention();
            sm.setText(from.getWord(p.getPosition()));
            sm.setArgumentList(new ArrayList<MentionArgument>());
            for(SrlEdge child : p.getEdges()) {
                int ai = child.getArg().getPosition();
                MentionArgument a = new MentionArgument();
                a.setRole(child.getLabel());
                
                // make an EntityMention
                EntityMention em = new EntityMention();
                em.setUuid(uuidFactory.getConcreteUUID());
                em.setEntityType("UNKNOWN");
                em.setPhraseType("OTHER");
                em.setText(from.getWord(ai));
                TokenRefSequence seq = new TokenRefSequence();
                em.setTokens(seq);
                seq.setAnchorTokenIndex(ai);
                seq.setTokenIndexList(Arrays.asList(ai));
                seq.setTokenizationId(useUUID.getUuid());
                
                a.setEntityMentionId(em.getUuid());
                addEntityMentionsTo.addToMentionList(em);
            }
        }
        return mentions;
    }

    private void addNerMentionsAndRelations(AnnoSentenceCollection sents, Communication comm) {
        //if (!sents.someHaveAt(AT.REL_LABELS)) { return; } 
        // TODO Auto-generated method stub
        //throw new RuntimeException();
    }
    
    private List<Tokenization> getTokenizationsCorrespondingTo(AnnoSentenceCollection sentences, Communication from) {
        List<Tokenization> ts = new ArrayList<Tokenization>();
        for(Section s : from.getSectionList()) {
            for(Sentence sent : s.getSentenceList()) {
                ts.add(sent.getTokenization());
            }
        }
        // make sure that the sentences line up
        if(ts.size() != sentences.size()) {
            throw new RuntimeException("Number of sentences don't match");
        }
        for(int i=0; i<ts.size(); i++) {
            if(ts.get(i).getTokenList().getTokenListSize() != sentences.get(i).size()) {
                throw new RuntimeException("Sentence lengths don't match");
            }
        }
        return ts;
    }

}
