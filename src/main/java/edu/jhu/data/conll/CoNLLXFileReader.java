package edu.jhu.data.conll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.data.simple.CloseableIterable;

/**
 * Reads a single file in CoNLL-X format.
 * 
 * @author mgormley
 *
 */
public class CoNLLXFileReader implements CloseableIterable<CoNLLXSentence>, Iterator<CoNLLXSentence> {

    private CoNLLXSentence sentence;
    private BufferedReader reader;

    public CoNLLXFileReader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public CoNLLXFileReader(InputStream inputStream) throws UnsupportedEncodingException {
        this(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
    }
    
    public CoNLLXFileReader(BufferedReader reader) {
        this.reader = reader;
        next();
    }

    public static CoNLLXSentence readCoNLLXSentence(BufferedReader reader) throws IOException {
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
            return new CoNLLXSentence(tokens);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        return sentence != null;
    }

    @Override
    public CoNLLXSentence next() {
        try {
            CoNLLXSentence curSent = sentence;
            sentence = readCoNLLXSentence(reader);
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
    public Iterator<CoNLLXSentence> iterator() {
        return this;
    }

    public void close() throws IOException {
        reader.close();
    }

}