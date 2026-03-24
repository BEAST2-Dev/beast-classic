package test.continuous;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beastclassic.spec.parameter.MatrixVectorParam;
import beastclassic.continuous.MultivariateDiffusionModel;
import beastclassic.evolution.tree.TreeTraitMap;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;

public class MatrixVectorParamTest {

    @Test
    public void testMatrixVectorParamBasics() {
        // 2x3 matrix stored as flat array: row0=[1,2,3], row1=[4,5,6]
        MatrixVectorParam<Real> p = new MatrixVectorParam<>();
        p.initByName("value", "1.0 2.0 3.0 4.0 5.0 6.0", "minordimension", 3);

        assertEquals(3, p.getMinorDimension1()); // columns
        assertEquals(2, p.getMinorDimension2()); // rows
        assertEquals(6, p.size());

        // Matrix access
        assertEquals(1.0, p.getMatrixValue(0, 0), 1e-10);
        assertEquals(2.0, p.getMatrixValue(0, 1), 1e-10);
        assertEquals(3.0, p.getMatrixValue(0, 2), 1e-10);
        assertEquals(4.0, p.getMatrixValue(1, 0), 1e-10);
        assertEquals(5.0, p.getMatrixValue(1, 1), 1e-10);
        assertEquals(6.0, p.getMatrixValue(1, 2), 1e-10);

        // Row extraction
        double[] row = new double[3];
        p.getMatrixValues1(0, row);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, row, 1e-10);
        p.getMatrixValues1(1, row);
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, row, 1e-10);

        // Matrix set
        p.setMatrixValue(0, 1, 99.0);
        assertEquals(99.0, p.getMatrixValue(0, 1), 1e-10);
        assertEquals(99.0, p.get(1), 1e-10); // flat index 1

        // Size and element access
        assertEquals(6, p.size());
        assertEquals(1.0, p.get(0), 1e-10);
        assertEquals(99.0, p.get(1), 1e-10);
    }

    @Test
    public void testMatrixVectorParamMinorDim2() {
        // Typical 2D trait parameter: minordimension=2 (lat/long per node)
        // 3 nodes, 2 traits each
        MatrixVectorParam<Real> p = new MatrixVectorParam<>();
        p.initByName("value", "10.0 20.0 30.0 40.0 50.0 60.0", "minordimension", 2);

        assertEquals(2, p.getMinorDimension1());
        assertEquals(3, p.getMinorDimension2());

        // Node 0: (10, 20), Node 1: (30, 40), Node 2: (50, 60)
        double[] trait = new double[2];
        p.getMatrixValues1(0, trait);
        assertArrayEquals(new double[]{10.0, 20.0}, trait, 1e-10);
        p.getMatrixValues1(1, trait);
        assertArrayEquals(new double[]{30.0, 40.0}, trait, 1e-10);
        p.getMatrixValues1(2, trait);
        assertArrayEquals(new double[]{50.0, 60.0}, trait, 1e-10);
    }

    @Test
    public void testDiffusionModelLogLikelihood() {
        // Known diffusion model: 2x2 identity precision matrix
        // logLikelihood of going from (0,0) to (1,1) in time 1
        // = MultivariateNormal logpdf with mean=(0,0), precision=I/time=I
        // = -0.5 * (x-mu)' P (x-mu) - 0.5 * d * log(2pi) + 0.5 * log|P|
        // = -0.5 * (1+1) - 0.5 * 2 * log(2pi) + 0.5 * log(1)
        // = -1.0 - 1.8378... = -2.8378...
        MatrixVectorParam<Real> precMatrix = new MatrixVectorParam<>();
        precMatrix.initByName("value", "1.0 0.0 0.0 1.0", "minordimension", 2);

        MultivariateDiffusionModel model = new MultivariateDiffusionModel();
        model.initByName("precisionMatrix", precMatrix);

        double[] start = {0.0, 0.0};
        double[] stop = {1.0, 1.0};
        double time = 1.0;

        double logL = model.getLogLikelihood(start, stop, time);
        double expected = -1.0 - Math.log(2 * Math.PI); // -1 - 1.8378... = -2.8378...
        assertEquals(expected, logL, 1e-10);

        // Same displacement but time=0.5 -> precision is 2*I
        // = -0.5 * 2*(1+1) - 0.5 * 2 * log(2pi) + 0.5 * log(4)
        // but MultivariateNormalDistribution.logPdf divides precision by time
        // so effective precision = I/0.5 = 2I
        // logL = -0.5*(1+1)*2 - log(2pi) + 0.5*log(4) = -2 - 1.8378 + 0.6931 = -3.1447
        double logL2 = model.getLogLikelihood(start, stop, 0.5);
        assertTrue(logL2 < logL, "Shorter time should give lower likelihood for same displacement");

        // Zero displacement at any time should give a finite positive-ish log likelihood
        double logL3 = model.getLogLikelihood(start, start, 1.0);
        assertTrue(logL3 > logL, "Zero displacement should have higher likelihood");
    }

    @Test
    public void testTreeTraitMapWithMatrixVectorParam() {
        // 3-taxon tree: ((A:1,B:1):1,C:2)
        Tree tree = new TreeParser("((A:1,B:1):1,C:2)", false, false, true, 0);
        // 5 nodes: A=0, B=1, C=2, AB=3, root=4

        // Create a MatrixVectorParam with minordimension=2, pre-sized for 5 nodes
        MatrixVectorParam<Real> param = new MatrixVectorParam<>();
        param.initByName("value", "0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "minordimension", 2);
        // 10 values = 5 nodes * 2 traits

        assertEquals(2, param.getMinorDimension1());
        assertEquals(5, param.getMinorDimension2());

        // Set known trait values: A=(10,20), B=(30,40), C=(50,60), AB=(15,25), root=(20,30)
        param.setMatrixValue(0, 0, 10.0); param.setMatrixValue(0, 1, 20.0); // A
        param.setMatrixValue(1, 0, 30.0); param.setMatrixValue(1, 1, 40.0); // B
        param.setMatrixValue(2, 0, 50.0); param.setMatrixValue(2, 1, 60.0); // C
        param.setMatrixValue(3, 0, 15.0); param.setMatrixValue(3, 1, 25.0); // AB
        param.setMatrixValue(4, 0, 20.0); param.setMatrixValue(4, 1, 30.0); // root

        // Create TreeTraitMap
        TreeTraitMap traitMap = new TreeTraitMap();
        traitMap.initByName("tree", tree, "parameter", param, "traitName", "location");

        assertEquals(2, traitMap.traitDim);

        // Check traits for each node
        double[] traitA = traitMap.getTrait(tree, tree.getNode(0));
        assertArrayEquals(new double[]{10.0, 20.0}, traitA, 1e-10, "Node A trait");

        double[] traitB = traitMap.getTrait(tree, tree.getNode(1));
        assertArrayEquals(new double[]{30.0, 40.0}, traitB, 1e-10, "Node B trait");

        double[] traitC = traitMap.getTrait(tree, tree.getNode(2));
        assertArrayEquals(new double[]{50.0, 60.0}, traitC, 1e-10, "Node C trait");

        double[] traitAB = traitMap.getTrait(tree, tree.getNode(3));
        assertArrayEquals(new double[]{15.0, 25.0}, traitAB, 1e-10, "Node AB trait");

        double[] traitRoot = traitMap.getTrait(tree, tree.getRoot());
        assertArrayEquals(new double[]{20.0, 30.0}, traitRoot, 1e-10, "Root trait");

        // Now test the diffusion model on this tree
        // Identity precision matrix
        MatrixVectorParam<Real> precMatrix = new MatrixVectorParam<>();
        precMatrix.initByName("value", "1.0 0.0 0.0 1.0", "minordimension", 2);

        MultivariateDiffusionModel diffModel = new MultivariateDiffusionModel();
        diffModel.initByName("precisionMatrix", precMatrix);

        // Branch A->AB: displacement=(10-15, 20-25)=(-5,-5), time=1
        // logL = -0.5 * (25+25)/1 - log(2pi) + 0.5*log(1) = -25 - 1.8378 = -26.8378
        double logL_A = diffModel.getLogLikelihood(
                traitMap.getTrait(tree, tree.getNode(3)),  // parent AB
                traitMap.getTrait(tree, tree.getNode(0)),  // child A
                1.0);
        double expected_A = -0.5 * 50.0 - Math.log(2 * Math.PI);
        assertEquals(expected_A, logL_A, 1e-8, "Branch A->AB logL");
    }

    @Test
    public void testSetDimensionPreservesMinorDimension() {
        MatrixVectorParam<Real> p = new MatrixVectorParam<>();
        p.initByName("value", "0.0 0.0", "minordimension", 2);

        assertEquals(2, p.getMinorDimension1());
        assertEquals(1, p.getMinorDimension2()); // 1 row

        // Simulate what TreeTraitMap does: expand to nNodes rows
        p.setDimension(6); // 3 nodes * 2 traits
        assertEquals(2, p.getMinorDimension1()); // should still be 2
        assertEquals(3, p.getMinorDimension2()); // now 3 rows
        assertEquals(6, p.size());
    }
}
