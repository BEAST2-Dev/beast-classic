package beast.geo;


import beast.core.*;
import beast.core.parameter.RealParameter;
import java.util.List;
import java.util.Random;


@Description("...")
public class GeoSpatialCollectionModel extends Distribution {
//    public Input<String> name = new Input<String>("name", "description here");
    public Input<RealParameter> pointsInput = new Input<RealParameter>("points", "description here");
    public Input<List<beast.geo.GeoSpatialDistribution>> geoSpatialDistributions = new Input<List<GeoSpatialDistribution>>("geoSpatialDistributions", "description here");
    public Input<Boolean> isIntersection = new Input<Boolean>("isIntersection", "description here");

    dr.geo.GeoSpatialCollectionModel geospatialcollectionmodel;

    RealParameter points;
    
    @Override
    public void initAndValidate() throws Exception {
    	points = pointsInput.get();
        geospatialcollectionmodel = new dr.geo.GeoSpatialCollectionModel(
        		points, 
        		geoSpatialDistributions.get(), 
        		isIntersection.get());
    }


    @Override
    public void store() {
        geospatialcollectionmodel.storeState();
     }

    @Override
    public void restore() {
        geospatialcollectionmodel.restoreState();
     }
    
    @Override
   	protected boolean requiresRecalculation() {
    	for (int i = 0; i < points.getDimension(); i++) {
    		if (points.isDirty(i)) {
    	    	geospatialcollectionmodel.handleVariableChangedEvent(i);    			
    		}
    	}
    	return true;
   	}
    
    @Override
    public double calculateLogP() throws Exception {
        return geospatialcollectionmodel.getLogLikelihood();
    }
    
    void makeDirty() {
        geospatialcollectionmodel.makeDirty();
     }
    RealParameter getParameter() {
        return geospatialcollectionmodel.getParameter();
     }


	@Override
	public List<String> getArguments() {return null;}
	@Override
	public List<String> getConditions() {return null;}
	@Override
	public void sample(State state, Random random) {}

}

