# Beast-Classic BEAST3 Migration

Status: **gold standard complete** (Java classes). Example XML migration in progress.

## Java migration

All 67 source files migrated to spec types. 9/9 tests pass.

All `RealParameter`, `BooleanParameter`, and `IntegerParameter` imports removed from
`src/main/java/`. Replaced with spec types: `RealVectorParam`, `RealScalarParam`,
`IntVectorParam`, `BoolVectorParam`, etc.

`MatrixVectorParam<D>` (in `beastclassic.spec.parameter`) extends `RealVectorParam<D>`
with `minordimension` support for matrix-style parameters used by the continuous trait
likelihood classes.

All `beast.base.core.Function` usage removed. beast-classic does not depend on beast3
PR #48 (spec types implementing Function). `Function` is on a deprecation path in beast3.

## Example XML migration

### Done

- **testSkyGrid.xml**: spec params, spec Gamma (no Prior wrapper), spec ScaleOperator
  and RealRandomWalkOperator. AVMN removed (no spec equivalent). Verified sampling.
- **testSkyRide.xml**: same pattern as testSkyGrid.
- **testDiscreteSmall.xml**: BoolVectorParam, IntVectorParam, RealVectorParam, RealScalarParam,
  SimplexParam. Spec Gamma/IID distributions, spec ScaleOperator/BitFlipOperator/UpDownOperator,
  spec StrictClockModel/Frequencies. RPNcalculator removed. Verified sampling.
- **beast1/testBinaryDollo2.xml**: TreeStatLogger fix only (deprecated params feed into
  beast-base classes, not beast-classic).

- **H5N1_HA_discrete2.xml**: full spec migration using spec SiteModel, spec Frequencies/SimplexParam,
  spec distributions (IID/Gamma/Exponential), spec operators. RPNcalculator and ParameterCumSum
  removed (Function interface). nonZeroRatePrior removed. Verified: parses and initialises
  (98 taxa, slow UPGMA tree construction).

### Not applicable (BEAST 1 format, not runnable)

- H5N1_HA_discrete1.xml
- RacRABV_LogNRRW1.xml
- beast1/testBinaryDollo1.xml

### Already clean

- RacRABV_LogNRRW2.xml (no deprecated parameter types)

## Migration pattern for XMLs

Each XML needs these changes for parameters consumed by beast-classic classes:

1. **Parameters**: `spec='parameter.RealParameter' lower='0'` → `spec='RealVectorParam' domain='NonNegativeReal'` (or `RealScalarParam` for scalars)
2. **Distributions**: `<prior x="@param"><Gamma distr .../></prior>` → `<distribution spec='beast.base.spec.inference.distribution.Gamma' param="@param"><alpha .../><theta .../></distribution>`
3. **Operators**: old `ScaleOperator`/`BactrianScaleOperator` → `beast.base.spec.inference.operator.ScaleOperator`; old `BactrianRandomWalkOperator` → `beast.base.spec.inference.operator.RealRandomWalkOperator`
4. **Namespace**: add `beast.base.spec.inference.parameter:beast.base.spec.inference.operator:beast.base.spec.inference.distribution`

Parameters consumed only by beast-base classes (e.g. HKY kappa, clock rate) can stay as old `RealParameter` with old priors/operators (hybrid approach, as in sampled-ancestors bears.xml).

## Other fixes

- **pom.xml**: excluded `com.github.fommil.netlib:all` from mtj to fix JPMS invalid module name error during `exec:exec`
- **TreeHeightLogger** → `TreeStatLogger` (renamed in beast3)
- **TreeList** wrapper needed for `GMRFMultilocusSkyrideLikelihood.trees` input

## Verification

```sh
mvn compile           # must pass
mvn test              # 9/9 tests pass
mvn exec:exec -Dbeast.args="examples/testSkyGrid.xml"   # starts sampling
```
