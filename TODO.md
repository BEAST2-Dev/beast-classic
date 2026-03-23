# Beast-Classic Strong Typing Migration TODO

Status: **compiles**, 2/3 test classes pass. Only BeauTi file uses old parameter types.

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

### BeauTi (deferred)

- [ ] `app/beauti/BeautiDiscreteTraitProvider.java` -- uses `RealParameter` in one place

### XML test files

- [ ] `examples/RacRABV_LogNRRW2.xml` -- uses `spec='RealParameter'`, `spec='IntegerParameter'`, `minordimension` attribute
- [ ] `examples/AncestralState.xml` -- similar XML references
- These need `spec='RealParameter'` -> `spec='RealVectorParam'` etc, plus `minordimension` handling

## Verification

```sh
mvn compile -pl beast-classic              # must pass
mvn test -pl beast-classic                 # 3/3 test classes should pass
# Check no old imports remain:
grep -rn "beast.base.inference.parameter.RealParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.BooleanParameter" beast-classic/src/main/java/ --include="*.java"
grep -rn "beast.base.inference.parameter.IntegerParameter" beast-classic/src/main/java/ --include="*.java"
```
