package beast.geo;



import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import beast.core.*;
import beast.core.Input.Validate;


import dr.math.distributions.MultivariateDistribution;


@Description("Distribution over a geographical area")
public class GeoSpatialDistribution extends CalculationNode implements MultivariateDistribution {
    public Input<String> labelInput = new Input<String>("label", "identifies nodes this distribution applies to -- " +
    		"can be taxon names, '" + ALL_NODES + "' for all nodes, 'root' for root node only");
    public Input<String> fileInput = new Input<String>("kmlfile","name of the kml file specifying a set of regions", Validate.REQUIRED);
//    public Input<List<Polygon2D>> regionInput = new Input<List<Polygon2D>>("region", "2 dimensional regions describing an area", new ArrayList<Polygon2D>());
    public Input<Boolean> insideInput = new Input<Boolean>("inside", "whether the area inside regions is allowed", true);
    public Input<Boolean> unionInput = new Input<Boolean>("union", "whether the union instead of intersection of regions is allowed", true);

    protected List<Polygon2D> regions;
    protected String label = null;
    private boolean outside = false;
    private boolean union = false;

    public static final String TYPE = "geoSpatial";
    public static final String ALL_NODES= "all";

    @Override
    public void initAndValidate() {
    	label = labelInput.get();
    	try {
			regions = Polygon2D.readKMLFile(fileInput.get());
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}
    	outside = !insideInput.get();
    	union = unionInput.get();
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean getOutside() {
        return outside;
    }
    
    public List<Polygon2D> getRegion() {
        return regions;
    }

    // MultivariateDistribution interface implementation
    @Override
    public double logPdf(double [] x) {
        boolean contains = false;
        if (union) {
        	contains = true;
	        for (Polygon2D region : regions) {
	        	contains = contains && region.containsPoint2D(new Point2D.Double(x[0], x[1]));  
	        }
        } else {
	        for (Polygon2D region : regions) {
	        	contains = contains || region.containsPoint2D(new Point2D.Double(x[0], x[1]));  
	        }
        }
        if (outside ^ contains)
            return 0;
        return Double.NEGATIVE_INFINITY;
    }
    
    
    @Override
    public double [][] getScaleMatrix() {
        return null;
    }
    
    @Override
    public double [] getMean() {
        return null;
    }
    
    @Override
    public String getType() {
        return TYPE;
    }

    
}
