package edu.jhu.data;


public class Lemma implements Label {

    private static final long serialVersionUID = -3390519842985192429L;
    private String lemma;
    
    public Lemma(String word) {
        if (word == null) {
            throw new IllegalArgumentException("lemma is null");
        }
        this.lemma = word.intern();
    }
    
    public String getLemma() {
        return lemma;
    }
    
    public String getLabel() {
        return lemma;
    }

    @Override
    public int hashCode() {
        return lemma.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Lemma) {
            Lemma other = (Lemma) obj;
            return lemma.equals(other.lemma);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Label label) {
        if (label instanceof Lemma) {
            return getLabel().compareTo(label.getLabel());
        } else {
            return -1;
        }
    }
    
    @Override
    public String toString() {
        return lemma;
    }

}
