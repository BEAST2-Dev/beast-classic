package beast.evolution.tree.coalescent;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("A demographic model of constant population size followed by exponential growth.")
public class Expansion extends ExponentialGrowth {
	final public Input<RealParameter> ancestralPopulationProportionInput = new Input<>("ancestralPopulationProportion", "", Validate.REQUIRED);
	

	@Override
	public List<String> getParameterIds() {
		List<String> ids = new ArrayList<>();
		ids.add(popSizeParameterInput.get().getID());
		ids.add(growthRateParameterInput.get().getID());
		ids.add(ancestralPopulationProportionInput.get().getID());
        return ids;
	}


	@Override
	public double getPopSize(double t) {

        double N0 = getN0();
        double N1 = getN1();
        double r = getGrowthRate();

        if (N1 > N0) throw new IllegalArgumentException("N0 must be greater than N1!");

        return N1 + ((N0 - N1) * Math.exp(-r * t));
    }

    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
	@Override
    public double getIntensity(double t) {
        double N0 = getN0();
        double N1 = getN1();
        double b = (N0 - N1);
        double r = getGrowthRate();

        return Math.log(b + N1 * Math.exp(r * t)) / (r * N1);
    }

	@Override
    public double getInverseIntensity(double x) {

        /* AER - I think this is right but until someone checks it...
          double nZero = getN0();
          double nOne = getN1();
          double r = getGrowthRate();

          if (r == 0) {
              return nZero*x;
          } else if (alpha == 0) {
              return Math.log(1.0+nZero*x*r)/r;
          } else {
              return Math.log(-(nOne/nZero) + Math.exp(nOne*x*r))/r;
          }
          */
        throw new RuntimeException("Not implemented!");
    }

    /**
     * @return initial population size.
     */
    public double getN1() {
        return ancestralPopulationProportionInput.get().getValue();
    }

}
