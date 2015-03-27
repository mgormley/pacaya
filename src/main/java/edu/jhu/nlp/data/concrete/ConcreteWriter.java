package edu.jhu.nlp.data.concrete;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.hlt.concrete.communications.SuperCommunication;
import edu.jhu.hlt.concrete.serialization.CompactCommunicationSerializer;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.uuid.UUIDFactory;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.prim.tuple.Pair;

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
        /**
         * Whether to write out SRL as a labeled dependency tree (i.e. syntax) or as SituationMentions.
         * 
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
        public boolean srlIsSyntax = false;
        /** Sets the include flag for each annotation type to true, or warns if it's not supported. */
        public void addAnnoTypes(Collection<AT> ats) {
            this.addDepParse = ats.contains(AT.DEP_TREE);
            this.addSrl = ats.contains(AT.SRL);
            this.addNerMentions = ats.contains(AT.NER);
            this.addRelations = ats.contains(AT.RELATIONS);
            
            EnumSet<AT> others = EnumSet.complementOf(EnumSet.of(AT.DEP_TREE, AT.SRL, AT.NER, AT.RELATIONS));
            for (AT at : ats) {
                if (others.contains(at)) {
                    log.warn("Annotations of type {} are not supported by ConcreteWriter and will not be added to Concrete Communications.", at);
                }
            }
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(ConcreteWriter.class);

    public static final String DEP_PARSE_TOOL = "Pacaya Dependency Parser";
    public static final String SRL_TOOL = "Pacaya Semantic Role Labeler (SRL)";
    private static final String REL_TOOL = "Pacaya Relation Extractor";
    private static final String NER_TOOL = "Pacaya Named Entity Recognizer (NER)";
        
    private final long timestamp;     // time that every annotation that is processed will get
    private final ConcreteWriterPrm prm;

    public ConcreteWriter(ConcreteWriterPrm prm) {
        this.timestamp = System.currentTimeMillis();
        this.prm = prm;
    }

    public void write(AnnoSentenceCollection sents, File out) throws IOException {
        List<Communication> comms = (List<Communication>) sents.getSourceSents();
        if (out.getName().endsWith(".zip")) {
            throw new RuntimeException("Zip file output not yet supported for Concrete");
        } else {
            if (comms.size() == 0) {
                throw new RuntimeException("No Communication in sourceSents field.");
            }
            if (comms.size() > 1) {
                throw new RuntimeException("Multiple Communications in input cannot be written to a single Communication as output.");
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
        int numSents = ConcreteUtils.getNumSents(comm);
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
        p.setUuid(getUUID());
        AnnotationMetadata meta = new AnnotationMetadata();
        meta.setTool(DEP_PARSE_TOOL);
        meta.setTimestamp(timestamp);
        p.setMetadata(meta);
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
            ems.setUuid(getUUID());
            ems.setMetadata(meta);
            SituationMentionSet sms = new SituationMentionSet();
            sms.setUuid(getUUID());
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
        p.setUuid(getUUID());
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
                em.setUuid(getUUID());
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
        // We require NER if we're adding it or not.
        if (!sents.someHaveAt(AT.NER)) { return; }
        // One map per sentence.
        List<Map<NerMention, EntityMention>> aem2cem = new ArrayList<>();
        if (prm.addNerMentions) {
            // 1.a. If we are adding NerMentions, convert the NerMentions to
            // EntityMentions (storing the below mapping along the
            // way).
            List<EntityMention> cEms = new ArrayList<>(); 
            List<Tokenization> ts = getTokenizationsCorrespondingTo(sents, comm);
            for(int i=0; i<sents.size(); i++) {
                Tokenization cSent = ts.get(i);
                AnnoSentence aSent = sents.get(i);
                Map<NerMention, EntityMention> a2cForSent = new HashMap<>();
                NerMentions aEms = aSent.getNamedEntities();
                if (aEms != null) {
                    for (NerMention aEm : aEms) {                    
                        TokenRefSequence cSpan = new TokenRefSequence();
                        cSpan.setTokenIndexList(toIntegerList(aEm.getSpan()));
                        cSpan.setTokenizationId(cSent.getUuid());
                        EntityMention cEm = new EntityMention();
                        cEm.setUuid(getUUID());
                        cEm.setTokens(cSpan);
                        String type = aEm.getEntityType();
                        if (aEm.getEntitySubType() != null) {
                            type += ":" + aEm.getEntitySubType();
                        }
                        cEm.setEntityType(type);
                        if (aEm.getPhraseType() != null) {
                            cEm.setPhraseType(aEm.getPhraseType());
                        }
                        a2cForSent.put(aEm, cEm);
                        cEms.add(cEm);
                    }
                }
                aem2cem.add(a2cForSent);
            }
            AnnotationMetadata cMeta = new AnnotationMetadata();
            cMeta.setTool(NER_TOOL);
            cMeta.setTimestamp(timestamp);
            EntityMentionSet cEmSet = new EntityMentionSet();
            cEmSet.setUuid(getUUID());
            cEmSet.setMetadata(cMeta);
            cEmSet.setMentionList(cEms);
        } else {
            assert comm.getEntityMentionSetListSize() == 1;
            // 1.b. Create a mapping from NerMention's to EntityMentions (these will be the existing
            // EntityMentions that we read in.)
            SuperCommunication cSupComm = new SuperCommunication(comm);
            Map<UUID, EntityMention> id2cem = cSupComm.generateEntityMentionIdToEntityMentionMap();
            for(int i=0; i<sents.size(); i++) {
                Map<NerMention, EntityMention> a2cForSent = new HashMap<>();
                AnnoSentence aSent = sents.get(i);
                for (NerMention aEm : aSent.getNamedEntities()) {
                    EntityMention cEm = id2cem.get(new UUID(aEm.getId()));
                    a2cForSent.put(aEm, cEm);
                }
                aem2cem.add(a2cForSent);
            }
        }
        
        if (prm.addRelations) {
            if (!sents.someHaveAt(AT.RELATIONS)) { return; }
            // 2. Convert AnnoSentence.getRelations() to Concrete's
            // SituationMentions using the above mapping.
            List<SituationMention> cRels = new ArrayList<>();
            for(int i=0; i<sents.size(); i++) {
                AnnoSentence s = sents.get(i);
                Map<NerMention, EntityMention> a2cForSent = aem2cem.get(i);
                RelationMentions aRels = s.getRelations();
                if (aRels != null) {
                    for (RelationMention aRel : aRels) {
                        List<MentionArgument> cArgs = new ArrayList<>();
                        for (Pair<String, NerMention> aArg : aRel.getNerOrderedArgs()) {
                            MentionArgument cArg = new MentionArgument();
                            cArg.setRole(aArg.get1());
                            cArg.setEntityMentionId(a2cForSent.get(aArg.get2()).getUuid());
                            cArgs.add(cArg);
                        }
                        SituationMention cRel = new SituationMention();
                        cRel.setUuid(getUUID());
                        cRel.setArgumentList(cArgs);
                        String relation = aRel.getType();
                        if (aRel.getSubType() != null) {
                            relation += ":" + aRel.getSubType();
                        }
                        cRel.setSituationKind(relation);
                        cRel.setSituationType("STATE");
                        cRels.add(cRel);
                    }
                }
            }
            AnnotationMetadata cMeta = new AnnotationMetadata();
            cMeta.setTool(REL_TOOL);
            cMeta.setTimestamp(timestamp);
            SituationMentionSet cRelSet = new SituationMentionSet();
            cRelSet.setUuid(getUUID());
            cRelSet.setMetadata(cMeta);
            cRelSet.setMentionList(cRels);
            comm.addToSituationMentionSetList(cRelSet);
        }
    }
    
    /** Converts a {@link Span} to a list of integers. */
    private static List<Integer> toIntegerList(Span span) {
        List<Integer> ids = new ArrayList<>();
        for (int i=span.start(); i<span.end(); i++) {
            ids.add(i);
        }
        return ids;
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

    private UUID getUUID() {
        return UUIDFactory.newUUID();
    }

}
