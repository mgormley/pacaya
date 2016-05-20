package edu.jhu.pacaya.gm.model;

import java.util.Arrays;

import edu.jhu.prim.iter.IntIter;

  /** Ported by hand from libDAI in C++.
   * Originally released under the BSD license.
   * 
   *  Tool for looping over the states of several variables.
   *  
   *  The class IndexFor is an important tool for indexing Factor entries.
   *  Its usage can best be explained by an example.
   *  Assume \a indexVars, \a forVars are both VarSet 's.
   *  Then the following code:
   *  \code
   *      IndexFor i( indexVars, forVars );
   *      size_t iter = 0;
   *      for( ; i.valid(); i++, iter++ ) {
   *          cout << "State of forVars: " << calcState( forVars, iter ) << "; ";
   *          cout << "state of indexVars: " << calcState( indexVars, size_t(i) ) << endl;
   *      }
   *  \endcode
   *  loops over all joint states of the variables in \a forVars,
   *  and <tt>(size_t)i</tt> equals the linear index of the corresponding
   *  state of \a indexVars, where the variables in \a indexVars that are
   *  not in \a forVars assume their zero'th value.
   *  \idea Optimize all indices as follows: keep a cache of all (or only
   *  relatively small) indices that have been computed (use a hash). Then,
   *  instead of computing on the fly, use the precomputed ones. Here the
   *  labels of the variables don't matter, but the ranges of the variables do.
   */
public class IndexFor implements IntIter {

    /** The current linear index corresponding to the state of indexVars. */
    private int _index;

    /** For each variable in forVars, the amount of change in _index. */
    private long[] _sum;

    /** For each variable in forVars, the current state. */
    private int[] _state;

    /** For each variable in forVars, its number of possible values. */
    private int[] _ranges;

    
    /** Construct IndexFor object from \a indexVars and \a forVars. */            
    public IndexFor(final VarSet indexVars, final VarSet forVars ) { 
        long sum = 1;
        _state = new int[forVars.size()];
        _ranges = new int[forVars.size()];
        _sum = new long[forVars.size()];

        for (int ii = 0; ii < indexVars.size(); ii++) {
            Var i = indexVars.get(ii);
            sum *= i.getNumStates();
        }
        int cur = 0;
        int jj = 0;
        for (int ii = 0; ii < indexVars.size(); ii++) {
            Var i = indexVars.get(ii);
            sum /= i.getNumStates();
            for ( ; jj < forVars.size() && forVars.get(jj).compareTo(i) <= 0; jj++) {
                Var j = forVars.get(jj);
                _ranges[cur] = j.getNumStates();
                _sum[cur] = (i.equals(j)) ? sum : 0;
                cur++;
            }
        }
        for ( ; jj < forVars.size(); jj++) {
            Var j = forVars.get(jj);
            _ranges[cur] = j.getNumStates();
            _sum[cur] = 0l;
            cur++;
        }
        _index = 0;
        
        assert (cur == _state.length);
    }

    /// Resets the state
    public void reset() {
        Arrays.fill( _state, 0 );
        _index = 0;
    }

    /** Increments the current state of \a forVars (prefix) and returns linear index of the current state of indexVars. */
    public int next() {
        int curIndex = _index;
        // Compute the next index.
        if( _index >= 0 ) {
            int i = _state.length - 1;

            while( i >= 0 ) {
                _index += _sum[i];
                if( ++_state[i] < _ranges[i] )
                    break;
                _index -= _sum[i] * _ranges[i];
                _state[i] = 0;
                i--;
            }

            if( i == -1 )
                _index = -1;
        }
        // Return the current index.
        return curIndex;
    }

    /// Returns \c true if the current state is valid
    public boolean hasNext() {
        return( _index >= 0 );
    }
    
    /**
     * For each variable in forVars, the current state.
     */
    public int[] getState() {
        return Arrays.copyOf(_state, _state.length);
    }
    
}
