# Exposed Measured Columns

English | [한국어](./README.ko.md)

This example shows how to persist typed measurements with the
`bluetape4k-exposed-measured` JDBC extension. `Length`, `Mass`, and absolute
`Temperature` values are stored as `DOUBLE` values in their provider-defined
base units and are reconstructed as typed values when selected.

## What this example covers

- DSL columns: `length("length")`, `mass("mass")`, and `temperature("temperature")`.
- DAO properties backed by the same columns through `IntEntity` and
  `EntityClass`.
- Unit conversion on write (`150.centimeters()` and `77.fahrenheit()`) and
  base-unit conversion on read.
- Nullable measured values and floating-point comparisons with an explicit
  tolerance.
- The provider's incompatible DB-value failure boundary.

## Architecture

![Measured Exposed JDBC architecture](../../../docs/images/readme-diagrams/06-advanced-13-exposed-measured-architecture-01.png)

![Measured product ERD](../../../docs/images/readme-diagrams/06-advanced-13-exposed-measured-erd-01.png)

The physical schema is intentionally small:

| Column | Kotlin value | Physical type | Stored base unit |
| --- | --- | --- | --- |
| `length` | `Measure<Length>` | `DOUBLE` | metre (`m`) |
| `mass` | `Measure<Mass>` | `DOUBLE` | kilogram (`kg`) |
| `nullable_mass` | `Measure<Mass>?` | `DOUBLE NULL` | kilogram (`kg`) |
| `temperature` | `Temperature` | `DOUBLE` | Kelvin (`K`) |

Changing a provider base unit is a schema and migration decision. The example
does not alter an existing table, define a measurement accuracy policy, or
provide a currency/measurement abstraction. R2DBC is intentionally out of
scope; it belongs in the `exposed-r2dbc-workshop` repository.

## DSL and DAO usage

```kotlin
internal object ProductTable: IntIdTable("measured_products") {
    val length = length("length")
    val mass = mass("mass")
    val nullableMass = mass("nullable_mass").nullable()
    val temperature = temperature("temperature")
}

internal class ProductEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: EntityClass<Int, ProductEntity>(ProductTable)

    var length by ProductTable.length
    var mass by ProductTable.mass
    var nullableMass by ProductTable.nullableMass
    var temperature by ProductTable.temperature
}

transaction {
    ProductTable.insert {
        it[length] = 150.centimeters()
        it[mass] = 2.5.kilograms()
        it[temperature] = 77.fahrenheit()
    }
}
```

The generic `Measure<T>` boundary prevents assigning a mass value to a length
column at compile time. On read, compare through a requested unit rather than
comparing the raw `DOUBLE` representation:

```kotlin
(row[ProductTable.length] `in` Length.meters).shouldBeNear(1.5, 1e-10)
row[ProductTable.temperature].inCelsius().shouldBeNear(25.0, 1e-10)
```

## Tests

```bash
./gradlew :13-exposed-measured:test
./gradlew :13-exposed-measured:test -PuseFastDB=true
```

`Ex01MeasuredColumns` runs the JDBC round-trip, DAO nullable, precision, and
incompatible-value cases through the shared `TestDB` matrix.
