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
        if (name == null) {
            throw new IllegalStateException("Feature names must be non-null.");
        }
        this.name = name;
    }

    public Feature(String name, boolean b) {
        this.name = name;
        setBias(b);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
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