# BEAST Classic

[![CI & Publish](https://github.com/BEAST2-Dev/beast-classic/actions/workflows/ci-publish.yml/badge.svg)](https://github.com/BEAST2-Dev/beast-classic/actions)

Classic BEAST models ported from BEAST 1, providing discrete and continuous
phylogeography, GMRF skyride coalescent models, and generalised linear models
for substitution rates.

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.compevol</groupId>
    <artifactId>beast-classic</artifactId>
    <version>1.7.0</version>
</dependency>
```

JPMS module name: `beast.classic`

## Building

Requires Java 25 and Maven. BEAST 3 dependencies are resolved from Maven Central.

```bash
mvn compile
```

## Running

Run BEAST with beast-classic on the module path:

```bash
mvn exec:exec -Dbeast.args="examples/testPhylogeography.xml"
```

Run BEAUti:

```bash
mvn exec:exec -Dbeast.module=beast.fx -Dbeast.main=beastfx.app.beauti.Beauti
```

## Discrete phylogeography

- `SVSGeneralSubstitutionModel` -- stochastic variable selection on substitution rates
- `BayesianStochasticSearchVariableSelection` -- BSSVS indicator logic
- `AlignmentFromTrait` / `AlignmentFromTraitMap` -- create discrete alignments from trait data
- `BitFlipBSSVSOperator` -- operator for BSSVS indicators
- `RateIndicatorInitializer` -- initialises rate indicators
- `CTMCScalePrior` -- CTMC reference prior for rate scaling

## Continuous phylogeography

- `MultivariateDiffusionModel` -- Brownian diffusion on a tree
- `SampledMultivariateTraitLikelihood` -- sampled trait likelihood
- `IntegratedMultivariateTraitLikelihood` -- integrated (marginalised) trait likelihood
- `AncestralStateTreeLikelihood` -- ancestral state reconstruction
- `GeoSpatialDistribution` / `Polygon2D` -- geographic constraints

## GMRF Skyride

- `GMRFSkyrideLikelihood` -- Gaussian Markov random field skyride coalescent
- `GMRFMultilocusSkyrideLikelihood` -- multilocus extension
- `GMRFSkyrideBlockUpdateOperator` / `GMRFMultilocusSkyrideBlockUpdateOperator` -- block update operators

## Coalescent population models

- `LogisticGrowth` -- logistic population growth function
- `Expansion` -- expansion population growth function

## GLM substitution models

- `GLM` -- generalised linear model for substitution rates
- `GLMBasedSubstModel` -- substitution model driven by GLM
- `GlmModel` -- abstract GLM base class
- `LogLinear` -- log-linear model
- `GeneralizedLinearModel` -- GLM distribution

## Other substitution models

- `FLU` -- influenza amino acid model
- `LG` -- Le-Gascuel amino acid model
- `RobustEigenSystem` -- robust eigendecomposition for rate matrices
- `ContinuousSubstitutionModel` -- base class for continuous-time models

## Tree trait handling

- `TreeTraitMap` / `TreeTraitProvider` -- map traits onto tree nodes
- `TreeWithTraitLogger` -- log tree with trait annotations
- `RootTrait` / `LeafTrait` -- root and leaf trait accessors

## Distributions

- `MultivariateNormalDistribution` -- multivariate normal
- `WishartDistribution` -- Wishart distribution for precision matrices

## BEAUti integration

- `BeautiDiscreteTraitProvider` / `BeautiLocationTraitProvider` -- trait data import
- `TraitInputEditor` / `LocationInputEditor` -- trait editing UI
- `TraitDialog` -- trait configuration dialog

## License

[GNU Lesser General Public License v2.1](LICENSE)
