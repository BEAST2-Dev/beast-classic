package beast.evolution.substitutionmodel;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.util.FastMath;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.TraitSet;


@Description("Extracts the intervals from a tree. Points in the intervals " +
        "are defined by the heights of nodes in the tree.")
public class GLM extends CalculationNode implements Loggable {	
    
	public Input<GlmModel> migrationGLMInput = new Input<>(
			"migrationGLM", "input of migration GLM model", Validate.REQUIRED);
    
	public Input<GlmModel> NeGLMInput = new Input<>(
			"NeGLM", "input of migration GLM model", Validate.REQUIRED);
    
    public Input<RealParameter> rateShiftsInput = new Input<>(
    		"rateShifts", "input of timings of rate shifts relative to the most recent sample", Validate.OPTIONAL);    
     
	public Input<Double> maxRateInput = new Input<>(
			"maxRate", "maximum rate used for integration", Double.POSITIVE_INFINITY);

    public Input<Integer> dimensionInput = new Input<>("dimension", "the number of different states." + 
    		" if -1, it will use the number of different types ", -1);
    
    public Input<TraitSet> typeTraitInput = new Input<>("typeTrait", "Type trait set.  Used only by BEAUti.");
    
    public Input<String> typesInput = new Input<>(
    		"types", "input of the different types, can be helpful for multilocus data", Validate.XOR, dimensionInput);

        
    double [] intTimes;
	int firstlargerzero;
	
	public GLM(){
    	typesInput.setRule(Input.Validate.REQUIRED);		
	}
	
	HashMap<String, Integer> traitToType = new HashMap<>(); 
	HashMap<Integer, String> reverseTraitToType;
	
    @Override
    public void initAndValidate() {    	
    	if (typesInput.get()!=null){
    		String[] splittedTypes = typesInput.get().split("\\s+");
    		
    		dimensionInput.set(splittedTypes.length);

    		traitToType = new HashMap<>();
    		reverseTraitToType = new HashMap<>();
    		for (int i = 0; i < splittedTypes.length; i++)
    			traitToType.put(splittedTypes[i], i);
    		for (int i = 0; i < splittedTypes.length; i++)
    			reverseTraitToType.put(i, splittedTypes[i]);    		
    	} else if (typeTraitInput.get()!=null){
    		traitToType = new HashMap<>();
    		reverseTraitToType = new HashMap<>();
    		List<String> taxa = typeTraitInput.get().taxaInput.get().asStringList();
    		ArrayList<String> unique = new ArrayList<>();
    		for (int i = 0; i < taxa.size(); i++)
    			unique.add(typeTraitInput.get().getStringValue(taxa.get(i)));
    		
    		Collections.sort(unique);
    		for (int i = unique.size()-2; i > -1; i--)
    			if(unique.get(i+1).equals(unique.get(i)))
    				unique.remove(i+1);
    		  
    		for (int i = 0; i < unique.size(); i++)
    			traitToType.put(unique.get(i), i);
    		for (int i = 0; i < unique.size(); i++)
    			reverseTraitToType.put(i, unique.get(i));

    	}
    	
    	if (dimensionInput.get()>1 && dimensionInput.get()<traitToType.size())
            throw new IllegalArgumentException("dimension is not -1 (undefined) and smaller " +
            		"than the number of different traits");    	
    	// if there are rate shifts as an input, use the stepwise glm model otherwise the constant
    	if (rateShiftsInput.get() != null){
	    	intTimes = new double[(int) rateShiftsInput.get().getDimension()];
	    	intTimes[0] = rateShiftsInput.get().getArrayValue(0);
	    	for (int i = 1; i < rateShiftsInput.get().getDimension(); i++){
	    		if (rateShiftsInput.get().getArrayValue(i-1)>=0){
	    			intTimes[i] = rateShiftsInput.get().getArrayValue(i) - rateShiftsInput.get().getArrayValue(i-1); 
	    		}else{
	    			intTimes[i] = rateShiftsInput.get().getArrayValue(i);
	    		}
	    			
	    	}
	    }else{
	    	intTimes = new double[1];
	    	intTimes[0] = Double.POSITIVE_INFINITY;
    	}
    	
    	// check which rateshiftInput is the first above 0
    	firstlargerzero = intTimes.length-1;
    	for (int i = 0 ; i < intTimes.length; i++){
    		if (intTimes[i] >  0){
    			firstlargerzero = i;
				break;
    		}
    	}
    	
    	String[] splittedTypes = typesInput.get().split("\\s+");

    	// check which rateshiftInput is the first above 0
    	firstlargerzero = intTimes.length-1;
    	for (int i = 0 ; i < intTimes.length; i++){
    		if (intTimes[i] >  0){
    			firstlargerzero = i;
				break;
    		}
    	}
    	
		dimensionInput.set(splittedTypes.length);
    	
		traitToType = new HashMap<>();
		reverseTraitToType = new HashMap<>();
		for (int i = 0; i < splittedTypes.length; i++)
			traitToType.put(splittedTypes[i], i);
		for (int i = 0; i < splittedTypes.length; i++)
			reverseTraitToType.put(i, splittedTypes[i]);
		
		// set the number of intervals for the GLM models
		migrationGLMInput.get().setNrIntervals(rateShiftsInput.get().getDimension());
		NeGLMInput.get().setNrIntervals(rateShiftsInput.get().getDimension());
    }

    /**
     * Returns the time to the next interval.
     */
    public double getInterval(int i) {
    	if (i >= rateShiftsInput.get().getDimension()-firstlargerzero){
     		return Double.POSITIVE_INFINITY;
     	}else{
			return intTimes[i+firstlargerzero];
     	}
    }   

    public double[] getIntervals() {
    	return intTimes;
    }
    
    public boolean intervalIsDirty(int i){
		if(NeGLMInput.get().isDirty())
			return true;
		if(migrationGLMInput.get().isDirty())
			return true;
    	return false;
    }  
    

    
    public double[] getCoalescentRate(int i){
		int intervalNr;
    	if (i >= rateShiftsInput.get().getDimension()-firstlargerzero)
    		intervalNr = rateShiftsInput.get().getDimension()-1;
    	else
    		intervalNr = i + firstlargerzero;

    	double[] Ne = NeGLMInput.get().getRates(intervalNr);
		double[] coal = new double[Ne.length];
		for (int j = 0; j < Ne.length; j++){
			coal[j] = FastMath.min(1/Ne[j],maxRateInput.get());
		}
		return coal;
    }
    
    
    /** returns GLM matrix for interval i 
     * with all zero diagonal 
     * **/
    public double [] getRateMatrix(int i){
		int intervalNr;
    	if (i >= rateShiftsInput.get().getDimension()-firstlargerzero)
    		intervalNr = rateShiftsInput.get().getDimension()-1;
    	else
    		intervalNr = i + firstlargerzero;

    	int n = dimensionInput.get();
    	double[] m = new double[n * (n-1)];
		double[] mig = migrationGLMInput.get().getRates(intervalNr);
		double[] Ne = NeGLMInput.get().getRates(intervalNr);
		
		int c = 0;
		for (int a = 0; a < dimensionInput.get(); a++){
			for (int b = 0; b < dimensionInput.get(); b++){
				if (a!=b){
					m[c] = FastMath.min( 
							Ne[a]*mig[c]/Ne[b],
							maxRateInput.get());
					c++;
				}
			}
		}
		return m;
    }
	
	public Double[] getAllCoalescentRate() {
		Double[] coal = new Double[NeGLMInput.get().nrIntervals*NeGLMInput.get().verticalEntries];
		
		for (int i = 0; i < intTimes.length; i++){
	    	double[] Ne = NeGLMInput.get().getRates(i);
	    	for (int j = 0; j < Ne.length; j++)
	    		coal[i*NeGLMInput.get().verticalEntries + j] = 1/Ne[j];
		}
		return coal;
	}

	public Double[] getAllBackwardsMigration() {
		Double[] mig = new Double[migrationGLMInput.get().nrIntervals*migrationGLMInput.get().verticalEntries];
		
		for (int i = 0; i < intTimes.length; i++){
	    	double[] m = migrationGLMInput.get().getRates(i);
	    	for (int j = 0; j < m.length; j++)
	    		mig[i*migrationGLMInput.get().verticalEntries + j] = m[j];
		}
		return mig;
	}

	@Override
	public void init(PrintStream out) {
		for (int j = 0; j < dimensionInput.get(); j++){
			for (int i = 0; i < intTimes.length; i++){
				out.print(String.format("Ne.%d.%d\t", j,i));
			}			
		}
	}

	@Override
	public void log(long sample, PrintStream out) {
		for (int j = 0; j < dimensionInput.get(); j++){
			for (int i = 0; i < intTimes.length; i++){
		    	double[] Ne = NeGLMInput.get().getRates(i);
				out.print(Ne[j] + "\t");
			}			
		}
	}

	@Override
	public void close(PrintStream out) {
		// TODO Auto-generated method stub
		
	}

//    @Override
//	protected boolean requiresRecalculation(){
//    	
//    	return intervalIsDirty(0);
//    }


    public int getEpochCount() {
    	return rateShiftsInput.get().getDimension();
    }
}