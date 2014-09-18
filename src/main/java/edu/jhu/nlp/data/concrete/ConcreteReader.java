package edu.jhu.nlp.data.concrete;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Constituent;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Parse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.SituationMention;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TaggedToken;
import edu.jhu.hlt.concrete.Token;
import edu.jhu.hlt.concrete.TokenList;
import edu.jhu.hlt.concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.TokenTagging;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.TokenizationKind;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.LambdaOne;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 */
public class ConcreteReader {

    private static final Logger log = Logger.getLogger(ConcreteReader.class);

    public static class ConcreteReaderPrm {
        public int tokenizationTheory = 0;
        public int posTagTheory = 0;
        public int lemmaTheory = 0;
        public int depParseTheory = 0;
        
        public ConcreteReaderPrm() { }
        
        public void setAll(int theory) {
            tokenizationTheory = theory;
            posTagTheory = theory;
            lemmaTheory = theory;
            depParseTheory = theory;
        }
    }

    private static final int SKIP = -1;

    private ConcreteReaderPrm prm;
    private int numEntityMentions = 0;
    private int numSituationMentions = 0;
    
    public ConcreteReader(ConcreteReaderPrm prm) {
        this.prm = prm;
    }

    public AnnoSentenceCollection toSentences(File inFile) throws IOException {
    	AnnoSentenceCollection sents;
        if (inFile.getName().endsWith(".zip")) {
            sents = sentsFromZipFile(inFile);
        } else {
            sents = sentsFromCommFile(inFile);
        }
        log.debug("Num entity mentions: " + numEntityMentions);
        log.debug("Num situation mentions: " + numSituationMentions);
        return sents;
    }
    
    public AnnoSentenceCollection sentsFromZipFile(File zipFile) throws IOException {
        try {
            AnnoSentenceCollection annoSents = new AnnoSentenceCollection();
            try (ZipFile zf = new ZipFile(zipFile)) {
                Enumeration<? extends ZipEntry> e = zf.entries();
                while (e.hasMoreElements()) {
                    ZipEntry ze = e.nextElement();
                    log.trace("Reading communication: " + ze.getName());
                    byte[] bytez = toBytes(zf.getInputStream(ze));
                    TDeserializer deser = new TDeserializer(new TBinaryProtocol.Factory());
                    Communication comm = new Communication();
                    deser.deserialize(comm, bytez);
                    addSentences(comm, annoSents);
                }
            }
            return annoSents;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Adapted from ThriftIO.
    private static byte[] toBytes(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
    
    public AnnoSentenceCollection sentsFromCommFile(File concreteFile) throws IOException {
        try {
            byte[] bytez = Files.readAllBytes(Paths.get(concreteFile.getAbsolutePath()));
            TDeserializer deser = new TDeserializer(new TBinaryProtocol.Factory());        
            Communication communication = new Communication();            
            deser.deserialize(communication, bytez);
            AnnoSentenceCollection sents = toSentences(communication);
            return sents;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public AnnoSentenceCollection toSentences(Communication comm) {
        AnnoSentenceCollection annoSents = new AnnoSentenceCollection();
        addSentences(comm, annoSents);
        annoSents.setSourceSents(comm);
        return annoSents;
    }

    /**
     * Converts each sentence in communication to a {@link AnnoSentence}
     * and adds it to annoSents.
     */
    public void addSentences(Communication comm, AnnoSentenceCollection aSents) {
        List<AnnoSentence> tmpSents = new ArrayList<>();
        
        // Assume first theory.
        assert comm.getSectionSegmentationsSize() == 1;
        SectionSegmentation cSs = comm.getSectionSegmentations().get(0);
        for (Section cSection : cSs.getSectionList()) {
            // Assume first theory.
            assert cSection.getSentenceSegmentationSize() == 1;
            SentenceSegmentation sentSegmentation = cSection.getSentenceSegmentation().get(0);
            for (Sentence cSent : sentSegmentation.getSentenceList()) { 
                // Assume first theory.
                assert cSent.getTokenizationListSize() == 1;
                Tokenization cToks = cSent.getTokenizationList().get(0);
                tmpSents.add(getAnnoSentence(cToks));
            }
        }

        if (comm.getEntityMentionSetsSize() > 0) {
            addEntityMentions(comm, tmpSents);

            if (comm.getSituationMentionSetsSize() > 0) {
                addSituationMentions(comm, tmpSents);
                
                for (AnnoSentence aSent : tmpSents) {                     
                    // Add the named entity pairs.
                    RelationsEncoder.addNePairsAndRelLabels(aSent);
                }                    
            }            
        }
        
        aSents.addAll(tmpSents);
    }

    private void addEntityMentions(Communication comm, List<AnnoSentence> tmpSents) {
        if (comm.getEntityMentionSetsSize() == 0) {
            return;
        }
        
        List<List<NerMention>> mentions = new ArrayList<>();
        for (int i=0; i<tmpSents.size(); i++) {
            mentions.add(new ArrayList<NerMention>());
        }
        
        Map<String, Integer> toksUuid2SentIdx = generateTokUuid2SentIdxMap(comm);
        
        assert comm.getEntityMentionSetsSize() == 1;
        EntityMentionSet cEms = comm.getEntityMentionSets().get(0);
        for (EntityMention cEm : cEms.getMentionSet()) {
            TokenRefSequence cEmToks = cEm.getTokens();
            Span span = getSpan(cEmToks);

            int sentIdx = toksUuid2SentIdx.get(cEmToks.getTokenizationId().getUuidString());
            String entityType = cEm.getEntityType();
            String entitySubtype = null;
            if (entityType.contains(":")) {
                String[] splits = entityType.split(":");
                entityType = splits[0];
                entitySubtype = splits[1];
            }
            NerMention aEm = new NerMention(
                    span, 
                    entityType,
                    entitySubtype,
                    cEm.getPhraseType(),
                    cEmToks.getAnchorTokenIndex(),
                    cEm.getUuid().getUuidString());
            mentions.get(sentIdx).add(aEm);
        }
        
        for (int i=0; i<tmpSents.size(); i++) {
            AnnoSentence aSent = tmpSents.get(i);
            NerMentions ner = new NerMentions(aSent.size(), mentions.get(i));
            aSent.setNamedEntities(ner);
        }
        
        numEntityMentions += cEms.getMentionSet().size();
    }

    private Span getSpan(TokenRefSequence toks) {
        int start = Collections.min(toks.getTokenIndexList());
        int end = Collections.max(toks.getTokenIndexList()) + 1;
        Span span = new Span(start, end);
        return span;
    }

    private void addSituationMentions(Communication comm, List<AnnoSentence> tmpSents) {

        if (comm.getSituationMentionSetsSize() == 0) {
            return;
        }
        
        for (int i=0; i<tmpSents.size(); i++) {
            tmpSents.get(i).setRelations(new RelationMentions());
        }
        
        Map<String, NerMention> emId2em = getUuid2ArgsMap(tmpSents);
        Map<String, Integer> emId2SentIdx = getUuid2SentIdxMap(tmpSents);
                
        assert comm.getSituationMentionSetsSize() == 1;
        SituationMentionSet cSms = comm.getSituationMentionSets().get(0);
        for (SituationMention cSm : cSms.getMentionList()) {
            if (!"STATE".equals(cSm.getSituationType())) {
                throw new IllegalStateException("Expecting situations of type STATE. " + cSm.getSituationType());
            }
            
            // Type / subtype.
            String stateType = cSm.getStateType();
            String stateSubtype = null;
            if (stateType.contains(":")) {
                String[] splits = stateType.split(":");
                stateType = splits[0];
                stateSubtype = splits[1];
            }
            
            // Arguments and sentence index.
            List<Pair<String,NerMention>> aArgs = new ArrayList<>();
            int sentIdx = -1;
            for (MentionArgument cArg : cSm.getArgumentList()) {
                String role = cArg.getRole();
                UUID cEmId = cArg.getEntityMentionId();
                NerMention aEm = emId2em.get(cEmId.getUuidString());
                aArgs.add(new Pair<String,NerMention>(role, aEm));
                
                int idx = emId2SentIdx.get(cEmId.getUuidString());                
                if (sentIdx != -1 && sentIdx != idx) {
                    throw new IllegalStateException("Multiple sentence indices for arguments: " + sentIdx + " " + idx);
                }
                sentIdx = idx;
            }
            
            // Situation's trigger extent.
            Span trigger = null;
            if (cSm.getTokens() != null) {
                trigger = getSpan(cSm.getTokens());
            }
            
            RelationMention aSm = new RelationMention(stateType, stateSubtype, aArgs, trigger);
            AnnoSentence aSent = tmpSents.get(sentIdx);
            RelationMentions aRels = aSent.getRelations();
            aRels.add(aSm);
            aSent.setRelations(aRels);
        }        
        numSituationMentions += cSms.getMentionList().size();
    }

    /** Get a map from UUIDs to our entity mentions. */
    private Map<String, NerMention> getUuid2ArgsMap(List<AnnoSentence> tmpSents) {
        Map<String, NerMention> emId2em = new HashMap<>();
        for (int i=0; i<tmpSents.size(); i++) {
            for (NerMention aEm : tmpSents.get(i).getNamedEntities()) {
                emId2em.put(aEm.getId(), aEm);
            }
        }
        return emId2em;
    }

    /** Gets a map from UUIDs to our sentence indices. */
    private Map<String, Integer> getUuid2SentIdxMap(List<AnnoSentence> tmpSents) {
        Map<String, Integer> emId2SentIdx = new HashMap<>();
        for (int i=0; i<tmpSents.size(); i++) {
            for (NerMention aEm : tmpSents.get(i).getNamedEntities()) {
                emId2SentIdx.put(aEm.getId(), i);
            }
        }
        return emId2SentIdx;
    }

    private Map<String, Integer> generateTokUuid2SentIdxMap(Communication comm) {
        Map<String,Integer> toksUuid2SentIdx = new HashMap<>();
        int i=0;
        SectionSegmentation cSs = comm.getSectionSegmentations().get(0);
        for (Section cSection : cSs.getSectionList()) {
            SentenceSegmentation sentSegmentation = cSection.getSentenceSegmentation().get(0);
            for (Sentence cSent : sentSegmentation.getSentenceList()) {
                Tokenization cToks = cSent.getTokenizationList().get(0);
                toksUuid2SentIdx.put(cToks.getUuid().getUuidString(), i++);
            }
        }
        return toksUuid2SentIdx;
    }

    private AnnoSentence getAnnoSentence(Tokenization tokenization) {
        TokenizationKind kind = tokenization.getKind();
        if (kind != TokenizationKind.TOKEN_LIST) {
            throw new IllegalArgumentException("tokens must be of kind TOKEN_LIST: " + kind);
        }

        AnnoSentence as = new AnnoSentence();

        // Words
        List<String> words = new ArrayList<String>();
        TokenList tl = tokenization.getTokenList();
        for (Token tok : tl.getTokens()) {
            words.add(tok.getText());
        }
        as.setWords(words);

        // POS Tags
        if (tokenization.isSetPosTagList() && prm.posTagTheory != SKIP) {
            List<String> posTags = getTagging(tokenization.getPosTagList());
            as.setPosTags(posTags);
        }

        // Lemmas
        if (tokenization.isSetLemmaList() && prm.lemmaTheory != SKIP) {
            List<String> lemmas = getTagging(tokenization.getLemmaList());
            as.setLemmas(lemmas);
        }

        // Dependency Parse
        if (tokenization.isSetDependencyParseList() && prm.depParseTheory != SKIP) {
            if (prm.depParseTheory >= tokenization.getDependencyParseListSize()) {
                throw new IllegalArgumentException("Sentence does not contain dependency parse theory: "
                        + prm.depParseTheory);
            }
            int numWords = words.size();
            int[] parents = getParents(tokenization.getDependencyParseList().get(prm.depParseTheory), numWords);
            as.setParents(parents);
        }
        
        // Constituency Parse
        if (tokenization.isSetParse()) {
            NaryTree tree = getParse(tokenization.getParse());
            as.setNaryTree(tree);
        }
        
        // TODO: Semantic Role Labeling Graph
        
        return as;
    }

    private NaryTree getParse(Parse parse) {
        IntIntHashMap id2idx = new IntIntHashMap();
        
        List<Constituent> cs = parse.getConstituentList();
        // Create the node for each constituent.
        NaryTree[] trees = new NaryTree[cs.size()];
        for (int i=0; i<cs.size(); i++) {
            Constituent c = cs.get(i);
            id2idx.put(c.getId(), i);
            Span span = new Span(NaryTree.NOT_INITIALIZED, NaryTree.NOT_INITIALIZED);
            if (c.isSetTokenSequence()) {
                span = getSpan(c.getTokenSequence());
            }
            boolean isLexical = (c.getChildList().size() == 0);
            trees[i] = new NaryTree(c.getTag(), span.start(), span.end(), new ArrayList<NaryTree>(), isLexical);
        }
        
        // Add the children for each node.
        for (int i=0; i<cs.size(); i++) {
            Constituent c = cs.get(i);
            for (int id : c.getChildList()) {
                int j = id2idx.get(id);
                trees[i].addChild(trees[j]);
            }
        }
        
        // Find the root.
        NaryTree root = trees[0];
        while (root.getParent() != null) {
            root = root.getParent();
        }
        
        final MutableInt numNodes = new MutableInt(0);
        root.preOrderTraversal(new LambdaOne<NaryTree>() {
            @Override
            public void call(NaryTree obj) {
                numNodes.v++;
            }
        });
        
        if (numNodes.v != cs.size()) {
            log.warn(String.format("Not all constituents were included in the tree: expected=%d actual=%d", cs.size(), numNodes.v));
        }
        
        return root;
    }

    private List<String> getTagging(TokenTagging tagging) {
        List<String> tags = new ArrayList<String>();
        for (TaggedToken tok : tagging.getTaggedTokenList()) {
            tags.add(tok.getTag());
        }
        return tags;
    }

    private int[] getParents(DependencyParse dependencyParse, int numWords) {
        int[] parents = new int[numWords];
        Arrays.fill(parents, -2);
        for (Dependency arc : dependencyParse.getDependencyList()) {
            if (parents[arc.getDep()] != -2) {
                throw new IllegalStateException("Multiple parents for token: " + dependencyParse);
            }
            if (!arc.isSetGov()) {
                parents[arc.getDep()] = -1;
            } else {
                parents[arc.getDep()] = arc.getGov();
            }
        }
        return parents;
    }

    /**
     * Reads a file containing a single Commmunication concrete object as bytes,
     * converts it to a AnnoSentence and prints it out in human readable
     * form.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java " + ConcreteReader.class + " <input file>");
            System.exit(1);
        }
        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("ERROR: File does not exist: " + inputFile);
            System.exit(1);
        }

        System.out.println("Reading file: " + inputFile);
        ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());
        for (AnnoSentence sent : reader.toSentences(inputFile)) {
            System.out.println(sent);
        }        
    }

}
