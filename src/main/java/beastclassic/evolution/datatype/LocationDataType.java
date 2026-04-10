package beastclassic.evolution.datatype;

import beast.base.core.Description;

@Description("Datatype for representing geographic locations in 2 dimension, latitude x longitude")
public class LocationDataType extends ContinuousDataType {
	
	@Override
	public void initAndValidate() {
		// nothing to do
	}
	
	@Override
	public int getDimension() {
		return 2;
	}
	
	@Override
	public double[] string2values(String sValue) throws Exception {
		String [] strs = sValue.trim().split(",");
		if (strs.length != 2) {
			throw new Exception ("Expected 2 comma separated numbers, not " + strs.length);
		}
		return super.string2values(sValue);
	}


}
