package edu.jhu.nlp.data.conll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.jhu.nlp.data.simple.CloseableIterable;

/**
 * Reads a single file in CoNLL-2009 format.
 * 
 * @author mgormley
 *
 */
public class CoNLL08FileReader implements CloseableIterable<CoNLL08Sentence>, Iterator<CoNLL08Sentence> {

    private CoNLL08Sentence sentence;
    private BufferedReader reader;

    public CoNLL08FileReader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public CoNLL08FileReader(InputStream inputStream) throws UnsupportedEncodingException {
        this(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
    }
    
    public CoNLL08FileReader(BufferedReader reader) {
        this.reader = reader;
        next();
    }

    public static CoNLL08Sentence readCoNLL08Sentence(BufferedReader reader) throws IOException {
        // The current token.
        String line;
        // The tokens for one sentence.
        ArrayList<String> tokens = new ArrayList<String>();

        while ((line = reader.readLine()) != null) {
            if (line.equals("")) {
                // End of sentence marker.
                break;
            } else {
                // Regular token.
                tokens.add(line);
            }
        }
        if (tokens.size() > 0) {
            return CoNLL08Sentence.getInstanceFromTokenStrings(tokens);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNext() {        
        return sentence != null;
    }

    @Override
    public CoNLL08Sentence next() {
        try {
            CoNLL08Sentence curSent = sentence;
            sentence = readCoNLL08Sentence(reader);
            if (curSent != null) { curSent.intern(); }
            return curSent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Iterator<CoNLL08Sentence> iterator() {
        return this;
    }

    public void close() throws IOException {
        reader.close();
    }

    public List<CoNLL08Sentence> readAll() {
        ArrayList<CoNLL08Sentence> sents = new ArrayList<CoNLL08Sentence>();
        for (CoNLL08Sentence sent : this) {
            sents.add(sent);
        }
        return sents;
    }

    public List<CoNLL08Sentence> readSents(int maxSents) {
        return readSents(maxSents, Integer.MAX_VALUE);
    }
    
    public List<CoNLL08Sentence> readSents(int maxSents, int maxSentLen) {
        ArrayList<CoNLL08Sentence> sents = new ArrayList<CoNLL08Sentence>();
        for (CoNLL08Sentence sent : this) {
            if (sents.size() >= maxSents) {
                break;
            }
            if (sent.size() >= maxSentLen) {
                continue;
            }
            sents.add(sent);
        }
        return sents;
    }

}