# Beast-Classic Strong Typing Migration TODO

Status: **compiles**, 2/3 test classes pass. Zero old parameter imports in main source (including BeauTi).

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

## Completed

All 64 non-BeauTi source files migrated to spec types. WishartDistribution test fixed and passing.

## Remaining

### XML test files (blocked by beast3 upstream)

The 2 XML-based tests fail because `RealVectorParam` does not implement `Function`.
In beast3, `Parameter<T> extends Function` but the spec types (`RealVectorParam`, etc.) do not.
This means any beast3 class with `Input<Function>` cannot accept spec param types.

Affected beast3 classes: `TreeWithMetaDataLogger` (metadata input), `LogNormalDistributionModel` (M, S inputs).
Affected beast-classic classes: `MultivariateNormalDistribution` (arg input -- takes `RootTrait` which implements `Function`).

**Resolution options (all upstream in beast3):**
1. Have `RealVectorParam` implement `Function` (cleanest)
2. Have `RealVector` extend `Function` (broader)
3. Change beast3 `Input<Function>` usages to accept spec types

Until resolved, these XML tests cannot pass with pure spec param types.

- [x] `examples/RacRABV_LogNRRW2.xml` -- updated to spec param types (requires beast3 PR #48)
- [ ] `examples/H5N1_HA_discrete2.xml` -- not yet updated
- [ ] `testSampledMultivariateTraitLikelihood` -- XML parses and likelihood computes, but value differs from expectation (-1443838 vs -835885). Likely due to loss of `minordimension` attribute which affected parameter initialization. Needs investigation: run the old code to confirm whether the new value is correct and the test expectation just needs recalibrating.
- [ ] `testAncestralStateTreeLikelihood` -- H5N1 XML not yet updated

## Verification

```sh
mvn compile -pl beast-classic              # must pass
mvn test -pl beast-classic                 # 3/3 test classes should pass
# Check no old imports remain:
grep -rn "beast.base.inference.parameter.RealParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.BooleanParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.IntegerParameter" beast-classic/src/main/java/ --include="*.java"
```
