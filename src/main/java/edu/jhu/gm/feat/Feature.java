package edu.jhu.gm.feat;

import java.io.Serializable;

/**
 * A feature in a factor graph model.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class Feature implements Serializable {

    private static final long serialVersionUID = -5575331602054552730L;
    private final Object name;
    private final boolean isBias;
    private final int hashCode;
    
    public Feature(Object name) {
        if (name == null) {
            throw new IllegalStateException("Feature names must be non-null.");
        }
        this.name = name;
        this.isBias = false;
        hashCode = computeHash();
    }

    public Feature(Object name, boolean isBias) {
        this.name = name;
        this.isBias = isBias;
        hashCode = computeHash();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHash() {
        return ((name == null) ? 0 : name.hashCode());
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
        if (isBias != other.isBias)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name.toString();
    }
    
    public boolean isBiasFeature() {
        return isBias;
    }
            
}