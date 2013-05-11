package edu.jhu.hltcoe.data.conll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Reads a single file in CoNLL-2009 format.
 * 
 * @author mgormley
 *
 */
public class CoNLL09FileReader implements Iterable<CoNLL09Sentence>, Iterator<CoNLL09Sentence> {

    private CoNLL09Sentence sentence;
    private BufferedReader reader;

    public CoNLL09FileReader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public CoNLL09FileReader(InputStream inputStream) throws UnsupportedEncodingException {
        this(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
    }
    
    public CoNLL09FileReader(BufferedReader reader) {
        this.reader = reader;
        next();
    }

    public static CoNLL09Sentence readCoNLL09Sentence(BufferedReader reader) throws IOException {
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
            return new CoNLL09Sentence(tokens);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        return sentence != null;
    }

    @Override
    public CoNLL09Sentence next() {
        try {
            CoNLL09Sentence curSent = sentence;
            sentence = readCoNLL09Sentence(reader);
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
    public Iterator<CoNLL09Sentence> iterator() {
        return this;
    }

    public void close() throws IOException {
        reader.close();
    }

}