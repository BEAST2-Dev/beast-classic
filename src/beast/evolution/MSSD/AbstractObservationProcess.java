/*
 * AbstractObservationProcess.java
 *
 * Copyright (C) 2002-2012 Alexei Drummond,
 * Andrew Rambaut, Marc Suchard and Alexander V. Alekseyenko
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.evolution.MSSD;


import beast.core.*;
import beast.core.Input.Validate;
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


@Description("Abstract Observation Process defines how the integration of gain events is done along the tree."+
        "Specific instances should define how the data is collected."+
        "Alekseyenko, AV., Lee, CJ., Suchard, MA. Wagner and Dollo: a stochastic duet" +
        "by composing two parsimonious solos. Systematic Biology 2008 57(5): 772 - 784; doi:" +
        "10.1080/10635150802434394. PMID: 18853363")
public class AbstractObservationProcess extends CalculationNode {
    //    Input<String> Name = new Input<String>("Name", "description here");
    public Input<Tree> treeModel = new Input<Tree>("tree", "description here", Validate.REQUIRED);
    public Input<Alignment> patterns = new Input<Alignment>("data", "description here", Validate.REQUIRED);
    public Input<SiteModel> siteModel = new Input<SiteModel>("siteModel", "description here", Validate.REQUIRED);
    public Input<BranchRateModel> branchRateModel = new Input<BranchRateModel>("branchRateModel", "description here");
    public Input<RealParameter> mu = new Input<RealParameter>("mu", "description here", Validate.REQUIRED);
    public Input<RealParameter> lam = new Input<RealParameter>("lam", "description here");
    public Input<Boolean> integrateGainRateInput = new Input<Boolean>("integrateGainRate", "description here", false);


    dr.evomodel.MSSD.AbstractObservationProcess abstractobservationprocess;

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
    double nodePatternLikelihood(double[] arg0, PartialsProvider arg1) {
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
    @Override
    public void store() {
        abstractobservationprocess.store();
    }

    @Override
    public void restore() {
        abstractobservationprocess.restore();
    }

    @Override
    protected boolean requiresRecalculation() {
        return abstractobservationprocess.requiresRecalculation();
    }

    public void re() {
        abstractobservationprocess.store();
    }
//    void accept() {
//        abstractobservationprocess.acceptState();

    //     }
    void setIntegrateGainRate(boolean integrateGainRate) {
        abstractobservationprocess.setIntegrateGainRate(integrateGainRate);
    }

}