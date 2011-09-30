package edu.jhu.hltcoe.data;


public class Word implements Label {

    private String word;
    
    public Word(String word) {
        this.word = word.intern();
    }

    public String getLabel() {
        return word;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((word == null) ? 0 : word.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Word other = (Word) obj;
        if (word == null) {
            if (other.word != null)
                return false;
        } else if (!word.equals(other.word))
            return false;
        return true;
    }
    
    @Override
    public int compareTo(Label arg0) {
        return getLabel().compareTo(arg0.getLabel());
    }

}
