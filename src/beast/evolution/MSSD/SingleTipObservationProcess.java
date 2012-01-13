/*
 * SingleTipObservationProcess.java
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

import beast.evolution.sitemodel.SiteModel;
import beast.evolution.alignment.Taxon;
import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.RealParameter;

/**
 * Package: SingleTimeObservationProcess
 * Description:
 * <p/>
 * <p/>
 * Created by
 *
 * @author Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 *         Date: Jan 13, 2012
 *         Time: 11:32:28 AM
 */
@Description("Observation process for Multi-State Stochastic Dollo model. Defines a data collection process where the traits must be present in a specific tip node.")
public class SingleTipObservationProcess extends AnyTipObservationProcess{

    public Input<Taxon> theTip = new Input<Taxon>("taxon", "A taxon in which the traits must be present", Input.Validate.REQUIRED);

    dr.evomodel.MSSD.AnyTipObservationProcess singletipobservationprocess;

    public void initAndValidate() throws Exception {
        singletipobservationprocess = new dr.evomodel.MSSD.SingleTipObservationProcess(treeModel.get(),
                patterns.get(),
                siteModel.get(),
                branchRateModel.get(),
                mu.get(),
                (lam.get() == null ? new RealParameter("1.0") : lam.get()),
                theTip.get(),
                integrateGainRateInput.get());
        anytipobservationprocess = singletipobservationprocess;
    }

    public double calculateLogTreeWeight() {
        return singletipobservationprocess.calculateLogTreeWeight();
    }
}
