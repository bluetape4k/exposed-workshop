# 01 Spring Boot: Spring MVC with Exposed

This module (
`spring-mvc-exposed`) demonstrates how to build a Spring Boot application using the Spring MVC framework, with Exposed as the Object-Relational Mapping (ORM) library for database interactions. It provides a practical example of managing a movie database, including actors, movies, and their many-to-many relationships, exposed through RESTful endpoints.

## Key Features and Components:

### 1. Spring Boot Application Core (`SpringMvcApplication.kt`)

- Standard Spring Boot entry point, configured as a `SERVLET` web application.

### 2. Domain Model (`exposed.workshop.springmvc.domain.model` package)

- **Movie Schema (`MovieSchema.kt`)**: Defines the database tables for the movie domain:
    - `MovieTable`: Stores movie details such as `name`, `producerName`, and `releaseDate`.
    - `ActorTable`: Stores actor details including `firstName`, `lastName`, and `birthday`.
    - `ActorInMovieTable`: A join table establishing a many-to-many relationship between movies and actors.
- **Exposed DAO Entities**: `MovieEntity` and
  `ActorEntity` classes provide a clean, object-oriented interface for interacting with the `MovieTable` and
  `ActorTable` respectively, including navigation properties for the many-to-many relationship.
- **Data Transfer Objects (`MovieRecords.kt`)**: Contains data classes like `ActorRecord` and
  `MovieWithActorRecord` for representing and transferring movie and actor data within the application.
- **Mappers (`MovieMappers.kt`)**: Provides utility functions for mapping between Exposed
  `ResultRow` objects and the domain-specific data classes.

### 3. Data Initialization (`DataInitializer.kt`)

- Implements
  `ApplicationListener<ApplicationReadyEvent>` to populate the database with sample movie and actor data when the application starts.
- Utilizes Exposed's `batchInsert` and
  `insert` operations to efficiently add initial data, demonstrating data seeding techniques.

### 4. RESTful API Controllers (`exposed.workshop.springmvc.controller` package)

- **`MovieController.kt`
  **: Handles HTTP requests related to movie operations (e.g., fetching all movies, fetching a single movie).
- **`ActorController.kt`**: Manages HTTP requests for actor-related operations (e.g., retrieving actor details).
- **`MovieActorsController.kt`**: Provides endpoints for managing the relationships between movies and actors.
- **`IndexController.kt`**: Serves the application's root or home page.

## Getting Started:

This module provides a fully functional Spring MVC application showcasing database operations with Exposed. To run this application, you would typically:

1. Configure your database connection in `application.properties` or `application.yml` (located in
   `src/main/resources`).
2. Build the project using Gradle.
3. Run the `SpringMvcApplication.kt` as a standard Kotlin/Spring Boot application.

This setup offers a clear blueprint for integrating Exposed into a robust Spring Boot MVC environment.
