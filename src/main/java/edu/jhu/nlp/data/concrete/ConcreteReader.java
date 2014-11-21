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

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Constituent;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Parse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.Sentence;
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
import edu.jhu.hlt.concrete.serialization.ThreadSafeCompactCommunicationSerializer;
import edu.jhu.hlt.concrete.util.ConcreteException;
import edu.jhu.hlt.concrete.util.TokenizationUtils.TagTypes;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.FnO1ToVoid;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 */
public class ConcreteReader {

    private static final Logger log = Logger.getLogger(ConcreteReader.class);

    private ThreadSafeCompactCommunicationSerializer ser = new ThreadSafeCompactCommunicationSerializer();
    private int numEntityMentions = 0;
    private int numOverlapingMentions = 0;
    private int numSituationMentions = 0;

    public ConcreteReader() { }

    public AnnoSentenceCollection toSentences(File inFile) throws IOException {
    	AnnoSentenceCollection sents;
        if (inFile.getName().endsWith(".zip")) {
            sents = sentsFromZipFile(inFile);
        } else {
            sents = sentsFromCommFile(inFile);
        }
        log.debug("Num entity mentions: " + numEntityMentions);
        log.debug("Num overlapping entity mentions: " + numOverlapingMentions);        
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
                    Communication comm = ser.fromBytes(bytez);
                    addSentences(comm, annoSents);
                }
            }
            return annoSents;
        } catch (ConcreteException e) {
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
            Communication communication = ser.fromBytes(bytez);
            AnnoSentenceCollection sents = toSentences(communication);
            return sents;
        } catch (ConcreteException e) {
            throw new RuntimeException(e);
        }
    }

    public AnnoSentenceCollection toSentences(Communication comm) {
        AnnoSentenceCollection annoSents = new AnnoSentenceCollection();
        addSentences(comm, annoSents);
        return annoSents;
    }

    /**
     * Converts each sentence in communication to a {@link AnnoSentence}
     * and adds it to annoSents.
     */
    protected void addSentences(Communication comm, AnnoSentenceCollection aSents) {
        List<AnnoSentence> tmpSents = new ArrayList<>();
        
        for (Section cSection : comm.getSectionList()) {
            for (Sentence cSent : cSection.getSentenceList()) { 
                Tokenization cToks = cSent.getTokenization();
                tmpSents.add(getAnnoSentence(cToks));
            }
        }

        if (comm.getEntityMentionSetListSize() > 0) {
            addEntityMentions(comm, tmpSents);

            if (comm.getSituationMentionSetListSize() > 0) {
                addSituationMentions(comm, tmpSents);
            }
        }
        
        aSents.addAll(tmpSents);
        // Update source sentences.
        if (aSents.getSourceSents() == null) {
            aSents.setSourceSents(new ArrayList<Communication>());
        }
        log.debug("Adding Communication in sourceSents");
        ((ArrayList<Communication>)aSents.getSourceSents()).add(comm);
    }

    private void addEntityMentions(Communication comm, List<AnnoSentence> tmpSents) {
        if (comm.getEntityMentionSetListSize() == 0) {
            return;
        }
        
        List<List<NerMention>> mentions = new ArrayList<>();
        for (int i=0; i<tmpSents.size(); i++) {
            mentions.add(new ArrayList<NerMention>());
        }
        
        Map<String, Integer> toksUuid2SentIdx = generateTokUuid2SentIdxMap(comm);
        
        assert comm.getEntityMentionSetListSize() == 1;
        EntityMentionSet cEms = comm.getEntityMentionSetList().get(0);
        for (EntityMention cEm : cEms.getMentionList()) {
            TokenRefSequence cEmToks = cEm.getTokens();
            Span span = getSpan(cEmToks);

            int sentIdx = toksUuid2SentIdx.get(cEmToks.getTokenizationId().getUuidString());
            String entityType = cEm.getEntityType();
            String entitySubtype = null;
            if (entityType != null && entityType.contains(":")) {
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
            numOverlapingMentions += ner.getNumOverlapping();
            aSent.setNamedEntities(ner);
        }
        
        numEntityMentions += cEms.getMentionList().size();
    }

    private void addSituationMentions(Communication comm, List<AnnoSentence> tmpSents) {

        if (comm.getSituationMentionSetListSize() == 0) {
            return;
        }
        
        for (int i=0; i<tmpSents.size(); i++) {
            tmpSents.get(i).setRelations(new RelationMentions());
        }
        
        Map<String, NerMention> emId2em = getUuid2ArgsMap(tmpSents);
        Map<String, Integer> emId2SentIdx = getUuid2SentIdxMap(tmpSents);
                
        assert comm.getSituationMentionSetListSize() == 1;
        SituationMentionSet cSms = comm.getSituationMentionSetList().get(0);
        for (SituationMention cSm : cSms.getMentionList()) {
            if (!"STATE".equals(cSm.getSituationType())) {
                throw new IllegalStateException("Expecting situations of type STATE. " + cSm.getSituationType());
            }
            
            // Type / subtype.
            String type = cSm.getSituationKind();
            String subtype = null;
            if (type.contains(":")) {
                String[] splits = type.split(":");
                type = splits[0];
                subtype = splits[1];
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
            
            RelationMention aSm = new RelationMention(type, subtype, aArgs, trigger);
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
        for (Section cSection : comm.getSectionList()) {
            for (Sentence cSent : cSection.getSentenceList()) {
                Tokenization cToks = cSent.getTokenization();
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
        for (Token tok : tl.getTokenList()) {
            words.add(tok.getText());
        }
        as.setWords(words);

        TokenTagging posTags = TokenizationUtils.getFirstXTags(tokenization, TagTypes.POS.name());
        TokenTagging lemmas = TokenizationUtils.getFirstXTags(tokenization, TagTypes.LEMMA.name());
        TokenTagging chunks = TokenizationUtils.getFirstXTags(tokenization, "CHUNK");
        as.setPosTags(getTagging(posTags));
        as.setLemmas(getTagging(lemmas));
        as.setChunks(getTagging(chunks));
        
        // Dependency Parse
        if (tokenization.isSetDependencyParseList()) {
            int numWords = words.size();
            DependencyParse depParse = TokenizationUtils.getFirstDependencyParseWithName(tokenization, "basic-deps");
            int[] parents = getParents(depParse, numWords);
            as.setParents(parents);
        }
        
        // Constituency Parse
        if (tokenization.isSetParseList()) {
            NaryTree tree = getParse(TokenizationUtils.getFirstParse(tokenization));
            as.setNaryTree(tree);
        }
        
        // TODO: Semantic Role Labeling Graph
        
        return as;
    }

    private static NaryTree getParse(Parse parse) {
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
        root.preOrderTraversal(new FnO1ToVoid<NaryTree>() {
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

    private static List<String> getTagging(TokenTagging tagging) {
        if (tagging == null) {
            return null;
        }
        List<String> tags = new ArrayList<String>();
        for (TaggedToken tok : tagging.getTaggedTokenList()) {
            tags.add(tok.getTag());
        }
        return tags;
    }

    private static int[] getParents(DependencyParse dependencyParse, int numWords) {
        if (dependencyParse == null) {
            return null;
        }
        int[] parents = new int[numWords];
        Arrays.fill(parents, -2);
        for (Dependency arc : dependencyParse.getDependencyList()) {
            int c = arc.getDep();
            if (c < 0) {
                throw new IllegalStateException(String.format("Invalid dep value %d for dependendency tree %s", arc.getDep(), dependencyParse.getUuid()));
            }
            if (parents[c] != -2) {
                throw new IllegalStateException("Multiple parents for token: " + dependencyParse);
            }
            if (!arc.isSetGov()) {
                parents[c] = -1;
            } else {
                parents[c] = arc.getGov();
            }
        }
        if (IntArrays.contains(parents, -2)) {
            log.warn("Dependency tree contains token(s) with no head: " + dependencyParse.getUuid());
        }
        return parents;
    }

    private static Span getSpan(TokenRefSequence toks) {
        int start = Collections.min(toks.getTokenIndexList());
        int end = Collections.max(toks.getTokenIndexList()) + 1;
        Span span = new Span(start, end);
        return span;
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
        ConcreteReader reader = new ConcreteReader();
        for (AnnoSentence sent : reader.toSentences(inputFile)) {
            System.out.println(sent);
        }        
    }

}
