package test.beast.evolution.substitutionmodel;

import java.util.Arrays;

import org.junit.Test;

import beast.base.evolution.substitutionmodel.DefaultEigenSystem;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import beast.base.evolution.substitutionmodel.EigenSystem;
import beast.evolution.substitutionmodel.RobustEigenSystem;
import beast.base.util.Randomizer;
import junit.framework.TestCase;

public class RobustEigenSystemTest extends TestCase {
	
	@Test
	public void testRobustEigenSystem() {
		Randomizer.setSeed(123);

		doTest(4);
		doTest(5);
		doTest(10);
		doTest(16);
		doTest(24);
	}	
	
	public void doTest(int n) {
		System.err.println("Testing matrices of size " + n);
		
		for (int k = 0; k < 1000; k++) {
		double [][] qMatrix = newRandomMatrix(n);
		
		EigenSystem res = new RobustEigenSystem(n);
		EigenDecomposition red = res.decomposeMatrix(qMatrix);
		EigenSystem def = new DefaultEigenSystem(n);
		EigenDecomposition ed = def.decomposeMatrix(qMatrix);
		
		double [] rev = red.getEigenValues();
		double [] dev = ed.getEigenValues();
		
		Arrays.sort(rev);
		Arrays.sort(dev);
		
		double sum = 0;
		for (int i = 0; i < n; i++) {
			sum += Math.abs(rev[i] - dev[i]);
		}
		//System.err.println("|robust eigenvalue - eigenvalue| = " + sum);
		assertEquals(sum, 0.0, 1e-11);
		}
	}

	// generate asymetric Q matrix with 50% zeros and other rates drawn from exponential distribution with mean = 1
	private double[][] newRandomMatrix(int n) {
		double [][] qMatrix = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i != j && Randomizer.nextBoolean()) {
					qMatrix[i][j] = Math.exp(Randomizer.nextDouble());
				}
			}
		}
		for (int i = 0; i < n; i++) {
			double sum = 0;
			for (int j = 0; j < n; j++) {
				sum += qMatrix[i][j];
			}
			qMatrix[i][i] = -sum;
		}
		return qMatrix;
	}
}
