package edu.jhu.data;


public class Word implements Label {

    private static final long serialVersionUID = 6261153458236675157L;
    private String word;
    
    public Word(String word) {
        if (word == null) {
            throw new IllegalArgumentException("word is null");
        }
        this.word = word.intern();
    }

    public String getWord() {
        return word;
    }
    
    public String getLabel() {
        return word;
    }

    @Override
    public int hashCode() {
        return word.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Word) {
            Word other = (Word) obj;
            return word.equals(other.word);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Label label) {
        if (label instanceof Word) {
            return getLabel().compareTo(label.getLabel());
        } else {
            return -1;
        }
    }
    
    @Override
    public String toString() {
        return word;
    }

}
