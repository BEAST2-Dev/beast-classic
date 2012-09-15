package beast.evolution.datatype;

import java.util.List;

import beast.core.Description;
import beast.core.Plugin;

@Description("Datatype for capturing continuous data, such as geographic locations")
public class ContinuousDataType extends DataType.Base {
		
	/* return the dimension of the data type */
	public int getDimension() {
		return 0;
	}
	
	@Override
	public int getStateCount() {
		return 0;
	}

	@Override
	public List<Integer> string2state(String sSequence) throws Exception {
		return null;
	}

	@Override
	public String state2string(List<Integer> nStates) {
		return null;
	}

	@Override
	public String state2string(int[] nStates) {
		return null;
	}

	@Override
	public boolean[] getStateSet(int iState) {
		return null;
	}

	@Override
	public int[] getStatesForCode(int iState) {
		return null;
	}

	@Override
	public boolean isAmbiguousState(int state) {
		return false;
	}

	@Override
	public boolean isStandard() {
		return false;
	}

	@Override
	public char getChar(int state) {
		return 0;
	}

	@Override
	public String getCode(int state) {
		return null;
	}

	/** by default assume comma separated list of values **/
	public double[] string2values(String sValue) throws Exception {
		String [] strs = sValue.trim().split(",");
		double [] values = new double [strs.length];
		for (int i = 0; i < strs.length; i++) {
			try {
			values[i] = Double.parseDouble(strs[i].trim());
			} catch (NumberFormatException e) {
				throw new Exception("String is not a comma separated list of numbers");
			}
		}
		return values;
	}

}
