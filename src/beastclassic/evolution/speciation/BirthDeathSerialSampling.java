package beastclassic.evolution.speciation;

import java.util.Set;

import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
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

    // R0
    RealParameter R0;

    // recovery rate
    RealParameter recoveryRate;

    // sampling probability
    RealParameter samplingProbability;


    // birth rate
    RealParameter lambda;

    // death rate
    RealParameter mu;

    // serial sampling rate
    RealParameter psi;

    // extant sampling proportion
    RealParameter p;

    //boolean death rate is relative?
    boolean relativeDeath = false;

    // boolean stating whether sampled individuals remain infectious, or become non-infectious
//    boolean sampledIndividualsRemainInfectious = false; // replaced by r

    //    the additional parameter 0 <= r <= 1 has to be estimated.
    //    for r=1, this is sampledRemainInfectiousProb=0
    //    for r=0, this is sampledRemainInfectiousProb=1
    RealParameter r;

    //RealParameter finalTimeInterval;

    boolean hasFinalSample = false;

    // the origin of the infection, x0 > tree.getRoot();
    RealParameter origin;
    public BirthDeathSerialSampling() {
    	
    }

    public BirthDeathSerialSampling(
            RealParameter lambda,
            RealParameter mu,
            RealParameter psi,
            RealParameter p,
            boolean relativeDeath,
            RealParameter r,
            boolean hasFinalSample,
            RealParameter origin) {
            //Type units) {

        this("birthDeathSerialSamplingModel", lambda, mu, psi, p, relativeDeath, r, hasFinalSample, origin/*, units*/);
    }

    final public Input<Boolean> relativeDeathInput = new Input<>("relativeDeath","? ? ? ? ");
    final public Input<RealParameter> lambdaInput = new Input<>("lambda","? ? ? ? ");
    final public Input<RealParameter> muInput = new Input<>("mu","? ? ? ? ");
    final public Input<RealParameter> psiInput = new Input<>("psi","? ? ? ? ");
    final public Input<RealParameter> pInput = new Input<>("p","? ? ? ? ");
    final public Input<Boolean> hasFinalSampleInput = new Input<>("hasFinalSample","? ? ? ? ");
    final public Input<RealParameter> rInput = new Input<>("r","? ? ? ? ");
    final public Input<RealParameter> originInput = new Input<>("origin","? ? ? ? ");
    
    @Override
    public void initAndValidate() {
    	super.initAndValidate();
        this.relativeDeath = relativeDeathInput.get();

        this.lambda = lambdaInput.get();
        //addVariable(lambda);
        lambda.setBounds(0.0, Double.POSITIVE_INFINITY);//new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.mu = muInput.get();
        //addVariable(mu);
        mu.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.psi = psiInput.get();
        //addVariable(psi);
        psi.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.p = pInput.get();
        //addVariable(p);
        p.setBounds(0.0, 1.0);

        this.hasFinalSample = hasFinalSampleInput.get();

        this.r = rInput.get();
        //addVariable(r);
        r.setBounds(0.0, 1.0);

        this.origin = originInput.get();
        if (origin != null) {
            //addVariable(origin);
            origin.setBounds(0.0, Double.POSITIVE_INFINITY);
        }
    }
    
    public BirthDeathSerialSampling(
            String modelName,
            RealParameter lambda,
            RealParameter mu,
            RealParameter psi,
            RealParameter p,
            boolean relativeDeath,
            RealParameter r,
            boolean hasFinalSample,
            RealParameter origin) {
            //Type units) {

        //super(modelName, units);

        this.relativeDeath = relativeDeath;

        this.lambda = lambda;
        //addVariable(lambda);
        lambda.setBounds(0.0, Double.POSITIVE_INFINITY);//new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.mu = mu;
        //addVariable(mu);
        mu.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.psi = psi;
        //addVariable(psi);
        psi.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.p = p;
        //addVariable(p);
        p.setBounds(0.0, 1.0);

        this.hasFinalSample = hasFinalSample;

        this.r = r;
        //addVariable(r);
        r.setBounds(0.0, 1.0);

        this.origin = origin;
        if (origin != null) {
            //addVariable(origin);
            origin.setBounds(0.0, Double.POSITIVE_INFINITY);
        }
    }

    public BirthDeathSerialSampling(
            String modelName,
            RealParameter R0,
            RealParameter recoveryRate,
            RealParameter samplingProbability,
            RealParameter origin ) {
            //Type units) {

        //super(modelName, units);

        this.relativeDeath = false;
        this.hasFinalSample = false;

        this.R0 = R0;
        //addVariable(R0);
        R0.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.recoveryRate = recoveryRate;
        //addVariable(recoveryRate);
        recoveryRate.setBounds(0.0, Double.POSITIVE_INFINITY);

        this.samplingProbability = samplingProbability;
        //addVariable(samplingProbability);
        samplingProbability.setBounds(0.0, 1.0);

        this.origin = origin;
        if (origin != null) {
            //addVariable(origin);
            origin.setBounds(0.0, Double.POSITIVE_INFINITY);
        }
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
//        double res = 2.0 * (1.0 - c2 * c2) + Math.exp(-c1 * t) * (1.0 - c2) * (1.0 - c2) + Math.exp(c1 * t) * (1.0 + c2) * (1.0 + c2);
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
//        if (mask != null) return mask.birth();
        if (lambda != null) {
            return lambda.getValue(0);
        } else {
            double r0 = R0.getValue(0);
            double rr = recoveryRate.getValue(0);
            return r0 * rr;
        }
    }

    public double death() {
//        if (mask != null) return mask.death();
        if (mu != null) {
            return relativeDeath ? mu.getValue(0) * birth() : mu.getValue(0);
        } else {
            double rr = recoveryRate.getValue(0);
            double sp = samplingProbability.getValue(0);

            return rr * (1.0 - sp);
        }
    }

    public double psi() {
 //       if (mask != null) return mask.psi();

        if (psi != null) {
        return psi.getValue(0);
        } else {
            double rr = recoveryRate.getValue(0);
            double sp = samplingProbability.getValue(0);

            return rr * sp;
        }
    }

    /**
     * @return the proportion of population sampled at final sample, or zero if there is no final sample
     */
    public double p() {

//        if (mask != null) return mask.p.getValue(0);
        return hasFinalSample ? p.getValue(0) : 0;
    }

    // The mask does not affect the following three methods

    public boolean isSamplingOrigin() {
        return origin != null;
    }

    public double x0() {
        return origin.getValue(0);
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
//            throw new RuntimeException("Orign value (" + x0() + ") cannot < tree root height (" + tree.getNodeHeight(tree.getRoot()) + ")");
        }

        //System.out.println("calculating tree log likelihood");
        //double time = finalTimeInterval();

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
//            logL = Math.log(1.0 / q(x0()));
            logL = - q(x0());
            //System.out.println("originLogL=" + logL + " x0");
        } else {
            throw new RuntimeException(
                    "The origin must be sampled, as integrating it out is not implemented!");
            // integrating out the time between the origin and the root of the tree
            //double bottom = c1 * (c2 + 1) * (1 - c2 + (1 + c2) * Math.exp(c1 * x1));
            //logL = Math.log(1 / bottom);
        }
        if (hasFinalSample) {
            logL += n * Math.log(4.0 * p);
        }
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            double x = tree.getNode(tree.getLeafNodeCount() + i).getHeight();
            logL += Math.log(b) - q(x);

            //System.out.println("internalNodeLogL=" + Math.log(b / q(x)));

        }
        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
            double y = tree.getNode(i).getHeight();

            if (y > 0.0) {
                logL += Math.log(psi()) + q(y);

                //System.out.println("externalNodeLogL=" + Math.log(psi() * (r() + (1.0 - r()) * p0(y)) * q(y)));

            } else if (!hasFinalSample) {
                //handle condition ending on final tip in sampling-through-time-only situation
                logL += Math.log(psi()) + q(y);
//                System.out.println("externalNodeLogL=" + Math.log(psi() * q(y)));

            }
        }

        return logL;
    }

    public double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude) {
        if (exclude.size() == 0) return calculateTreeLogLikelihood(tree);
        throw new RuntimeException("Not implemented!");
    }

//    public void mask(SpeciationModel mask) {
//        if (mask instanceof BirthDeathSerialSamplingModel) {
//            this.mask = (BirthDeathSerialSamplingModel) mask;
//        } else {
//            throw new IllegalArgumentException();
//        }
//    }

//    public void unmask() {
//        mask = null;
//    }

    // if a mask exists then use the mask's parameters instead (except for origin and finalTimeInterval)
//    BirthDeathSerialSamplingModel mask = null;
}