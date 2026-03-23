# Beast-Classic Strong Typing Migration TODO

Status: **compiles**, 1/3 test classes pass. 15 source files still use old parameter types.

## API cheat sheet

| Old | New (read-only) | New (mutable/operator) |
|-----|-----------------|------------------------|
| `Input<RealParameter>` scalar | `Input<RealScalar<? extends D>>` | `Input<RealScalarParam<? extends D>>` |
| `Input<RealParameter>` vector | `Input<RealVector<? extends D>>` | `Input<RealVectorParam<? extends D>>` |
| `Input<BooleanParameter>` | `Input<BoolVector>` | `Input<BoolVectorParam>` |
| `Input<IntegerParameter>` | `Input<IntVector<? extends D>>` | `Input<IntVectorParam<? extends D>>` |
| `.getValue()` | `.get()` | `.get()` |
| `.getValue(i)` | `.get(i)` | `.get(i)` |
| `.getDimension()` | `.size()` | `.size()` |
| `.setValue(v)` | -- | `.set(v)` |
| `.setValue(i, v)` | -- | `.set(i, v)` |
| `.getArrayValue(i)` | `.get(i)` | `.get(i)` |
| `.getValues()` (Double[]) | build manually | `.getValues()` (double[]) |
| `.somethingIsDirty()` | cast to StateNode | `.somethingIsDirty()` |
| `.isDirty(int)` | -- | `.isDirty(int)` (VectorParam only) |
| `.getID()` | `((BEASTInterface) x).getID()` | `.getID()` |

Domains: `PositiveReal`(>0), `NonNegativeReal`(>=0), `Real`(unbounded), `UnitInterval`([0,1]), `PositiveInt`, `Int`

## Remaining files (15 source + 1 test)

### Continuous trait hierarchy (5 files)

These form a dependency chain. Migrate top-down.

- [ ] `continuous/MultivariateDiffusionModel.java`
  - Extends `ContinuousSubstitutionModel`
  - `Input<RealParameter> diffusionPrecisionMatrixInput` -> `Input<RealVectorParam<? extends Real>>`
  - Uses `.getValues()`, `.getDimension()`

- [ ] `continuous/AbstractMultivariateTraitLikelihood.java`
  - Extends `GenericTreeLikelihood` -> switch to `beast.base.spec.evolution.likelihood.GenericTreeLikelihood`
  - Multiple `Input<RealParameter>` fields
  - Uses `.getValue()`, `.getDimension()`

- [ ] `continuous/IntegratedMultivariateTraitLikelihood.java`
  - Extends `AbstractMultivariateTraitLikelihood`

- [ ] `continuous/FullyConjugateMultivariateTraitLikelihood.java`
  - Extends `IntegratedMultivariateTraitLikelihood`

- [ ] `continuous/SampledMultivariateTraitLikelihood.java`
  - Extends `AbstractMultivariateTraitLikelihood`
  - Uses `.getDimension()`, `.getValue(i)`, `.setValue(i, v)`

### Operators (7 files)

All extend `Operator`. Use Param types (mutable).

- [ ] `evolution/operators/BitFlipBSSVSOperator.java`
  - `BooleanParameter` -> `BoolVectorParam`, `RealParameter` -> `RealVectorParam`
  - `.getValue(i)` -> `.get(i)`, `.setValue(i,v)` -> `.set(i,v)`, `.getDimension()` -> `.size()`

- [ ] `evolution/operators/PickIndicatorOperator.java`
  - `BooleanParameter` -> `BoolVectorParam`
  - `.getValue(i)` -> `.get(i)`, `.setValue(i,v)` -> `.set(i,v)`, `.getDimension()` -> `.size()`

- [ ] `evolution/operators/GeneralIntegerOperator.java`
  - `IntegerParameter` -> `IntVectorParam`, `RealParameter` -> `RealVectorParam`
  - `.getValue(i)` -> `.get(i)`, `.setValue(i,v)` -> `.set(i,v)`, `.getDimension()` -> `.size()`

- [ ] `evolution/operators/SphereRandomWalker.java`
  - `RealParameter` -> `RealVectorParam<? extends Real>` (location vector)

- [ ] `evolution/operators/RootTraitRandowWalkOperator.java`
  - Extends `RealRandomWalkOperator` -> switch to `beast.base.spec.inference.operator.RealRandomWalkOperator`
  - `RealParameter` in TreeTraitMap access already migrated

- [ ] `evolution/operators/PrecisionMatrixGibbsOperator.java`
  - `RealParameter` -> `RealVectorParam<? extends Real>` for precision matrix
  - Depends on continuous trait hierarchy types

- [ ] `evolution/operators/TraitGibbsOperator.java`
  - Multiple `RealParameter` fields -> spec types
  - Depends on continuous trait hierarchy types

### Other (2 files)

- [ ] `evolution/tree/coalescent/Expansion.java`
  - Extends old `ExponentialGrowth` -> switch to `beast.base.spec.evolution.tree.coalescent.ExponentialGrowth`
  - `Input<RealParameter>` -> `Input<RealScalar<? extends UnitInterval>>`
  - Parent inputs become `RealScalar<? extends PositiveReal>` (popSize), `RealScalar<? extends Real>` (growthRate)

- [ ] `evolution/substitutionmodel/SVSGeneralSubstitutionModel.java`
  - Still uses `BooleanParameter` for indicator -> `BoolVectorParam`
  - Already extends spec `ComplexSubstitutionModel`

### BeauTi (deferred)

- [ ] `app/beauti/BeautiDiscreteTraitProvider.java` -- uses `RealParameter` in one place

### Tests

- [ ] `test/math/distributions/WishartDisitrbutionTest.java`
  - Constructs `new RealParameter(...)` -> `new RealVectorParam<>()` with `initByName("value", ...)`
  - `scaleMatrix` input now expects `RealVector<? extends NonNegativeReal>`

- [ ] `examples/RacRABV_LogNRRW2.xml` and `examples/AncestralState.xml`
  - XML references to old parameter class names need updating to spec param types
  - `spec='beast.base.inference.parameter.RealParameter'` -> `spec='beast.base.spec.inference.parameter.RealVectorParam'` etc.

### module-info.java

Should not need changes -- `requires beast.base` already covers the spec packages. Verify after migration.

## Verification

```sh
mvn compile -pl beast-classic              # must pass
mvn test -pl beast-classic                 # 3/3 test classes should pass
# Check no old imports remain:
grep -rn "beast.base.inference.parameter.RealParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.BooleanParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.IntegerParameter" beast-classic/src/main/java/ --include="*.java"
```
