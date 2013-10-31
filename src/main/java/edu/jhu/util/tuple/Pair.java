package edu.jhu.util.tuple;

import edu.jhu.util.Utilities;

public class Pair<X,Y> {

	private X x;
	private Y y;
	
	public Pair(X x, Y y) {
		this.x = x;
		this.y = y;
	}
	
	public X get1() { 
		return x;
	}
	public Y get2() {
		return y;
	}
	
	@Override
	public boolean equals(Object o) { 
		if (o instanceof Pair<?,?>) {
			Pair<?,?> p = (Pair<?,?>)o;
			if (Utilities.safeEquals(x, p.get1()) &&
					Utilities.safeEquals(y, p.get2())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 37*result + (x == null ? 0 : x.hashCode());
		result = 37*result + (y == null ? 0 : y.hashCode());
		return result;
	}

    @Override
    public String toString() {
        return String.format("<%s, %s>", x,y);
    }
	
}
