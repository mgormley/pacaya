package edu.jhu.data.concrete;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import edu.jhu.hlt.concrete.Concrete.Communication;
import edu.jhu.hlt.concrete.Concrete.DependencyParse;
import edu.jhu.hlt.concrete.Concrete.DependencyParse.Dependency;
import edu.jhu.hlt.concrete.Concrete.Section;
import edu.jhu.hlt.concrete.Concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Concrete.Sentence;
import edu.jhu.hlt.concrete.Concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.Concrete.Token;
import edu.jhu.hlt.concrete.Concrete.TokenTagging;
import edu.jhu.hlt.concrete.Concrete.TokenTagging.TaggedToken;
import edu.jhu.hlt.concrete.Concrete.Tokenization;
import edu.jhu.hlt.concrete.Concrete.Tokenization.Kind;
import edu.jhu.hlt.concrete.io.ProtocolBufferReader;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 * @author mmitchell
 */
public class ConcreteReader {

    public static class ConcreteReaderPrm {
        public int tokenizationTheory = 0;
        public int posTagTheory = SKIP;
        public int lemmaTheory = SKIP;
        public int depParseTheory = SKIP;
        
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

    public SimpleAnnoSentenceCollection getSentences(File concreteFile) throws Exception {
        ProtocolBufferReader<Communication> reader = new ProtocolBufferReader<Communication>(concreteFile.getAbsolutePath(), Communication.class);
        return getSentences(reader);        
    }

    public SimpleAnnoSentenceCollection getSentences(InputStream is) throws Exception {
        ProtocolBufferReader<Communication> reader = new ProtocolBufferReader<Communication>(is, Communication.class);
        return getSentences(reader);
    }

    public SimpleAnnoSentenceCollection getSentences(ProtocolBufferReader<Communication> reader) {
        SimpleAnnoSentenceCollection annoSents = new SimpleAnnoSentenceCollection();
        while(reader.hasNext()) {
            Communication communication = reader.next();
            // Assume first theory.
            SectionSegmentation sectionSegmentation = communication.getSectionSegmentation(0);
            for (Section section : sectionSegmentation.getSectionList()) {
                // Assume first theory.
                SentenceSegmentation sentSegmentation = section.getSentenceSegmentation(0);
                for (Sentence sentence : sentSegmentation.getSentenceList()) {                        
                    annoSents.add(getAnnoSentence(sentence));
                }
            }
        }
        return annoSents;
    }

    public SimpleAnnoSentence getAnnoSentence(Sentence sentence) {
        if (prm.tokenizationTheory >= sentence.getTokenizationCount()) {
            throw new IllegalArgumentException("Sentence does not contain Tokenization theory: "
                    + prm.tokenizationTheory);
        }
        return getAnnoSentence(sentence.getTokenization(0));
    }

    public SimpleAnnoSentence getAnnoSentence(Tokenization tokenization) {

        Kind kind = tokenization.getKind();
        if (kind != Kind.TOKEN_LIST) {
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
        if (prm.posTagTheory != SKIP) {
            if (prm.posTagTheory >= tokenization.getPosTagsCount()) {
                throw new IllegalArgumentException("Sentence does not contain POS tags theory: " + prm.posTagTheory);
            }
            List<String> posTags = getTagging(tokenization.getPosTags(prm.posTagTheory));
            as.setPosTags(posTags);
        }

        // Lemmas
        if (prm.lemmaTheory != SKIP) {
            if (prm.lemmaTheory >= tokenization.getLemmasCount()) {
                throw new IllegalArgumentException("Sentence does not contain lemma theory: " + prm.lemmaTheory);
            }
            List<String> lemmas = getTagging(tokenization.getLemmas(prm.lemmaTheory));
            as.setLemmas(lemmas);
        }

        // Dependency Parse
        if (prm.depParseTheory != SKIP) {
            if (prm.depParseTheory >= tokenization.getDependencyParseCount()) {
                throw new IllegalArgumentException("Sentence does not contain dependency parse theory: "
                        + prm.depParseTheory);
            }
            int[] parents = getParents(tokenization.getDependencyParse(prm.depParseTheory));
            as.setParents(parents);
        }
        return as;
    }

    public List<String> getTagging(TokenTagging tagging) {
        List<String> tags = new ArrayList<String>();
        for (TaggedToken tok : tagging.getTaggedTokenList()) {
            tags.add(tok.getTag());
        }
        return tags;
    }

    public int[] getParents(DependencyParse dependencyParse) {
        int[] parents = new int[dependencyParse.getDependencyCount()];
        for (int i = 0; i < dependencyParse.getDependencyCount(); i++) {
            Dependency arc = dependencyParse.getDependency(i);
            parents[arc.getDep()] = arc.getGov();
        }
        return parents;
    }

    /**
     * Reads a Protocol Buffer file containing Commmunications, converts the
     * dependency trees to Stanford objects, and prints them out in a human
     * readable form.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: java " + ConcreteReader.class + " <input file>");
            System.exit(1);
        }
        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("ERROR: File does not exist: " + inputFile);
            System.exit(1);
        }

        InputStream is = new FileInputStream(inputFile);
        if (inputFile.getName().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        Communication communication;
        while ((communication = Communication.parseDelimitedFrom(is)) != null) {
            for (SectionSegmentation sectionSegmentation : communication.getSectionSegmentationList()) {
                for (Section section : sectionSegmentation.getSectionList()) {
                    for (SentenceSegmentation sentSegmentation : section.getSentenceSegmentationList()) {
                        for (Sentence sent : sentSegmentation.getSentenceList()) {
                            for (Tokenization tokens : sent.getTokenizationList()) {

                                ConcreteReaderPrm prm = new ConcreteReaderPrm();                              
                                ConcreteReader cr = new ConcreteReader(prm);
                                SimpleAnnoSentence asb = cr.getAnnoSentence(tokens);
                                System.out.println("");
                                System.out.println("");
                            }
                        }
                    }
                }
            }
        }
        is.close();
    }

}
