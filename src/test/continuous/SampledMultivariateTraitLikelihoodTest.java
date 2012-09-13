package test.continuous;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import beast.core.Distribution;
import beast.core.MCMC;
import beast.util.Randomizer;
import beast.util.XMLParser;

public class SampledMultivariateTraitLikelihoodTest {

	@Test
	public void testSampledMultivariateTraitLikelihood() throws Exception {
		Randomizer.setSeed(123);
		
		XMLParser parser = new XMLParser();
		MCMC mcmc = (MCMC) parser.parseFile(new File("examples/RacRABV_LogNRRW2.xml"));
		Distribution posterior = mcmc.posteriorInput.get();
		double logP = mcmc.robustlyCalcPosterior(posterior);
        assertEquals(-3042252.6578551414, logP, 1e-10);
	}

}
