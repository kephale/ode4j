package org.ode4j.math;

/**
 * Constant (unmodifiable) interface for dQuaternion.
 *
 * @author Tilmann Zaeschke
 */
public interface DQuaternionC {

	/**
	 * @param i The row to return [0, 1, 2].
	 */
	public double get(int i);
	public double get0();
	public double get1();
	public double get2();
	public double get3();
}
