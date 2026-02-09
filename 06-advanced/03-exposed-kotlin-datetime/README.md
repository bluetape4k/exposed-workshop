# 06 Advanced: Exposed Kotlinx-Datetime Integration

This module (`03-exposed-kotlin-datetime`) provides examples and test cases demonstrating the integration of
`kotlinx.datetime` API types with the Exposed framework using the
`exposed-kotlin-datetime` extension. It showcases how to effectively define and manipulate date and time data in your database using Kotlin-native date/time objects, offering a modern and idiomatic alternative to
`java.time` for Kotlin applications.

## Key Features and Components:

### 1. Basic Kotlinx-Datetime Usage (`Ex01_KotlinDateTime.kt`)

- Illustrates the fundamental process of defining columns using `kotlinx.datetime` types such as `LocalDate`,
  `LocalDateTime`, `Instant`, and `TimeZone` in Exposed table schemas.
- Covers basic CRUD operations involving these Kotlin-native date and time types.

### 2. Default Values for Date/Time Columns (`Ex02_Defaults.kt`)

- Demonstrates how to assign default values to
  `kotlinx.datetime` columns, including dynamic defaults for automatically recording creation or update times.

### 3. Date/Time Literals in Queries (`Ex03_DateTimeLiteral.kt`)

- Focuses on using
  `kotlinx.datetime` literals directly within Exposed queries, enabling precise filtering and comparisons based on date and time values.

### 4. Kotlinx-Datetime Support Helpers (`KotlinDateTimeSupports.kt`)

- Contains helper functions, utilities, or configurations specifically designed to facilitate testing and usage of
  `kotlinx.datetime` support within Exposed.

## Purpose:

This module is designed to help users understand:

- How to effectively integrate
  `kotlinx.datetime` types into their Exposed-based database applications for Kotlin-native date/time handling.
- Defining date and time columns with precision and proper handling using `kotlinx.datetime`.
- Utilizing default values and literals for date/time fields in queries.
- Leveraging the `exposed-kotlin-datetime` extension as a modern alternative to `java.time` integration.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/kotlin/datetime`.
2. Run the test cases using your IDE or Gradle to observe how
   `exposed-kotlin-datetime` simplifies date and time handling in Kotlin.

This module provides a clear guide to mastering `kotlinx.datetime` integration with Exposed.
