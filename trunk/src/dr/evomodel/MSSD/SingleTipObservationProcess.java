package dr.evomodel.MSSD;

import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Tree;


//import dr.evolution.alignment.PatternList;
//import dr.evolution.util.Taxon;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import dr.evomodelxml.MSSD.SingleTipObservationProcessParser;
//import dr.inference.model.Parameter;

/**
 * Package: SingleTipObservationProcess
 * Description:
 * <p/>
 * <p/>
 * Created by
 * Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 * Date: Feb 19, 2008
 * Time: 2:57:14 PM
 */
public class SingleTipObservationProcess extends AnyTipObservationProcess {
    protected Taxon sourceTaxon;

    public SingleTipObservationProcess(Tree treeModel, Alignment patterns, SiteModel siteModel,
                                       BranchRateModel branchRateModel, RealParameter mu, RealParameter lam, Taxon sourceTaxon) {
        super("SingleTipObservationProcess", treeModel, patterns, siteModel, branchRateModel, mu, lam);
        this.sourceTaxon = sourceTaxon;
    }

    public double calculateLogTreeWeight() {
        return -lam.getValue(0) / (getAverageRate() * mu.getValue(0));
    }

}
