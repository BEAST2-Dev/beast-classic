package beast.evolution.MSSD;

import beast.core.parameter.RealParameter;



//import beast.core.*;
//import dr.evolution.alignment.PatternList;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import dr.inference.model.Parameter;


//@Description("...")
public class AnyTipObservationProcess extends AbstractObservationProcess {
 


    dr.evomodel.MSSD.AnyTipObservationProcess anytipobservationprocess;


    @Override
    public void initAndValidate() throws Exception {
        anytipobservationprocess = new dr.evomodel.MSSD.AnyTipObservationProcess(
        		             "AnyTip",
                             treeModel.get(),
                             patterns.get(),
                             siteModel.get(),
                             branchRateModel.get(),
                             mu.get(),
                             (lam.get() == null ? new RealParameter("1.0") : lam.get()),
                             integrateGainRateInput.get());
        abstractobservationprocess = anytipobservationprocess;
    }


    double calculateLogTreeWeight() {
        return anytipobservationprocess.calculateLogTreeWeight();
     }
    void setTipNodePatternInclusion() {
        anytipobservationprocess.setTipNodePatternInclusion();
     }
    void setNodePatternInclusion() {
       anytipobservationprocess.setNodePatternInclusion();
     }

}