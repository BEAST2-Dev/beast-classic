package test.math.distributions;

import static org.junit.jupiter.api.Assertions.*;



import org.junit.jupiter.api.Test;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beastclassic.math.distributions.WishartDistribution;



public class WishartDisitrbutionTest {

	private RealVectorParam<Real> realParam(String values) {
		RealVectorParam<Real> p = new RealVectorParam<>();
		p.initByName("value", values);
		return p;
	}

	private RealVectorParam<NonNegativeReal> nnrealParam(String values) {
		RealVectorParam<NonNegativeReal> p = new RealVectorParam<>();
		p.initByName("value", values);
		return p;
	}

	@Test
	public void testWishartDistribution() throws Exception {

        WishartDistribution wd = new WishartDistribution();
        wd.initByName("df", 2.0,
        			"scaleMatrix", nnrealParam("500.0"),
        			"arg", realParam("1.0"));
        // The above is just an approximation
        var gd = org.apache.commons.statistics.distribution.GammaDistribution.of(1.0 / 1000.0, 1000.0);

        assertEquals(-6.908755278962187, wd.calculateLogP(), 1e-10);
        assertEquals(-6.915086640662835, gd.logDensity(1.0), 1e-10);


        wd.initByName("df", 4.0,
    			"scaleMatrix", nnrealParam("5.0"),
    			"arg", realParam("1.0"));
        gd = org.apache.commons.statistics.distribution.GammaDistribution.of(2.0, 10.0);
        assertEquals(-4.7051701859880914, wd.calculateLogP(), 1e-10);

        wd.initByName("df", 1.0,
    			"scaleMatrix", null,
    			"arg", realParam("0.1"));
        assertEquals(2.3025850929940455, wd.calculateLogP(), 1e-10);

        wd.initByName("arg", realParam("1.0"));
        assertEquals(0.0, wd.calculateLogP(), 1e-10);

        wd.initByName("arg", realParam("10.0"));
        assertEquals(-2.302585092994046, wd.calculateLogP(), 1e-10);


        System.out.println("Wishart, uninformative, PDF(10.0): " + wd.calculateLogP());

	}


	@Test
	public void testClassicWishartDistribution() {
		beastclassic.dr.math.distributions.WishartDistribution wd = new beastclassic.dr.math.distributions.WishartDistribution(2, new Double[]{500.0});
        var gd = org.apache.commons.statistics.distribution.GammaDistribution.of(1.0 / 1000.0, 1000.0);
        double[] x = new double[]{1.0};

        assertEquals(-6.908755278962187, wd.logPdf(x), 1e-10);
        assertEquals(-6.915086640662835, gd.logDensity(x[0]), 1e-10);


        wd = new beastclassic.dr.math.distributions.WishartDistribution(4, new Double[]{5.0});
        gd = org.apache.commons.statistics.distribution.GammaDistribution.of(2.0, 10.0);
        x = new double[]{1.0};
        assertEquals(-4.7051701859880914, wd.logPdf(x), 1e-10);

        wd = new beastclassic.dr.math.distributions.WishartDistribution(1);
        x = new double[]{0.1};
        assertEquals(2.3025850929940455, wd.logPdf(x), 1e-10);
        x = new double[]{1.0};
        assertEquals(0.0, wd.logPdf(x), 1e-10);
        x = new double[]{10.0};
        assertEquals(-2.302585092994046, wd.logPdf(x), 1e-10);

        System.out.println("Wishart, uninformative, PDF(10.0): " + wd.logPdf(x));

	}
}
