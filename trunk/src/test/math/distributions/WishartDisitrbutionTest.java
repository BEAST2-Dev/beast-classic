package test.math.distributions;

import static org.junit.Assert.*;

import org.apache.commons.math.distribution.GammaDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.junit.Test;

import dr.math.distributions.WishartDistribution;

public class WishartDisitrbutionTest {

	@Test
	public void testWishartDistribution() {
        WishartDistribution wd = new WishartDistribution(2, new Double[]{500.0});
        // The above is just an approximation
        GammaDistribution gd = new GammaDistributionImpl(1.0 / 1000.0, 1000.0);
        double[] x = new double[]{1.0};
        
        assertEquals(-6.908755278962187, wd.logPdf(x), 1e-10);
        assertEquals(-6.915086640662835, gd.logDensity(x[0]), 1e-10);


        wd = new WishartDistribution(4, new Double[]{5.0});
        gd = new GammaDistributionImpl(2.0, 10.0);
        x = new double[]{1.0};
        assertEquals(-4.7051701859880914, wd.logPdf(x), 1e-10);

        wd = new WishartDistribution(1);
        x = new double[]{0.1};
        assertEquals(2.3025850929940455, wd.logPdf(x), 1e-10);
        x = new double[]{1.0};
        assertEquals(0.0, wd.logPdf(x), 1e-10);
        x = new double[]{10.0};
        assertEquals(-2.302585092994046, wd.logPdf(x), 1e-10);
        
        
        System.out.println("Wishart, uninformative, PDF(10.0): " + wd.logPdf(x));
        // These tests show the correspondence between a 1D Wishart and a Gamma
        //WishartDistribution.testBivariateMethod();

	}
}
