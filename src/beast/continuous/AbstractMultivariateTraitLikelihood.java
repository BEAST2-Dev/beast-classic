package beast.continuous;






import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.AlignmentFromTraitMap;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.datatype.ContinuousDataType;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.ContinuousSubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import beast.evolution.tree.TreeTrait;
import beast.evolution.tree.TreeTraitMap;


/**
 * @author Marc Suchard
 */
@Description("AbstractMultivariateTraitLikelihood ported from BEAST1")
public abstract class AbstractMultivariateTraitLikelihood extends GenericTreeLikelihood implements Loggable
//        implements TreeTraitProvider, Citable 
{
    //public Input<Tree> treeModelInput = new Input<Tree>("tree","", Validate.REQUIRED);
    //public Input<MultivariateDiffusionModel> diffusionModelInput = new Input<MultivariateDiffusionModel>("diffusionmodel", "", Validate.REQUIRED);
    public Input<RealParameter> traitParameterInput = new Input<RealParameter>("traitParameter", "", Validate.REQUIRED);
    //public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);
    
    public Input<RealParameter> deltaParameterInput = new Input<RealParameter>("deltaParameter", "");
    public Input<List<Integer>> missingIndicesInput = new Input<List<Integer>>("missingIndices", "");
    public Input<Boolean> cacheBranchesInput = new Input<Boolean>("cacheBranches", "", false);
    public Input<Boolean> scaleByTimeInput = new Input<Boolean>("scaleByTime", "", false);
    public Input<Boolean> integrateInput = new Input<Boolean>("integrate", "", false);
    public Input<Boolean> useTreeLengthInput = new Input<Boolean>("useTreeLength", "", false);
    //public Input<BranchRateModel.Base> rateModelInput = new Input<BranchRateModel.Base>("branchratemodel","");
    //public Input<Model> samplingDensityInput = new Input
    public Input<Boolean> reportAsMultivariateInput = new Input<Boolean>("reportAsMultivariate", "", true);
    public Input<Boolean> reciprocalRatesInput = new Input<Boolean>("reciprocalRates", "" , false);


    protected List<BranchRateModel> optimalValues = null;

    public static final String TRAIT_LIKELIHOOD = "multivariateTraitLikelihood";
    public static final String CONJUGATE_ROOT_PRIOR = "conjugateRootPrior";
    public static final String MODEL = "diffusionModel";
    public static final String TREE = "tree";
    public static final String CACHE_BRANCHES = "cacheBranches";
    public static final String REPORT_MULTIVARIATE = "reportAsMultivariate";
    public static final String CHECK = "check";
    public static final String USE_TREE_LENGTH = "useTreeLength";
    public static final String SCALE_BY_TIME = "scaleByTime";
    public static final String SUBSTITUTIONS = "substitutions";
    public static final String SAMPLING_DENSITY = "samplingDensity";
    public static final String INTEGRATE = "integrateInternalTraits";
    public static final String RECIPROCAL_RATES = "reciprocalRates";
    public static final String PRIOR_SAMPLE_SIZE = "priorSampleSize";
    public static final String RANDOM_SAMPLE = "randomSample";
    public static final String IGNORE_PHYLOGENY = "ignorePhylogeny";
    public static final String ASCERTAINMENT = "ascertainedTaxon";
    public static final String EXCHANGEABLE_TIPS = "exchangeableTips";

    TreeTraitMap traitMap;
    protected double[] m_branchLengths;
    protected double[] m_StoredBranchLengths;
    
    @Override
    public void initAndValidate() {
        //super.initAndValidate();
        m_branchLengths = new double[0];
        m_StoredBranchLengths = new double[0];

        this.traitName = traitName;
        this.treeModel = treeInput.get();
        this.rateModel = branchRateModelInput.get();
        this.diffusionModel = (ContinuousSubstitutionModel) 
        			((SiteModel.Base) siteModelInput.get()).getSubstitutionModel(); 
        		//diffusionModelInput.get();
        this.traitParameter = traitParameterInput.get();        
        this.useTreeLength = useTreeLengthInput.get();

        this.missingIndices = missingIndicesInput.get();

        this.deltaParameter = deltaParameterInput.get();

        if (rateModel != null) {
            hasRateModel = true;
        }


        this.reportAsMultivariate = reportAsMultivariateInput.get();

        this.cacheBranches = cacheBranchesInput.get();
        if (cacheBranches) {
            cachedLogLikelihoods = new double[treeModel.getNodeCount()];
            storedCachedLogLikelihood = new double[treeModel.getNodeCount()];
            validLogLikelihoods = new boolean[treeModel.getNodeCount()];
            storedValidLogLikelihoods = new boolean[treeModel.getNodeCount()];
        }

        this.scaleByTime = scaleByTimeInput.get();;
        this.useTreeLength = useTreeLengthInput.get();;
        this.reciprocalRates = reciprocalRatesInput.get();;

        //dimTrait = diffusionModel.getPrecisionmatrix().length;
        dimTrait = ((ContinuousDataType) dataInput.get().getDataType()).getDimension();
        dim = traitParameter != null ? traitParameter.getMinorDimension1() : 0;
        numData = dim / dimTrait;

        if (dim % dimTrait != 0)
            throw new RuntimeException("dim is not divisible by dimTrait");

        //traitMap = mapInput.get();
        if (dataInput.get() instanceof AlignmentFromTraitMap) {
        	traitMap = ((AlignmentFromTraitMap) dataInput.get()).getTraitMap();
        } else {
        	throw new IllegalArgumentException ("Expected that data input is AlignmentFromTraitMap");
        }
        recalculateTreeLength();                           
//        printInformtion();

    }

//    protected void printInformtion() {
//        StringBuffer sb = new StringBuffer("Creating multivariate diffusion model:\n");
//        sb.append("\tTrait: ").append(traitName).append("\n");
//        sb.append("\tDiffusion process: ").append(diffusionModel.getId()).append("\n");
//        sb.append("\tHeterogenity model: ").append(rateModel != null ? rateModel.getId() : "homogeneous").append("\n");
//        sb.append("\tTree normalization: ").append(scaleByTime ? (useTreeLength ? "length" : "height") : "off").append("\n");
//        sb.append("\tUsing reciprocal (precision) rates: ").append(reciprocalRates).append("\n");
//        if (scaleByTime) {
//            recalculateTreeLength();
//            if (useTreeLength) {
//                sb.append("\tInitial tree length: ").append(treeLength).append("\n");
//            } else {
//                sb.append("\tInitial tree height: ").append(treeLength).append("\n");
//            }
//        }
//        sb.append(extraInfo());
//        sb.append("\tPlease cite:\n");
//        sb.append(Citable.Utils.getCitationString(this));
//
//
//        sb.append("\n\tDiffusion dimension   : ").append(dimTrait).append("\n");
//        sb.append(  "\tNumber of observations: ").append(numData).append("\n");
//        Logger.getLogger("dr.evomodel").info(sb.toString());
//    }

//    private static Citable TraitAscertainmentCitation = new Citable() {//} implements Citable {
//
//        public List<Citation> getCitations() {
//            List<Citation> list = new ArrayList<Citation>();
//            list.add(
//                    new Citation(
//                            new Author[]{
//                                    new Author("MA", "Suchard"),
//                                    new Author("J", "Novembre"),
//                                    new Author("B", "von Holdt"),
//                                    new Author("G", "Cybis"),
//                            },
//                            Citation.Status.IN_PREPARATION
//                    )
//            );
//            return list;
//        }
//    };

//    public List<Citation> getCitations() {
//        List<Citation> citations = new ArrayList<Citation>();
//        citations.add(
//                CommonCitations.LEMEY_2010
//        );
//        return citations;
//    }

    protected abstract String extraInfo();

    public RealParameter getTraitParameter() {
        return traitParameter;
    }

//    public void setAscertainedTaxon(Taxon taxon) {
//        ascertainedTaxonIndex = treeModel.getTaxonIndex(taxon);
//        if (ascertainedTaxonIndex == -1) {
//            throw new RuntimeException("Taxon " + taxon.getID() + " is not in tree " + treeModel.getID());
//        }
//        doAscertainmentCorrect = true;
////        StringBuilder sb = new StringBuilder("Enabling ascertainment correction for multivariate trait model: ");
////        sb.append(getId()).append("\n");
////        sb.append("\tTaxon: ").append(taxon.getId()).append("\n");
////        sb.append("\tPlease cite:\n");
////        sb.append(Citable.Utils.getCitationString(TraitAscertainmentCitation));
////        Logger.getLogger("dr.evomodel").info(sb.toString());
//    }

    public double getRescaledBranchLength(Node node) {

        double length = node.getLength();

        if (hasRateModel) {
            if (reciprocalRates) {
                length /= rateModel.getRateForBranch(node); // branch rate scales as precision (inv-time)
            } else {
                length *= rateModel.getRateForBranch(node); // branch rate scales as variance (time)
            }
        }

        if (scaleByTime) {
            length /= treeLength;
        }

        if (deltaParameter != null && node.isLeaf()) {
            length += deltaParameter.getValue(0);
        }

        return length;
    }

    // **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************

//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//
//        if (!cacheBranches) {
//            likelihoodKnown = false;
//            if (model == treeModel)
//                recalculateTreeLength();
//            return;
//        }
//
//        if (model == diffusionModel) {
//            updateAllNodes();
//        }
//
//        // fireTreeEvents sends two events here when a node trait is changed,
//        // ignoring object instance Parameter case
//
//        else if (model == treeModel) {
//            if (object instanceof TreeModel.TreeChangedEvent) {
//                TreeModel.TreeChangedEvent event = (TreeModel.TreeChangedEvent) object;
//                if (event.isTreeChanged()) {
//                    recalculateTreeLength();
//                    updateAllNodes();
//                } else  if (event.isHeightChanged()) {
//                    recalculateTreeLength();
//                    if (useTreeLength || (scaleByTime && treeModel.isRoot(event.getNode())))
//                        updateAllNodes();
//                    else {
//                        updateNodeAndChildren(event.getNode());
//                    }
//                } else if (event.isNodeParameterChanged()) {
//                    updateNodeAndChildren(event.getNode());
//                } else if (event.isNodeChanged()) {
//                    recalculateTreeLength();
//                    if (useTreeLength || (scaleByTime && treeModel.isRoot(event.getNode())))
//                        updateAllNodes();
//                    else {
//                        updateNodeAndChildren(event.getNode());
//                    }
//                } else {
//                    throw new RuntimeException("Unexpected TreeModel TreeChangedEvent occurring in AbstractMultivariateTraitLikelihood");
//                }
//            } else if (object instanceof RealParameter) {
//                // Ignoring
//            } else {
//                throw new RuntimeException("Unexpected object throwing events in AbstractMultivariateTraitLikelihood");
//            }
//        } else if (model == rateModel) {
//            if (index == -1) {
//                updateAllNodes();
//            } else {
//                if (object == null || ((RealParameter) object).getDimension() == 2 * (treeModel.getNodeCount() - 1))
//                    updateNode(treeModel.getNode(index)); // This is a branch specific update
//                else
//                    updateAllNodes(); // Probably an epoch model
//            }
//        } else {
//            throw new RuntimeException("Unknown componentChangedEvent");
//        }
//    }

    private void updateAllNodes() {
    	if (cacheBranches)
    		for (int i = 0; i < treeModel.getNodeCount(); i++)
    			validLogLikelihoods[i] = false;
        likelihoodKnown = false;
    }

    private void updateNode(Node node) {
    	if (cacheBranches)
    		validLogLikelihoods[node.getNr()] = false;
        likelihoodKnown = false;
    }

    private void updateNodeAndChildren(Node node) {
    	if (cacheBranches) {
	        validLogLikelihoods[node.getNr()] = false;
	        for (int i = 0; i < node.getChildCount(); i++)
	            validLogLikelihoods[node.getChild(i).getNr()] = false;
    	}
        likelihoodKnown = false;
    }


    public void recalculateTreeLength() {
        if (!scaleByTime)
            return;

        if (useTreeLength) {
            treeLength = 0;
            for (int i = 0; i < treeModel.getNodeCount(); i++) {
                Node node = treeModel.getNode(i);
                if (!node.isRoot())
                    treeLength += node.getLength(); // Bug was here
            }
        } else { // Normalizing by tree height.
            treeLength = treeModel.getRoot().getHeight();
        }
    }

    
    @Override
    protected boolean requiresRecalculation() {
    	
//    	if (true) {
//	    	recalculateTreeLength();
//	    	updateAllNodes();
//	        likelihoodKnown = false;
//	        return true;
//    	}
    	
    	if (diffusionModel.isDirtyCalculation() || (hasRateModel && rateModel.isDirtyCalculation())
    			|| (deltaParameter != null && deltaParameter.somethingIsDirty())) {
        	recalculateTreeLength();
	    	updateAllNodes();
            likelihoodKnown = false;
    	}
    	if (traitParameter.somethingIsDirty()) {
        	recalculateTreeLength();
    		int d = traitParameter.getMinorDimension1();
    		if (cacheBranches) {
	        	Arrays.fill(validLogLikelihoods, true);
	        	Node [] nodes = treeModel.getNodesAsArray();
	    		for (int i = 0; i < traitParameter.getMinorDimension2(); i++) {
	    			if (traitParameter.isDirty(i * 2)) {
	    				validLogLikelihoods[i] = false;
	    				for (Node child : nodes[i].getChildren()) {
	    					validLogLikelihoods[child.getNr()] = false;
	    				}
	    			}
	    		}
    		}
            likelihoodKnown = false;
    	}
    	if (treeModel.somethingIsDirty()) {
//            if (useTreeLength || (scaleByTime && treeModel.getRoot().isDirty() != Tree.IS_CLEAN)) {
            	// recalc everything
            	recalculateTreeLength();
            	updateAllNodes();
//            }
//        	Node [] nodes = treeModel.getNodesAsArray();
//        	Arrays.fill(validLogLikelihoods, true);
//        	for (int i = 0; i < nodes.length; i++) {
//        		if (nodes[i].isDirty() != Tree.IS_CLEAN) {
//                    validLogLikelihoods[nodes[i].getNr()] = false;
//    				for (Node child : nodes[i].getChildren()) {
//    					validLogLikelihoods[child.getNr()] = false;
//    				}
//        		}
//        	}
            likelihoodKnown = false;
    	}
    	return true;
    }
    // **************************************************************
    // VariableListener IMPLEMENTATION
    // **************************************************************

//    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
//        if (variable == deltaParameter) {
//            likelihoodKnown = false;
//        }
//
//        if (variable == traitParameter) {
//            likelihoodKnown = false;
//        }
//
//        // All parameter changes are handled first by the treeModel
//        if (!cacheBranches)
//            likelihoodKnown = false;
//    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    /**
     * Stores the precalculated state: in this case the intervals
     */
    @Override
    public void store() {
        storedLikelihoodKnown = likelihoodKnown;
        storedLogLikelihood = logLikelihood;
        storedTreeLength = treeLength;

        if (cacheBranches) {
            System.arraycopy(cachedLogLikelihoods, 0, storedCachedLogLikelihood, 0, treeModel.getNodeCount());
            System.arraycopy(validLogLikelihoods, 0, storedValidLogLikelihoods, 0, treeModel.getNodeCount());
        }
        super.store();
        System.arraycopy(m_branchLengths, 0, m_StoredBranchLengths, 0, m_branchLengths.length);
    }

    /**
     * Restores the precalculated state: that is the intervals of the tree.
     */
    @Override
    public void restore() {
        likelihoodKnown = storedLikelihoodKnown;
        logLikelihood = storedLogLikelihood;
        treeLength = storedTreeLength;

        if (cacheBranches) {
            double[] tmp = storedCachedLogLikelihood;
            storedCachedLogLikelihood = cachedLogLikelihoods;
            cachedLogLikelihoods = tmp;
            boolean[] tmp2 = storedValidLogLikelihoods;
            storedValidLogLikelihoods = validLogLikelihoods;
            validLogLikelihoods = tmp2;
        }
        super.restore();
        double[] tmp = m_branchLengths;
        m_branchLengths = m_StoredBranchLengths;
        m_StoredBranchLengths = tmp;
    }

    protected void acceptState() {
    } // nothing to do

    public TreeInterface getTreeModel() {
        return treeModel;
    }

    public String getTraitName() {
        return traitName;
    }

//    public MultivariateDiffusionModel getDiffusionModel() {
//        return diffusionModel;
//    }

//	public boolean getInSubstitutionTime() {
//		return inSubstitutionTime;
//	}

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

//    public Model getModel() {
//        return this;
//    }

    public String toString() {
        return getClass().getName();// + "(" + getLogLikelihood() + ")";

    }

    public final double getLogLikelihood() {
        if (!likelihoodKnown) {
            logLikelihood = calculateLogLikelihood();
            if (doAscertainmentCorrect) {
                double correction = calculateAscertainmentCorrection(ascertainedTaxonIndex);
//                System.err.println("Correction = " + correction);
                logLikelihood -= correction;
            }
            likelihoodKnown = true;
        }
        return logLikelihood;
    }
    
    @Override
    public double calculateLogP() {
    	logP = getLogLikelihood();
    	return logP;
    }
    

    protected abstract double calculateAscertainmentCorrection(int taxonIndex);

    public abstract double getLogDataLikelihood();

    public void makeDirty() {
        likelihoodKnown = false;
        if (cacheBranches)
            updateAllNodes();
    }

    public double[] getOptimalValue(Node node) {
        if (optimalValues != null) {
            final int dim = optimalValues.size();
            double[] optVals = new double[dim];
            for (int i = 0; i < dim; ++i) {
                optVals[i] = optimalValues.get(i).getRateForBranch(node);
            }
            return optVals;
        } else {
            throw new RuntimeException("getOptimalValue should not be called.");
        }
    }
    
    
//    public LogColumn[] getColumns() {
//        return new LogColumn[]{
//                new LikelihoodColumn(getId() + ".joint"),
//                new NumberColumn(getId() + ".data") {
//                    public double getDoubleValue() {
//                        return getLogDataLikelihood();
//                    }
//                }
//        };
//    }

    
    /** loggable implementation **/
    @Override
    public void init(PrintStream out) {
    	out.print(getID() + ".joint\t" + getID() + ".data\t");
    }
    
    @Override
    public void log(int nSample, PrintStream out) {
    	out.print(logP + "\t" + getLogDataLikelihood() + "\t");
    }
    
    public abstract double calculateLogLikelihood();

//    public double getMaxLogLikelihood() {
//        return maxLogLikelihood;
//    }


    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    private TreeTrait[] treeTraits = null;

    public TreeTrait[] getTreeTraits() {
        if (treeTraits == null) {
            final double[] trait = getRootNodeTrait();
            if (trait.length == 1 || reportAsMultivariate) {
                treeTraits = new TreeTrait[] {
                        new TreeTrait.DA() {
                            public String getTraitName() {
                                return traitName;
                            }

                            public Intent getIntent() {
                                return Intent.NODE;
                            }

                            public Class getTraitClass() {
                                return Double.class;
                            }

                            public double[] getTrait(TreeInterface tree, Node node) {
                                return getTraitForNode(tree, node, traitName);
                            }
                        }
                };
            } else {
                throw new RuntimeException("Reporting of traits is only supported as multivariate");
            }
        }
        return treeTraits;
    }

    public TreeTrait getTreeTrait(String key) {
        TreeTrait[] tts = getTreeTraits();
        for (TreeTrait tt : tts) {
            if (tt.getTraitName().equals(key)) {
                return tt;
            }
        }
        return null;
    }

    public final int getNumData() {
        return numData;
    }

    public final int getDimTrait() {
        return dimTrait;
    }

    protected double[] getRootNodeTrait() {
    	return traitMap.getTrait(treeModel, treeModel.getRoot());
    }

    public abstract double[] getTraitForNode(TreeInterface tree, Node node, String traitName);
    

//    public void check(RealParameter trait) throws Exception {
//        diffusionModel.check(trait);
//    }

//    // **************************************************************
//    // XMLElement IMPLEMENTATION
//    // **************************************************************
//
//    public Element createElement(Document d) {
//        throw new RuntimeException("Not implemented yet!");
//    }
//
//    // **************************************************************
//    // XMLObjectParser
//    // **************************************************************

//    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
//
//        public String getParserName() {
//            return TRAIT_LIKELIHOOD;
//        }
//
//        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
//
//            MultivariateDiffusionModel diffusionModel = (MultivariateDiffusionModel) xo.getChild(MultivariateDiffusionModel.class);
//            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
//
//            boolean cacheBranches = xo.getAttribute(CACHE_BRANCHES, false);
//            boolean integrate = xo.getAttribute(INTEGRATE, false);
//            boolean useTreeLength = xo.getAttribute(USE_TREE_LENGTH, false);
//            boolean scaleByTime = xo.getAttribute(SCALE_BY_TIME, false);
//            boolean reciprocalRates = xo.getAttribute(RECIPROCAL_RATES, false);
//            boolean reportAsMultivariate = xo.getAttribute(REPORT_MULTIVARIATE, true);
//
//            BranchRateModel rateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);
//
//            TreeTraitParserUtilities utilities = new TreeTraitParserUtilities();
//            String traitName = TreeTraitParserUtilities.DEFAULT_TRAIT_NAME;
//
//            TreeTraitParserUtilities.TraitsAndMissingIndices returnValue =
//                    utilities.parseTraitsFromTaxonAttributes(xo, traitName, treeModel, integrate);
//            CompoundParameter traitParameter = returnValue.traitParameter;
//            List<Integer> missingIndices = returnValue.missingIndices;
//            traitName = returnValue.traitName;
//
//            Model samplingDensity = null;
//
//            if (xo.hasChildNamed(SAMPLING_DENSITY)) {
//                XMLObject cxo = xo.getChild(SAMPLING_DENSITY);
//                samplingDensity = (Model) cxo.getChild(Model.class);
//            }
//
//            Parameter deltaParameter = null;
//            if (xo.hasChildNamed("delta")) {
//                XMLObject cxo = xo.getChild("delta");
//                deltaParameter = (Parameter) cxo.getChild(Parameter.class);
//            }
//            AbstractMultivariateTraitLikelihood like;
//
//            if (integrate) {
//
//                MultivariateDistributionLikelihood rootPrior =
//                        (MultivariateDistributionLikelihood) xo.getChild(MultivariateDistributionLikelihood.class);
//                if (rootPrior != null) {
//
//                    if (!(rootPrior.getDistribution() instanceof MultivariateDistribution))
//                        throw new XMLParseException("Only multivariate normal priors allowed for Gibbs sampling the root trait");
//
//                    MultivariateNormalDistribution rootDistribution =
//                            (MultivariateNormalDistribution) rootPrior.getDistribution();
//
//                    like = new SemiConjugateMultivariateTraitLikelihood(traitName, treeModel, diffusionModel,
//                            traitParameter, missingIndices, cacheBranches,
//                            scaleByTime, useTreeLength, rateModel, samplingDensity, reportAsMultivariate,
//                            rootDistribution, reciprocalRates);
//                } else {
//                    XMLObject cxo = xo.getChild(CONJUGATE_ROOT_PRIOR);
//                    if (cxo == null) {
//                        throw new XMLParseException("Must specify a conjugate or multivariate normal root prior");
//                    }
//
//                    boolean ignorePhylogeny = xo.getAttribute(IGNORE_PHYLOGENY, false);
//
//                    Parameter meanParameter = (Parameter) cxo.getChild(MultivariateDistributionLikelihood.MVN_MEAN)
//                            .getChild(Parameter.class);
//
//                    if (meanParameter.getDimension() != diffusionModel.getPrecisionmatrix().length) {
//                        throw new XMLParseException("Root prior mean dimension does not match trait diffusion dimension");
//                    }
//
//                    Parameter sampleSizeParameter = (Parameter) cxo.getChild(PRIOR_SAMPLE_SIZE).getChild(Parameter.class);
//
//                    double[] mean = meanParameter.getParameterValues();
//                    double pseudoObservations = sampleSizeParameter.getParameterValue(0);
//
//                    if (ignorePhylogeny) {
//                        boolean exchangeableTips = xo.getAttribute(EXCHANGEABLE_TIPS, true);
//
//                        like = new NonPhylogeneticMultivariateTraitLikelihood(traitName, treeModel, diffusionModel,
//                                traitParameter, deltaParameter, missingIndices, cacheBranches,
//                                scaleByTime, useTreeLength, rateModel, samplingDensity, reportAsMultivariate,
//                                mean, pseudoObservations, reciprocalRates, exchangeableTips);
//                    } else {
//                        like = new FullyConjugateMultivariateTraitLikelihood(traitName, treeModel, diffusionModel,
//                                traitParameter, deltaParameter, missingIndices, cacheBranches,
//                                scaleByTime, useTreeLength, rateModel, samplingDensity, reportAsMultivariate,
//                                mean, pseudoObservations, reciprocalRates);
//                    }
//                }
//            } else {
//
//                like = new SampledMultivariateTraitLikelihood(traitName, treeModel, diffusionModel,
//                        traitParameter, missingIndices, cacheBranches,
//                        scaleByTime, useTreeLength, rateModel, samplingDensity, reportAsMultivariate,
//                        reciprocalRates);
//            }
//
//            if (!integrate && xo.hasChildNamed(TreeTraitParserUtilities.RANDOMIZE)) {
//                utilities.randomize(xo);
//            }
//
//            if (xo.hasChildNamed(TreeTraitParserUtilities.JITTER)) {
//                utilities.jitter(xo, diffusionModel.getPrecisionmatrix().length, missingIndices);
//            }
//
//            if (xo.hasChildNamed(CHECK)) {
//                XMLObject cxo = xo.getChild(CHECK);
//                Parameter check = (Parameter) cxo.getChild(Parameter.class);
//                like.check(check);
//            }
//
//            if (xo.hasChildNamed(ASCERTAINMENT)) {
//                XMLObject cxo = xo.getChild(ASCERTAINMENT);
//                Taxon taxon = (Taxon) cxo.getChild(Taxon.class);
//                if (!integrate) {
//                    throw new XMLParseException("Ascertainment correction is currently only implemented" +
//                        " for integrated multivariate trait likelihood models");
//                }
//                like.setAscertainedTaxon(taxon);
//            }
//
//            return like;
//        }
//
//        //************************************************************************
//        // AbstractXMLObjectParser implementation
//        //************************************************************************
//
//        public String getParserDescription() {
//            return "Provides the likelihood of a continuous trait evolving on a tree by a " +
//                    "given diffusion model.";
//        }
//
//        public XMLSyntaxRule[] getSyntaxRules() {
//            return rules;
//        }
//
//        private final XMLSyntaxRule[] rules = {
//                new StringAttributeRule(TreeTraitParserUtilities.TRAIT_NAME, "The name of the trait for which a likelihood should be calculated"),
//                new ElementRule(TreeTraitParserUtilities.TRAIT_PARAMETER, new XMLSyntaxRule[]{
//                        new ElementRule(Parameter.class)
//                }),
//                new ElementRule("delta", new XMLSyntaxRule[]{
//                        new ElementRule(Parameter.class)
//                }, true),
//                AttributeRule.newBooleanRule(INTEGRATE, true),
////                new XORRule(
//                new ElementRule(MultivariateDistributionLikelihood.class, true),
//                new ElementRule(CONJUGATE_ROOT_PRIOR, new XMLSyntaxRule[]{
//                        new ElementRule(MultivariateDistributionLikelihood.MVN_MEAN,
//                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
//                        new ElementRule(PRIOR_SAMPLE_SIZE,
//                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
//                }, true),
////                        true),
//                new ElementRule(ASCERTAINMENT, new XMLSyntaxRule[] {
//                        new ElementRule(Taxon.class)
//                }, true),
//                new ElementRule(MultivariateDiffusionModel.class),
//                new ElementRule(TreeModel.class),
//                new ElementRule(BranchRateModel.class, true),
//                AttributeRule.newDoubleArrayRule("cut", true),
//                AttributeRule.newBooleanRule(REPORT_MULTIVARIATE, true),
//                AttributeRule.newBooleanRule(USE_TREE_LENGTH, true),
//                AttributeRule.newBooleanRule(SCALE_BY_TIME, true),
//                AttributeRule.newBooleanRule(RECIPROCAL_RATES, true),
//                AttributeRule.newBooleanRule(CACHE_BRANCHES, true),
//                AttributeRule.newIntegerRule(RANDOM_SAMPLE, true),
//                AttributeRule.newBooleanRule(IGNORE_PHYLOGENY, true),
//                AttributeRule.newBooleanRule(EXCHANGEABLE_TIPS, true),
//                new ElementRule(Parameter.class, true),
//                TreeTraitParserUtilities.randomizeRules(true),
//                TreeTraitParserUtilities.jitterRules(true),
//                new ElementRule(CHECK, new XMLSyntaxRule[]{
//                        new ElementRule(Parameter.class)
//                }, true)
//        };
//
//
//        public Class getReturnType() {
//            return AbstractMultivariateTraitLikelihood.class;
//        }
//    };

    TreeInterface treeModel = null;
    //MultivariateDiffusionModel 
    ContinuousSubstitutionModel diffusionModel = null;
    String traitName = null;
    RealParameter traitParameter;
    List<Integer> missingIndices;

    protected double logLikelihood;
    protected double maxLogLikelihood = Double.NEGATIVE_INFINITY;
    private double storedLogLikelihood;
    protected boolean likelihoodKnown = false;
    private boolean storedLikelihoodKnown = false;
    private BranchRateModel.Base rateModel = null;
    private boolean hasRateModel = false;

    double treeLength;
    private double storedTreeLength;

    private boolean reportAsMultivariate;

    private boolean scaleByTime;
    private boolean useTreeLength;
    private boolean reciprocalRates;

    protected boolean cacheBranches;
    protected double[] cachedLogLikelihoods;
    protected double[] storedCachedLogLikelihood;
    protected boolean[] validLogLikelihoods;
    protected boolean[] storedValidLogLikelihoods;

    private RealParameter deltaParameter;

    private boolean doAscertainmentCorrect = false;
    private int ascertainedTaxonIndex;

    protected int numData;
    protected int dimTrait;
    protected int dim;

}

