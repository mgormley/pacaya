package edu.jhu.gm;

import java.io.Serializable;

/**
 * A feature in a factor graph model.
 * 
 * @author mgormley
 *
 */
public class Feature implements Serializable {

    private static final long serialVersionUID = -5575331602054552730L;
    private String name;
    private boolean isBias = false;
    
    public Feature(String name) {
        this.name = name;
    }

    public Feature(String name, boolean b) {
        this.name = name;
        setBias(b);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Feature other = (Feature) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public boolean isBiasFeature() {
        return isBias;
    }
    
    public void setBias(boolean bias) {
        isBias = bias;
    }
            
}