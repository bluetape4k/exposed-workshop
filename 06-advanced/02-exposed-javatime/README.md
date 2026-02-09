# 06 Advanced: Exposed JavaTime Integration

This module (`02-exposed-javatime`) provides examples and test cases demonstrating the integration of
`java.time` (JSR 310) API types with the Exposed framework using the
`exposed-javatime` extension. It showcases how to effectively define and manipulate date and time data in your database using modern Java date/time objects, ensuring type safety and consistency.

## Key Features and Components:

### 1. Basic JavaTime Usage (`Ex01_JavaTime.kt`)

- Illustrates the fundamental process of defining columns using `LocalDate`, `LocalDateTime`, `Instant`, and other
  `java.time` types in Exposed table schemas.
- Covers basic CRUD operations involving these modern date and time types.

### 2. Default Values for Date/Time Columns (`Ex02_Defaults.kt`)

- Demonstrates how to assign default values to `java.time` columns, including dynamic defaults like `CurrentDateTime` or
  `CurrentTimestamp` for automatically recording creation or update times.

### 3. Date/Time Literals in Queries (`Ex03_DateTimeLiteral.kt`)

- Focuses on using
  `java.time` literals directly within Exposed queries, enabling precise filtering and comparisons based on date and time values.

### 4. Miscellaneous JavaTime Examples (`Ex04_MiscTable.kt`)

- Provides a table structure with a variety of
  `java.time` columns to showcase their diverse usage patterns and interactions within Exposed.

## Purpose:

This module is designed to help users understand:

- How to effectively integrate `java.time` types into their Exposed-based database applications.
- Defining date and time columns with precision and proper handling.
- Utilizing default values and literals for date/time fields in queries.
- Leveraging the `exposed-javatime` extension for modern date/time management.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/java/time`.
2. Run the test cases using your IDE or Gradle to observe how `exposed-javatime` simplifies date and time handling.

This module provides a clear guide to mastering `java.time` integration with Exposed.
