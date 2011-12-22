package beast.evolution.MSSD;



import beast.core.*;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.LikelihoodCore;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Tree;
//import dr.evolution.alignment.PatternList;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import beast.evolution.likelihood.LikelihoodCore;
//import dr.inference.model.Parameter;


@Description("...")
class AbstractObservationProcess extends CalculationNode {
    Input<String> Name = new Input<String>("Name", "description here");
    Input<Tree> treeModel = new Input<Tree>("treeModel", "description here");
    Input<Alignment> patterns = new Input<Alignment>("patterns", "description here");
    Input<SiteModel> siteModel = new Input<SiteModel>("siteModel", "description here");
    Input<BranchRateModel> branchRateModel = new Input<BranchRateModel>("branchRateModel", "description here");
    Input<RealParameter> mu = new Input<RealParameter>("mu", "description here");
    Input<RealParameter> lam = new Input<RealParameter>("lam", "description here");



    dr.evomodel.MSSD.AbstractObservationProcess abstractobservationprocess;

    public void store() {
        abstractobservationprocess.store();
     }
//    void setNodePatternInclusion() {
//        abstractobservationprocess.setNodePatternInclusion();
//     }
//    double calculateSiteLogLikelihood(int arg0, double [] arg1, double [] arg2) {
//        return abstractobservationprocess.calculateSiteLogLikelihood(arg0, arg1, arg2);
//     }
//    void calculateNodePatternLikelihood(int arg0, double [] arg1, LikelihoodCore arg2, double arg3, double [] arg4) {
//        abstractobservationprocess.calculateNodePatternLikelihood(arg0, arg1, arg2, arg3, arg4);
//     }
    double getNodeSurvivalProbability(int index, double averageRate) {
        return abstractobservationprocess.getNodeSurvivalProbability(index, averageRate);
     }
//    double accumulateCorrectedLikelihoods(double [] arg0, double arg1, double [] arg2) {
//        return abstractobservationprocess.accumulateCorrectedLikelihoods(arg0, arg1, arg2);
//     }
    double nodePatternLikelihood(double [] arg0, LikelihoodCore arg1) {
        return abstractobservationprocess.nodePatternLikelihood(arg0, arg1);
     }
    double getAverageRate() {
        return abstractobservationprocess.getAverageRate();
     }
//    double getAscertainmentCorrection(double [] arg0) {
//        return abstractobservationprocess.getAscertainmentCorrection(arg0);
//     }
    double getLogTreeWeight() {
        return abstractobservationprocess.getLogTreeWeight();
     }
    double calculateLogTreeWeight() {
        return abstractobservationprocess.calculateLogTreeWeight();
     }
//    void handleModelChangedEvent(Model model, Object object, int index) {
//        abstractobservationprocess.handleModelChangedEvent(model, object, index);
//     }
//    void handleVariableChangedEvent(Variable arg0, int arg1, ChangeType arg2) {
//        abstractobservationprocess.handleVariableChangedEvent(arg0, arg1, arg2);
//     }
    public void restore() {
        abstractobservationprocess.restore();
     }
//    void accept() {
//        abstractobservationprocess.acceptState();
//     }
    void setIntegrateGainRate(boolean integrateGainRate) {
        abstractobservationprocess.setIntegrateGainRate(integrateGainRate);
     }

}