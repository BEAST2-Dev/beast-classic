open module beast.classic {
    requires beast.base;
    requires beast.pkgmgmt;
    requires java.xml;
    requires java.logging;
    requires java.desktop;
    requires java.scripting;
    requires beagle;
    requires colt;
    requires mtj;
    requires org.apache.commons.statistics.distribution;
    requires static beast.fx;
    requires static javafx.controls;

    exports beastclassic.app.beauti;
    exports beastclassic.spec.parameter;
    exports beastclassic.continuous;
    exports beastclassic.dr;
    exports beastclassic.dr.evolmodel.continuous;
    exports beastclassic.dr.math.distributions;
    exports beastclassic.dr.util;
    exports beastclassic.evolution.alignment;
    exports beastclassic.evolution.datatype;
    exports beastclassic.evolution.likelihood;
    exports beastclassic.evolution.operators;
    exports beastclassic.evolution.speciation;
    exports beastclassic.evolution.substitutionmodel;
    exports beastclassic.evolution.tree;
    exports beastclassic.evolution.tree.coalescent;
    exports beastclassic.geo;
    exports beastclassic.inference;
    exports beastclassic.inference.distribution;
    exports beastclassic.math.distributions;
    exports beastclassic.phylogeography;

    provides beast.base.core.BEASTInterface with
        beastclassic.continuous.SampledMultivariateTraitLikelihood,
        beastclassic.continuous.MultivariateDiffusionModel,
        beastclassic.evolution.alignment.AlignmentFromTrait,
        beastclassic.evolution.alignment.AlignmentFromTraitMap,
        beastclassic.evolution.datatype.ContinuousDataType,
        beastclassic.evolution.datatype.LocationDataType,
        beastclassic.evolution.likelihood.AncestralSequenceLogger,
        beastclassic.evolution.likelihood.AncestralStateTreeLikelihood,
        beastclassic.evolution.likelihood.LeafTrait,
        beastclassic.evolution.operators.BitFlipBSSVSOperator,
        beastclassic.evolution.operators.GMRFMultilocusSkyrideBlockUpdateOperator,
        beastclassic.evolution.operators.GMRFSkyrideBlockUpdateOperator,
        beastclassic.evolution.operators.GeneralIntegerOperator,
        beastclassic.evolution.operators.PickIndicatorOperator,
        beastclassic.evolution.operators.PrecisionMatrixGibbsOperator,
        beastclassic.evolution.operators.RegressionGibbsEffectOperator,
        beastclassic.evolution.operators.RegressionGibbsPrecisionOperator,
        beastclassic.evolution.operators.RootTraitRandowWalkOperator,
        beastclassic.evolution.operators.SphereRandomWalker,
        beastclassic.evolution.operators.TraitGibbsOperator,
        beastclassic.evolution.speciation.BirthDeathSerialSampling,
        beastclassic.evolution.substitutionmodel.FLU,
        beastclassic.evolution.substitutionmodel.GLM,
        beastclassic.evolution.substitutionmodel.GLMBasedSubstModel,
        beastclassic.evolution.substitutionmodel.LG,
        beastclassic.evolution.substitutionmodel.LogLinear,
        beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModel,
        beastclassic.evolution.substitutionmodel.SVSGeneralSubstitutionModelLogger,
        beastclassic.evolution.tree.RootTrait,
        beastclassic.evolution.tree.TreeTraitMap,
        beastclassic.evolution.tree.TreeWithTraitLogger,
        beastclassic.evolution.tree.coalescent.Expansion,
        beastclassic.evolution.tree.coalescent.GMRFMultilocusSkyrideLikelihood,
        beastclassic.evolution.tree.coalescent.GMRFSkyrideLikelihood,
        beastclassic.evolution.tree.coalescent.LogisticGrowth,
        beastclassic.evolution.tree.coalescent.TreeList,
        beastclassic.geo.GeoSpatialDistribution,
        beastclassic.inference.distribution.GeneralizedLinearModel,
        beastclassic.inference.distribution.LinearRegression,
        beastclassic.inference.distribution.LogisticRegression,
        beastclassic.math.distributions.CTMCScalePrior,
        beastclassic.math.distributions.MultivariateNormalDistribution,
        beastclassic.math.distributions.WishartDistribution,
        beastclassic.phylogeography.RateIndicatorInitializer,
        beastclassic.spec.parameter.MatrixVectorParam;
}
