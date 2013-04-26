package edu.jhu.hltcoe.parse.cky;

/**
 * Container for lambda expression interfaces.
 * 
 * @author mgormley
 *
 */
public class Lambda {

    private Lambda() {
        // private constructor.
    }

    // TODO: Generalize this.
    public interface LambdaOne<T> {
        public void call(T obj);
    }
    
}
