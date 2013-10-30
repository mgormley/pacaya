package edu.jhu.gm.model;

public class UnsupportedFactorTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedFactorTypeException(Factor f) {
        super("Unsupported factor type: " + f.getClass());
    }

    public UnsupportedFactorTypeException(Factor f, String msg) {
        super("Unsupported factor type: " + f.getClass() + " - " + msg);
    }
    
}
