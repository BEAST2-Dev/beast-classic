/*
 * AnyTipObservationProcess.java
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

import beast.core.Description;
import beast.core.parameter.RealParameter;


//import beast.core.*;
//import dr.evolution.alignment.PatternList;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import dr.inference.model.Parameter;


@Description("Observation process for Multi-State Stochastic Dollo model. Defines a data collection process where the traits must be present in at least one tip node.")
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