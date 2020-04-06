package beast.evolution.substitutionmodel;

import java.io.PrintStream;

import beast.core.Input;

public class LogLinear extends GlmModel {

	private int verticalEntries;
	
	@Override
	public void initAndValidate() {
		verticalEntries = covariatesInput.get().get(0).getDimension();
		// set the dimension of the scalers, indicators and potentially the error term
    	scalerInput.get().setDimension(covariatesInput.get().size());
    	indicatorInput.get().setDimension(covariatesInput.get().size());
    	
    	if (errorInput.get()!=null)
    		errorInput.get().setDimension(covariatesInput.get().get(0).getDimension());
    	
    	if (constantErrorInput.get()!=null)
    		if (constantErrorInput.get().getDimension()<1)
    			constantErrorInput.get().setDimension(verticalEntries);
	}
	

	@Override
	public double[] getRates() {
    	double[] logrates = new double[verticalEntries];
    	
    	for (int j = 0; j < logrates.length; j++)
    		logrates[j] = 0;
    	    	
		for (int j = 0; j < covariatesInput.get().size(); j++){
			if (indicatorInput.get().getArrayValue(j) > 0.0){
				for (int k = 0; k < logrates.length; k++){
					logrates[k] += scalerInput.get().getArrayValue(j)
						*covariatesInput.get().get(j).getArrayValue(k);
				}
			}
		}
		
    	if (errorInput.get()!=null)
    		for (int k = 0; k < logrates.length; k++)
    			logrates[k] += errorInput.get().getArrayValue(k);
    	
    	if (constantErrorInput.get()!=null)
    		for (int k = 0; k < logrates.length; k++)
    			logrates[k] += constantErrorInput.get().getArrayValue( k);

    	double[] rates = new double[verticalEntries];
   	
		for (int k = 0; k < verticalEntries; k++){
			rates[k] = clockInput.get().getArrayValue()*Math.exp(logrates[k]);				
		}

		return rates;
	}

	@Override
	public void init(PrintStream out) {
		for (int i = 0 ; i < scalerInput.get().getDimension(); i++){
			out.print(String.format("%sscaler.%s\t", getID(), covariatesInput.get().get(i).getID()));
		}
	}

	@Override
	public void log(int sample, PrintStream out) {
		for (int i = 0 ; i < scalerInput.get().getDimension(); i++){
			if (indicatorInput.get().getArrayValue(i) > 0.5)
				out.print(scalerInput.get().getArrayValue(i) +"\t");
			else
				out.print("0.0\t");
		}
		
	}

	@Override
	public void close(PrintStream out) {
	}

}
