/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
*/
package cern.jet.random;

/**
 * Contains various mathematical helper methods.
 *
 * <b>Implementation:</b> High performance implementation.
 * <dt>This is a port of <tt>gen_fun.cpp</tt> from the <A HREF="http://www.cis.tu-graz.ac.at/stat/stadl/random.html">C-RAND / WIN-RAND</A> library.
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
class Fun {
/**
 * Makes this class non instantiable, but still let's others inherit from it.
 */
protected Fun() {
	throw new RuntimeException("Non instantiable");
}
/**
 * Returns the gamma function <tt>gamma(x)</tt>.
 */
public static double gamma(double x) {
	x = logGamma(x);
	//if (x > Math.log(Double.MAX_VALUE)) return Double.MAX_VALUE;
	return Math.exp(x);
}
/**
 * Returns a quick approximation of <tt>log(gamma(x))</tt>.
 */
public static double logGamma(double x) {
	final double c0 = 9.1893853320467274e-01, c1 = 8.3333333333333333e-02,
		c2 =-2.7777777777777777e-03, c3 = 7.9365079365079365e-04,
		c4 =-5.9523809523809524e-04, c5 = 8.4175084175084175e-04,
		c6 =-1.9175269175269175e-03;
	double g,r,z;

	if (x <= 0.0 /* || x > 1.3e19 */ ) return -999;
	
	for (z = 1.0; x < 11.0; x++) z *= x;
	
	r = 1.0 / (x * x);
	g = c1 + r * (c2 + r * (c3 + r * (c4 + r * (c5 + r + c6))));
	g = (x - 0.5) * Math.log(x) - x + c0 + g / x;
	if (z == 1.0) return g;
	return g - Math.log(z);
}
}
