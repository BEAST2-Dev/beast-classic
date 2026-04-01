package beastclassic.evolution.speciation;

import java.util.Set;

import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.type.RealScalar;
import beast.base.evolution.alignment.Taxon;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;

/**
 * Beginning of tree prior for birth-death + serial sampling + extant sample proportion. More Tanja magic...
 *
 * @author Alexei Drummond
 */
public class BirthDeathSerialSampling extends TreeDistribution {

    //boolean death rate is relative?
    boolean relativeDeath = false;

    boolean hasFinalSample = false;

    final public Input<Boolean> relativeDeathInput = new Input<>("relativeDeath","? ? ? ? ");
    final public Input<RealScalar<? extends PositiveReal>> lambdaInput = new Input<>("lambda","birth rate");
    final public Input<RealScalar<? extends NonNegativeReal>> muInput = new Input<>("mu","death rate");
    final public Input<RealScalar<? extends NonNegativeReal>> psiInput = new Input<>("psi","serial sampling rate");
    final public Input<RealScalar<? extends UnitInterval>> pInput = new Input<>("p","extant sampling proportion");
    final public Input<Boolean> hasFinalSampleInput = new Input<>("hasFinalSample","? ? ? ? ");
    final public Input<RealScalar<? extends UnitInterval>> rInput = new Input<>("r","removal probability upon sampling");
    final public Input<RealScalar<? extends PositiveReal>> originInput = new Input<>("origin","the origin of the infection");

    // Alternative parameterisation fields
    private RealScalar<? extends PositiveReal> R0;
    private RealScalar<? extends PositiveReal> recoveryRate;
    private RealScalar<? extends UnitInterval> samplingProbability;

    @Override
    public void initAndValidate() {
    	super.initAndValidate();
        this.relativeDeath = relativeDeathInput.get();
        this.hasFinalSample = hasFinalSampleInput.get();
        // domain constraints handled by spec types
    }

    /**
     * @param b   birth rate
     * @param d   death rate
     * @param p   proportion sampled at final time point
     * @param psi rate of sampling per lineage per unit time
     * @param t   time
     * @return the probability of no sampled descendants after time, t
     */
    public static double p0(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);

        double expc1trc2 = Math.exp(-c1 * t) * (1.0 - c2);

        return (b + d + psi + c1 * ((expc1trc2 - (1.0 + c2)) / (expc1trc2 + (1.0 + c2)))) / (2.0 * b);
    }

    public static double q(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);
        double res = c1 * t + 2.0 * Math.log( Math.exp(-c1 * t) * (1.0 - c2) + (1.0 + c2) ); // operate directly in logspace, c1 * t too big
        return res;
    }

    private static double c1(double b, double d, double psi) {
        return Math.abs(Math.sqrt(Math.pow(b - d - psi, 2.0) + 4.0 * b * psi));
    }

    private static double c2(double b, double d, double p, double psi) {
        return -(b - d - 2.0 * b * p - psi) / c1(b, d, psi);
    }


    public double p0(double t) {
        return p0(birth(), death(), p(), psi(), t);
    }

    public double q(double t) {
        return q(birth(), death(), p(), psi(), t);
    }

    private double c1() {
        return c1(birth(), death(), psi());
    }

    private double c2() {
        return c2(birth(), death(), p(), psi());
    }

    public double birth() {
        if (lambdaInput.get() != null) {
            return lambdaInput.get().get();
        } else {
            double r0 = R0.get();
            double rr = recoveryRate.get();
            return r0 * rr;
        }
    }

    public double death() {
        if (muInput.get() != null) {
            return relativeDeath ? muInput.get().get() * birth() : muInput.get().get();
        } else {
            double rr = recoveryRate.get();
            double sp = samplingProbability.get();
            return rr * (1.0 - sp);
        }
    }

    public double psi() {
        if (psiInput.get() != null) {
            return psiInput.get().get();
        } else {
            double rr = recoveryRate.get();
            double sp = samplingProbability.get();
            return rr * sp;
        }
    }

    /**
     * @return the proportion of population sampled at final sample, or zero if there is no final sample
     */
    public double p() {
        return hasFinalSample ? pInput.get().get() : 0;
    }

    public boolean isSamplingOrigin() {
        return originInput.get() != null;
    }

    public double x0() {
        return originInput.get().get();
    }

    @Override
    public double calculateLogP() {
    	logP = calculateTreeLogLikelihood((Tree) treeInput.get());
    	return logP;
    }

    /**
     * Generic likelihood calculation
     *
     * @param tree the tree to calculate likelihood of
     * @return log-likelihood of density
     */
    public final double calculateTreeLogLikelihood(Tree tree) {

        if (isSamplingOrigin() && x0() < tree.getRoot().getHeight()) {
            return Double.NEGATIVE_INFINITY;
        }

        // extant leaves
        int n = 0;
        // extinct leaves
        int m = 0;

        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
            Node node = tree.getNode(i);
            if (node.getHeight() == 0.0) {
                n += 1;
            } else {
                m += 1;
            }
        }

        if (!hasFinalSample && n < 1) {
            throw new RuntimeException(
                    "For sampling-through-time model there must be at least one tip at time zero.");
        }

        double b = birth();
        double p = p();

        double logL;
        if (isSamplingOrigin()) {
            logL = - q(x0());
        } else {
            throw new RuntimeException(
                    "The origin must be sampled, as integrating it out is not implemented!");
        }
        if (hasFinalSample) {
            logL += n * Math.log(4.0 * p);
        }
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            double x = tree.getNode(tree.getLeafNodeCount() + i).getHeight();
            logL += Math.log(b) - q(x);
        }
        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
            double y = tree.getNode(i).getHeight();

            if (y > 0.0) {
                logL += Math.log(psi()) + q(y);
            } else if (!hasFinalSample) {
                logL += Math.log(psi()) + q(y);
            }
        }

        return logL;
    }

    public double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude) {
        if (exclude.size() == 0) return calculateTreeLogLikelihood(tree);
        throw new RuntimeException("Not implemented!");
    }
}
