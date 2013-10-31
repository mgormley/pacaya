package edu.jhu.util.tuple;

public class Quadruple<X,Y,Z,A> {

	private X x;
	private Y y;
	private Z z;
	private A a;
	
	public Quadruple(X x, Y y, Z z, A a) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
	}
	
	public X get1() { 
		return x;
	}
	public Y get2() {
		return y;
	}
	
	public Z get3() {
		return z;
	}
	
	public A get4() {
        return a;
    }
	
	@Override
	public String toString() {
	    return String.format("<%s, %s, %s, %s>", x,y,z,a);
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((x == null) ? 0 : x.hashCode());
        result = prime * result + ((y == null) ? 0 : y.hashCode());
        result = prime * result + ((z == null) ? 0 : z.hashCode());
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
        @SuppressWarnings("rawtypes")
        Quadruple other = (Quadruple) obj;
        if (a == null) {
            if (other.a != null)
                return false;
        } else if (!a.equals(other.a))
            return false;
        if (x == null) {
            if (other.x != null)
                return false;
        } else if (!x.equals(other.x))
            return false;
        if (y == null) {
            if (other.y != null)
                return false;
        } else if (!y.equals(other.y))
            return false;
        if (z == null) {
            if (other.z != null)
                return false;
        } else if (!z.equals(other.z))
            return false;
        return true;
    }
	
	
}
