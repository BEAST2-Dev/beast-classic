package test.continuous;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.Real;
import beast.base.evolution.tree.TreeParser;
import beastclassic.continuous.MultivariateDiffusionModel;
import beastclassic.continuous.SampledMultivariateTraitLikelihood;
import beastclassic.evolution.alignment.AlignmentFromTraitMap;
import beastclassic.evolution.datatype.LocationDataType;
import beastclassic.evolution.tree.TreeTraitMap;
import beastclassic.spec.parameter.MatrixVectorParam;

/**
 * Tests for the continuous trait diffusion likelihood on small trees
 * with known trait values and hand-calculable expected likelihoods.
 */
public class SampledMultivariateTraitLikelihoodTest {

    /**
     * Test diffusion likelihood on a 3-taxon tree with identity precision.
     *
     * Tree: ((A:1,B:1):1,C:2)
     * Traits: A=(0,0), B=(1,0), C=(0,1), AB=(0.5,0), root=(0.25,0.25)
     * Precision: identity matrix
     * No rate model, scaleByTime=false, useTreeLength=false
     *
     * The trait logL is the sum over branches of:
     *   MultivariateNormal.logPdf(child, parent, precision/time)
     *
     * For 2D with identity precision and branch length t:
     *   logL = -0.5 * |delta|^2 / t - log(2*pi*t)
     *   where |delta|^2 = sum of squared differences
     */
    @Test
    public void testDiffusionLikelihoodOnSmallTree() throws Exception {
        // Tree: ((A:1,B:1):1,C:2);  5 nodes: A=0, B=1, C=2, AB=3, root=4
        TreeParser tree = new TreeParser("((A:1.0,B:1.0):1.0,C:2.0);", false, false, true, 0);

        // Trait parameter: 5 nodes * 2 dims = 10 values
        // A=(0,0), B=(1,0), C=(0,1), AB=(0.5,0), root=(0.25,0.25)
        MatrixVectorParam<Real> traits = new MatrixVectorParam<>();
        traits.initByName("value", "0.0 0.0 1.0 0.0 0.0 1.0 0.5 0.0 0.25 0.25",
                "minordimension", 2);

        // Identity precision matrix
        MatrixVectorParam<Real> precision = new MatrixVectorParam<>();
        precision.initByName("value", "1.0 0.0 0.0 1.0", "minordimension", 2);

        MultivariateDiffusionModel diffModel = new MultivariateDiffusionModel();
        diffModel.initByName("precisionMatrix", precision);

        // Build TreeTraitMap
        TreeTraitMap traitMap = new TreeTraitMap();
        traitMap.initByName("tree", tree, "parameter", traits, "traitName", "location");

        // Build AlignmentFromTraitMap
        LocationDataType locDataType = new LocationDataType();
        locDataType.initAndValidate();
        AlignmentFromTraitMap alignment = new AlignmentFromTraitMap();
        alignment.initByName("traitMap", traitMap, "userDataType", locDataType);

        // Build SampledMultivariateTraitLikelihood
        // Using a simple SiteModel wrapper for the diffusion model
        beast.base.evolution.sitemodel.SiteModel siteModel = new beast.base.evolution.sitemodel.SiteModel();
        siteModel.initByName("substModel", diffModel);

        SampledMultivariateTraitLikelihood likelihood = new SampledMultivariateTraitLikelihood();
        likelihood.initByName(
                "tree", tree,
                "traitParameter", traits,
                "data", alignment,
                "siteModel", siteModel,
                "useTreeLength", false,
                "scaleByTime", false,
                "reciprocalRates", false,
                "reportAsMultivariate", true
        );

        double logL = likelihood.calculateLogP();

        // Hand-calculate expected likelihood:
        // Branch A->AB: parent=(0.5,0), child=(0,0), t=1, delta=(-0.5,0), |d|^2=0.25
        //   logL = -0.5*0.25/1 - log(2*pi*1) = -0.125 - 1.8378... = -1.9628...
        // Branch B->AB: parent=(0.5,0), child=(1,0), t=1, delta=(0.5,0), |d|^2=0.25
        //   logL = -0.125 - 1.8378... = -1.9628...
        // Branch AB->root: parent=(0.25,0.25), child=(0.5,0), t=1, delta=(0.25,-0.25), |d|^2=0.125
        //   logL = -0.5*0.125/1 - 1.8378... = -0.0625 - 1.8378... = -1.9003...
        // Branch C->root: parent=(0.25,0.25), child=(0,1), t=2, delta=(-0.25,0.75), |d|^2=0.625
        //   logL = -0.5*0.625/2 - log(2*pi*2) = -0.15625 - 2.5310... = -2.6872...
        // Total = -1.9628 - 1.9628 - 1.9003 - 2.6872 = -8.5132...

        double log2pi = Math.log(2 * Math.PI);

        double logL_A  = -0.5 * 0.25 / 1.0 - log2pi;        // branch A->AB
        double logL_B  = -0.5 * 0.25 / 1.0 - log2pi;        // branch B->AB
        double logL_AB = -0.5 * 0.125 / 1.0 - log2pi;       // branch AB->root
        double logL_C  = -0.5 * 0.625 / 2.0 - Math.log(2 * Math.PI * 2.0); // branch C->root

        double expected = logL_A + logL_B + logL_AB + logL_C;

        assertEquals(expected, logL, 1e-8,
                "Diffusion likelihood on ((A:1,B:1):1,C:2) with identity precision");
    }
}
