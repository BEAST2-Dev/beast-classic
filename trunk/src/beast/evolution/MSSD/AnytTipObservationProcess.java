package beast.evolution.MSSD;



//import beast.core.*;
//import dr.evolution.alignment.PatternList;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import dr.inference.model.Parameter;


//@Description("...")
class AnyTipObservationProcess extends AbstractObservationProcess {
 


    dr.evomodel.MSSD.AnyTipObservationProcess anytipobservationprocess;


    @Override
    public void initAndValidate() throws Exception {
        anytipobservationprocess = new dr.evomodel.MSSD.AnyTipObservationProcess(
        		             Name.get(),
                             treeModel.get(),
                             patterns.get(),
                             siteModel.get(),
                             branchRateModel.get(),
                             mu.get(),
                             lam.get());
        abstractobservationprocess = anytipobservationprocess;
    }


    double calculateLogTreeWeight() {
        return anytipobservationprocess.calculateLogTreeWeight();
     }
    void setTipNodePatternInclusion() {
 //       anytipobservationprocess.setTipNodePatternInclusion();
     }
    void setNodePatternInclusion() {
 //       anytipobservationprocess.setNodePatternInclusion();
     }

}