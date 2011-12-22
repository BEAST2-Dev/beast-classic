package dr.evomodel.MSSD;

import beast.core.*;
import beast.evolution.likelihood.TreeLikelihood;

@Description("Treelikelihood for running the stochastic dollo process")
public class ALSTreeLikelihood extends TreeLikelihood {
	public Input<AbstractObservationProcess> op = new Input<AbstractObservationProcess>("observationprocess","description here");
	
    protected AbstractObservationProcess observationProcess;

    @Override
    public void initAndValidate() throws Exception {
    	observationProcess = op.get();
        // ensure TreeLikelihood initialises the partials for tips
    	m_useAmbiguities.setValue(true, this);
    	super.initAndValidate();
    }
 
    @Override
    public double calculateLogP() throws Exception {
        // Calculate the partial likelihoods
    	super.calculateLogP();
        // get the frequency model
    	double [] freqs = m_pSiteModel.get().m_pSubstModel.get().getFrequencies();
        // let the observationProcess handle the rest
        logP = observationProcess.nodePatternLikelihood(freqs, m_likelihoodCore);
        return logP;
    }
	
}
