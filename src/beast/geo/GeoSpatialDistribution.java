package beast.geo;


import beast.core.*;
import dr.geo.Polygon2D;


@Description("...")
public class GeoSpatialDistribution extends Plugin {
    public Input<String> label = new Input<String>("label", "description here");
    public Input<Polygon2D> region = new Input<Polygon2D>("region", "description here");
    public Input<Boolean> inside = new Input<Boolean>("inside", "description here", true);

    dr.geo.GeoSpatialDistribution geospatialdistribution;

    @Override
    public void initAndValidate() throws Exception {
        geospatialdistribution = new dr.geo.GeoSpatialDistribution(
                             label.get(),
                             region.get(),
                             inside.get());
    }


    public String getType() {
        return geospatialdistribution.getType();
     }
    public double logPdf(double [] arg0) {
        return geospatialdistribution.logPdf(arg0);
     }
    public double [][] getScaleMatrix() {
        return geospatialdistribution.getScaleMatrix();
     }
    public double [] getMean() {
        return geospatialdistribution.getMean();
     }
    public String getLabel() {
        return geospatialdistribution.getLabel();
     }
    public boolean getOutside() {
        return geospatialdistribution.getOutside();
     }
    public Polygon2D getRegion() {
        return geospatialdistribution.getRegion();
     }

}
