package edu.jhu.data.concrete;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.SectionKind;
import edu.jhu.hlt.concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.TaggedToken;
import edu.jhu.hlt.concrete.Token;
import edu.jhu.hlt.concrete.TokenTagging;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.TokenizationKind;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 */
public class ConcreteReader {

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

    public ConcreteReader(ConcreteReaderPrm prm) {
        this.prm = prm;
    }

    public SimpleAnnoSentenceCollection toSentences(File concreteFile) throws IOException {
        try {
            byte[] bytez = Files.readAllBytes(Paths.get(concreteFile.getAbsolutePath()));
            TDeserializer deser = new TDeserializer(new TBinaryProtocol.Factory());        
            Communication communication = new Communication();            
            deser.deserialize(communication, bytez);
            SimpleAnnoSentenceCollection sents = toSentences(communication);
            sents.setSourceSents(communication);
            return sents;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public SimpleAnnoSentenceCollection toSentences(Communication communication) {
        SimpleAnnoSentenceCollection annoSents = new SimpleAnnoSentenceCollection();
        addSentences(communication, annoSents);
        return annoSents;
    }

    /**
     * Converts each sentence in communication to a {@link SimpleAnnoSentence}
     * and adds it to annoSents.
     */
    public void addSentences(Communication communication, SimpleAnnoSentenceCollection annoSents) {
        // Assume first theory.
        assert communication.getSectionSegmentationsSize() == 1;
        SectionSegmentation sectionSegmentation = communication.getSectionSegmentations().get(0);
        for (Section section : sectionSegmentation.getSectionList()) {
            if (shouldSkipSection(section)) { continue; } 
            // Assume first theory.
            assert section.getSentenceSegmentationSize() == 1;
            SentenceSegmentation sentSegmentation = section.getSentenceSegmentation().get(0);
            for (Sentence sentence : sentSegmentation.getSentenceList()) {                        
                annoSents.add(getAnnoSentence(sentence));
            }
        }
    }

    private SimpleAnnoSentence getAnnoSentence(Sentence sentence) {
        if (prm.tokenizationTheory >= sentence.getTokenizationListSize()) {
            throw new IllegalArgumentException("Sentence does not contain Tokenization theory: "
                    + prm.tokenizationTheory);
        }
        return getAnnoSentence(sentence.getTokenizationList().get(0));
    }

    private SimpleAnnoSentence getAnnoSentence(Tokenization tokenization) {

        TokenizationKind kind = tokenization.getKind();
        if (kind != TokenizationKind.TOKEN_LIST) {
            throw new IllegalArgumentException("tokens must be of kind TOKEN_LIST: " + kind);
        }

        SimpleAnnoSentence as = new SimpleAnnoSentence();

        // Words
        List<String> words = new ArrayList<String>();
        for (Token tok : tokenization.getTokenList()) {
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
        
        // TODO: Semantic Role Labeling Graph        
        
        return as;
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
     * converts it to a SimpleAnnoSentence and prints it out in human readable
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

        ConcreteReader reader = new ConcreteReader(new ConcreteReaderPrm());
        for (SimpleAnnoSentence sent : reader.toSentences(inputFile)) {
            System.out.println(sent);
        }        
    }

    /** Returns whether to skip this section. */
    public static boolean shouldSkipSection(Section section) {
        return section.getKind() != SectionKind.PASSAGE;
    }

}


// TODO: Switch to this code for reading TokenTagging theories if the thrift schema changes.
// 
//        // POS Tags
//        if (tokenization.isSetPosTagList() && prm.posTagTheory != SKIP) {
//            if (prm.posTagTheory >= tokenization.getPosTagListSize()) {
//                throw new IllegalArgumentException("Sentence does not contain POS tags theory: " + prm.posTagTheory);
//            }
//            List<String> posTags = getTagging(tokenization.getPosTags(prm.posTagTheory));
//            as.setPosTags(posTags);
//        }
//        
//        // Lemmas
//        if (tokenization.isSetLemmaList() && prm.lemmaTheory != SKIP) {
//            if (prm.lemmaTheory >= tokenization.getLemmasListSize()) {
//                throw new IllegalArgumentException("Sentence does not contain lemma theory: " + prm.lemmaTheory);
//            }
//            List<String> lemmas = getTagging(tokenization.getLemmas(prm.lemmaTheory));
//            as.setLemmas(lemmas);
//        }
