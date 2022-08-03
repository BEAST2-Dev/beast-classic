package test.continuous;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import beast.base.inference.Distribution;
import beast.base.inference.MCMC;
import beast.base.util.Randomizer;
import beast.base.parser.XMLParser;



public class SampledMultivariateTraitLikelihoodTest {

	@Test
	public void testSampledMultivariateTraitLikelihood() throws Exception {
		Randomizer.setSeed(123);
		
		XMLParser parser = new XMLParser();
		MCMC mcmc = (MCMC) parser.parseFile(new File("examples/RacRABV_LogNRRW2.xml"));
		Distribution posterior = mcmc.posteriorInput.get();
		double logP = mcmc.robustlyCalcPosterior(posterior);
		// logP changed with reimplementation of relaxed clock
        // assertEquals(-3042252.6578551414, logP, 1e-5);
		// another relaxed clock update
		// assertEquals(-854843.2551478981, logP, 1e-5);
		assertEquals(-835885.9143802306, logP, 1e-5);        
	}

	@Test
	public void testAncestralStateTreeLikelihood() throws Exception {
		Randomizer.setSeed(123);
		
		XMLParser parser = new XMLParser();
		MCMC mcmc = (MCMC) parser.parseFile(new File("examples/H5N1_HA_discrete2.xml"));
		Distribution posterior = mcmc.posteriorInput.get();
		double logP = mcmc.robustlyCalcPosterior(posterior);
		// used to be -17376.726764175364 before ClusterTree was made to scale branch lengths by 1/2
		// logP changed with reimplementation of relaxed clock
        // assertEquals(-17417.93849467215, logP, 1e-5);
        // assertEquals(-15883.60532266501, logP, 1e-5);
		assertEquals(-15892.40883050923, logP, 1e-5);
	}

}
