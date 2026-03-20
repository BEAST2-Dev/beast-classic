# BEAST Classic

[![Build Status](https://github.com/alexeid/beast-classic/workflows/Unit%2Fintegration%20tests/badge.svg)](https://github.com/alexeid/beast-classic/actions)

Classic BEAST models ported from BEAST 1, providing discrete and continuous
phylogeography, GMRF skyride coalescent models, and generalised linear models
for substitution rates.

## Building

Requires Java 25 and Maven.

```bash
mvn compile
```

BEAST3 dependencies are resolved from GitHub Packages. You may need a
GitHub personal access token configured in your `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>
  </server>
</servers>
```

## Module

JPMS module name: `beast.classic`

### Discrete phylogeography

- `SVSGeneralSubstitutionModel` — stochastic variable selection on substitution rates
- `BayesianStochasticSearchVariableSelection` — BSSVS indicator logic
- `AlignmentFromTrait` / `AlignmentFromTraitMap` — create discrete alignments from trait data
- `BitFlipBSSVSOperator` — operator for BSSVS indicators
- `RateIndicatorInitializer` — initialises rate indicators
- `CTMCScalePrior` — CTMC reference prior for rate scaling

### Continuous phylogeography

- `MultivariateDiffusionModel` — Brownian diffusion on a tree
- `SampledMultivariateTraitLikelihood` — sampled trait likelihood
- `IntegratedMultivariateTraitLikelihood` — integrated (marginalised) trait likelihood
- `AncestralStateTreeLikelihood` — ancestral state reconstruction
- `GeoSpatialDistribution` / `Polygon2D` — geographic constraints

### GMRF Skyride

- `GMRFSkyrideLikelihood` — Gaussian Markov random field skyride coalescent
- `GMRFMultilocusSkyrideLikelihood` — multilocus extension
- `GMRFSkyrideBlockUpdateOperator` / `GMRFMultilocusSkyrideBlockUpdateOperator` — block update operators

### Coalescent population models

- `LogisticGrowth` — logistic population growth function
- `Expansion` — expansion population growth function

### GLM substitution models

- `GLM` — generalised linear model for substitution rates
- `GLMBasedSubstModel` — substitution model driven by GLM
- `GlmModel` — abstract GLM base class
- `LogLinear` — log-linear model
- `GeneralizedLinearModel` — GLM distribution

### Other substitution models

- `FLU` — influenza amino acid model
- `LG` — Le-Gascuel amino acid model
- `RobustEigenSystem` — robust eigendecomposition for rate matrices
- `ContinuousSubstitutionModel` — base class for continuous-time models

### Tree trait handling

- `TreeTraitMap` / `TreeTraitProvider` — map traits onto tree nodes
- `TreeWithTraitLogger` — log tree with trait annotations
- `RootTrait` / `LeafTrait` — root and leaf trait accessors

### Distributions

- `MultivariateNormalDistribution` — multivariate normal
- `WishartDistribution` — Wishart distribution for precision matrices

### BEAUti integration

- `BeautiDiscreteTraitProvider` / `BeautiLocationTraitProvider` — trait data import
- `TraitInputEditor` / `LocationInputEditor` — trait editing UI
- `TraitDialog` — trait configuration dialog

## BEAST3 migration status

Branch: `beast3`

All 67 source files compile against beast3 (`beast-base` 2.8.0-SNAPSHOT).

Changes from BEAST2:
- Maven build (was Ant)
- JPMS module `beast.classic`
- `commons-math` 2.x/3.x replaced with `commons-statistics` and `java.lang.Math`
- `org.json` replaced with `beast.base.internal.json`
- `MathException` replaced with `RuntimeException`
- MTJ (Matrix Toolkits for Java) added as Maven dependency for GMRF
- `StateNode.getDimension()` replaced with `BooleanParameter` input

TODO:
- [ ] Migrate tests from JUnit 4 to JUnit 5
- [ ] Add `version.xml` for BEAST package manager
- [ ] Add release script
