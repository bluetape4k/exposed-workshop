# 05 Exposed DML: Functions

This module (
`03-functions`) provides a comprehensive set of examples and test cases demonstrating how to utilize various SQL functions within Exposed queries. It covers a wide array of function categories, including general-purpose, mathematical, statistical, trigonometric, and advanced window functions, showcasing Exposed's capability to integrate complex SQL logic directly into Kotlin code.

## Key Features and Components:

### 1. Function Example Base (`exposed.examples.functions` package)

- **`Ex00_FunctionBase.kt`
  **: Serves as a foundational class or common setup for the various function examples, providing shared utilities or test data.

### 2. General SQL Functions

- **`Ex01_Functions.kt`
  **: Contains general examples of integrating common SQL functions (e.g., string manipulation, date functions, conditional expressions) into Exposed queries.

### 3. Mathematical Functions

- **`Ex02_MathFunction.kt`**: Demonstrates how to use mathematical SQL functions (e.g., `ABS`, `ROUND`, `CEIL`,
  `FLOOR`) with Exposed, enabling numerical operations directly in queries.

### 4. Statistical Functions

- **`Ex03_StatisticsFunction.kt`**: Provides examples for applying aggregate and statistical functions (e.g., `AVG`,
  `SUM`, `COUNT`, `MIN`, `MAX`) to dataset columns within Exposed.

### 5. Trigonometric Functions

- **`Ex04_TrigonometricalFunction.kt`**: Illustrates the usage of trigonometric SQL functions (e.g., `SIN`, `COS`,
  `TAN`) for specialized mathematical calculations.

### 6. Window Functions

- **`Ex05_WindowFunction.kt`**: Showcases advanced SQL window functions (e.g., `ROW_NUMBER`, `RANK`, `LEAD`,
  `LAG`) with Exposed, enabling complex analytical operations over partitioned and ordered result sets.

## Purpose:

This module is designed to help users understand:

- How to seamlessly incorporate a wide variety of SQL functions into Exposed queries.
- Enhancing data manipulation and analysis capabilities directly within Kotlin code.
- Leveraging advanced SQL features like window functions with Exposed.

## Getting Started:

To explore these examples:

1. Review the source code in `src/test/kotlin/exposed/examples/functions`.
2. Run the test cases using your IDE or Gradle to observe how different SQL functions are implemented and used with Exposed.

This module serves as a practical guide to mastering the use of SQL functions in Exposed.
