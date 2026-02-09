# 02 Alternatives to JPA: R2DBC Example

This module (
`r2dbc-example`) demonstrates how to build a reactive Spring Boot application leveraging R2DBC (Reactive Relational Database Connectivity) through Spring Data R2DBC. It serves as a practical example for those exploring alternatives to traditional JPA or Exposed for reactive and non-blocking database interactions. The module manages a "Post and Comment" database, showcasing reactive data access patterns, exposed through RESTful endpoints.

## Key Features and Components:

### 1. Spring Boot Application Core (`R2dbcApplication.kt`)

- Standard Spring Boot entry point, configured as a
  `REACTIVE` web application, likely utilizing Spring WebFlux for its web layer.

### 2. Domain Model (`alternative.r2dbc.example.domain.model` package)

- **R2DBC Entities (`Post.kt`, `Comment.kt`, `Customer.kt`)
  **: Defines the database entities using Spring Data R2DBC annotations (`@Table`, `@Column`, `@Id`).
    - `Post`: Represents a blog post with an `id`, `title`, and `content`.
    - `Comment`: Represents a comment on a post with an `id`, `content`, and a
      `postId` establishing a many-to-one relationship with `Post`.
    - `Customer`: (Assumed) Represents a customer entity, demonstrating additional data models.
- **Relationships**: Establishes a standard parent-child relationship between `Post` and
  `Comment` entities, demonstrating how to define and manage these relationships with Spring Data R2DBC.

### 3. Reactive Data Access Layer (`alternative.r2dbc.example.domain.repository` package)

- **`PostRepository.kt`**: Provides an interface for reactive database operations on
  `Post` entities, typically extending `ReactiveCrudRepository`.
- **`CommentRepository.kt`**: Provides an interface for reactive database operations on `Comment` entities.
- **`CustomerRepository.kt`**: Provides an interface for reactive database operations on `Customer` entities.

### 4. RESTful API Controllers (`alternative.r2dbc.example.controller` package)

- (Assumed) Contains controllers like `PostController.kt`, `CommentController.kt`, and
  `CustomerController.kt` to handle reactive HTTP requests for managing posts, comments, and customers respectively. These would expose the reactive data access layer through a RESTful API.

### 5. Other Components (`alternative.r2dbc.example.config`, `exceptions`, `utils` packages)

- **Config**: Application configuration, including R2DBC specific settings.
- **Exceptions**: Custom exception handling for the reactive application.
- **Utils**: General utility functions supporting the application.

## Getting Started:

This module provides a fully functional Spring Boot WebFlux application showcasing reactive database operations with R2DBC. To run this application, you would typically:

1. Configure your database connection in `application.properties` or `application.yml` (located in
   `src/main/resources`).
2. Build the project using Gradle.
3. Run the `R2dbcApplication.kt` as a standard Kotlin/Spring Boot application.

This example provides a clear understanding of integrating R2DBC into a Spring Boot WebFlux environment, highlighting its capabilities for building scalable and non-blocking data-driven applications with a reactive programming model.
