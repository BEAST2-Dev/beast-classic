package beast.evolution.operators;

import java.text.DecimalFormat;

import org.json.JSONObject;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.OperatorSchedule;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
import beast.util.Randomizer;

@Description("Produces proposals of a random walk on a sphere")
public class SphereRandomWalker extends Operator {
	public Input<RealParameter> locationInput = new Input<RealParameter>("location", "latitude/longitude pairs representing location", Validate.REQUIRED);
    public Input<Double> windowSizeInput =
            new Input<Double>("windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian", Input.Validate.REQUIRED);
    public Input<Boolean> useGaussianInput =
            new Input<Boolean>("useGaussian", "Use Gaussian to move instead of uniform interval. Default false.", false);

    public Input<Operator> operatorInput = new Input<Operator>("operator" ,"optional tree operator -- locations of filthy " +
            "nodes will get a new location");
    public Input<Boolean> optimiseInput = new Input<Boolean>("optimise", "if true, the window size will be optimised " +
            "throughout the MCMC run", true);
    
    
    
	RealParameter location;
	double windowSize;
	int range;
	boolean useGaussian;
	
	Operator operator;
	TreeInterface tree;
	
	boolean optimise = false;

	@Override
	public void initAndValidate() {
		location = locationInput.get();
		windowSize = windowSizeInput.get();
		range = 1 + location.getDimension()/2;
		useGaussian = useGaussianInput.get();
		if (operatorInput.get() != null) {
			operator = operatorInput.get();
			operator.setOperatorSchedule(new OperatorSchedule());
			tree = (TreeInterface) operator.getInput("tree").get();
		}
		optimise = optimiseInput.get();
	}

	@Override
	public double proposal() {
		if (operator != null) {
			// do operator, then do random walk on all filthy nodes
			double logHR = operator.proposal();
			for (Node node : tree.getNodesAsArray()) {
				if (node.isDirty() == Tree.IS_FILTHY) {
                    // Why not isLeaf?? (JH) (Q2R)
					if (node.getNr() >= tree.getLeafNodeCount()) {
						logHR += doproposal(2 * node.getNr());
					}
				}
			}
			return logHR;
		}
		
        final int i = Randomizer.nextInt(range/2-1);
        return doproposal(range + i * 2);
	}
	
	public double doproposal(int nodeIndex) {
        double latitude = location.getValue(nodeIndex);
        double longitude = location.getValue(nodeIndex + 1);
        
        double newLatitude = latitude;
        if (useGaussian) {
            newLatitude += Randomizer.nextGaussian() * windowSize;
        } else {
            newLatitude += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }
        double newLongitude = longitude;
        if (useGaussian) {
            newLongitude += Randomizer.nextGaussian() * windowSize;
        } else {
            newLongitude += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newLatitude < -90) {
        	newLatitude = -90 + (-90 - newLatitude);
        	newLongitude += 180;
        }
        if (newLatitude > 90) {
        	newLatitude = 90 - (newLatitude - 90);  
        	newLongitude += 180;
        }
        
        if (newLatitude < location.getLower() || newLatitude > location.getUpper()) {
            return Double.NEGATIVE_INFINITY;
        }
        
        while (newLongitude < -180) {
        	newLongitude += 360;
        }
        while (newLongitude > 180) {
        	newLongitude -= 360;
        }
        if (newLongitude < location.getLower() || newLongitude > location.getUpper()) {
            return Double.NEGATIVE_INFINITY;
        }
        
        if (newLatitude == latitude && newLongitude == longitude) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }

        location.setValue(nodeIndex, newLatitude);
        location.setValue(nodeIndex + 1, newLongitude);

        return 0.0;
	}

	@Override
	public void accept() {
		if (operator != null) {
			operator.accept();
		}
		super.accept();
	}
	
	@Override
	public void reject() {
		if (operator != null) {
			operator.reject();
		}
		super.reject();
	}
	
	@Override
	public void restoreFromFile(JSONObject o) {
		super.restoreFromFile(o);
		if (operator != null) {
			// TODO
		}
	}
    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double fValue) {
        windowSize = fValue;
    }
    @Override
    public void optimize(double logAlpha) {
    	if (optimise) {
    		if (operator != null) {
    			operator.optimize(logAlpha);
    			return;
    		}
	        // must be overridden by operator implementation to have an effect
	        double fDelta = calcDelta(logAlpha);
	
	        fDelta += Math.log(windowSize);
	        windowSize = Math.exp(fDelta);
    	}
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > 0.40) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
}
