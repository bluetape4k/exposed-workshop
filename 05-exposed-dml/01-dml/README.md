# 05 DML Basic Operations (01-dml)

This module provides a comprehensive set of examples demonstrating various Data Manipulation Language (DML) operations using the Exposed framework. It covers fundamental and advanced techniques for
`SELECT`, `INSERT`, and `UPDATE` statements, showcasing how to interact with your database using idiomatic Kotlin code.

## Examples Covered:

### 1. SELECT Operations (Ex01_Select.kt)

- **Basic Selection**: Simple `SELECT` queries with `WHERE` clauses for filtering data.
- **Conditional Logic**: Combining `WHERE` conditions using `AND` and `OR` operators.
- **Comparison Operators**: Usage of `NOT EQUAL` (`<>`) and other comparison operators.
- **List-based Filtering**: Utilizing `inList` and `notInList` for single values, paired expressions, and
  `EntityID` columns.
- **Subquery Filtering**: Demonstrations of `inSubQuery` and `notInSubQuery` for complex filtering criteria.
- **Table-based Filtering**: Examples using `inTable` and `notInTable` to filter based on another table's content.
- **Quantified Comparisons**: Usage of `ANY` and
  `ALL` with subqueries, arrays, and lists in conjunction with operators like `eq`, `neq`, and `greaterEq`.
- **Distinct Results**: Achieving unique results with `SELECT DISTINCT` and `SELECT DISTINCT ON` clauses.
- **Compound Conditions**: Combining multiple `Op` objects with `compoundAnd` and `compoundOr`.
- **Query Customization**: Adding comments to SQL queries.
- **Pagination**: Implementing `LIMIT` and `OFFSET` for result set control.

### 2. INSERT Operations (Ex02_Insert.kt)

- **Basic Insertion**: Simple `INSERT` statements.
- **Retrieving IDs**: Using `insertAndGetId` for tables with auto-incrementing integer IDs (
  `IntIdTable`) and custom ID columns.
- **Conflict Handling**: Employing `insertIgnoreAndGetId` and
  `insertIgnore` to gracefully handle unique constraint violations.
- **Predefined IDs**: Inserting records with explicitly provided `EntityID` values (e.g., String or UUID).
- **Batch Insertion**: Efficiently inserting multiple rows using `batchInsert`.
- **Expression-based Inserts**: Inserting values derived from database functions (e.g., `SUBSTRING`,
  `TRIM`) or subqueries.
- **DAO-style Inserts**: Demonstrating data insertion using Exposed's DAO (Data Access Object) pattern.
- **Client-side Defaults**: Utilizing `clientDefault` for values generated on the client.
- **Transaction Rollback
  **: Examples of transaction rollback in both regular and suspended contexts when constraint exceptions occur.
- **Generated Columns**: Inserting data into tables with database-generated columns.
- **Default Expressions**: Inserting records where some columns have default values (e.g., `CURRENT_TIMESTAMP`).
- **UUID Primary Keys**: Demonstrating the use of `databaseGenerated` UUIDs as primary keys.

### 3. UPDATE Operations (Ex03_Update.kt)

- **Basic Updates**: Standard `UPDATE` statements with `WHERE` clauses.
- **Limited Updates**: Applying `LIMIT` to `UPDATE` statements (with notes on dialect compatibility).
- **Joined Updates**: Updating records across multiple tables using `INNER JOIN`.
- **Conditional Joined Updates**: `UPDATE` operations involving joins with additional `WHERE` conditions.
- **Subquery Joins in Updates**: Utilizing subqueries within `JOIN` clauses for more complex update scenarios.

Each example is provided as a test case, making it easy to understand and verify the functionality against different database dialects.
