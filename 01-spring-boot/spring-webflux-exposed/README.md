# 01 Spring Boot: Spring WebFlux with Exposed

This module (
`spring-webflux-exposed`) demonstrates how to build a reactive Spring Boot application using the Spring WebFlux framework, with Exposed as the Object-Relational Mapping (ORM) library for asynchronous database interactions. It provides a practical example of managing a movie database, including actors, movies, and their many-to-many relationships, exposed through reactive RESTful endpoints.

## Key Features and Components:

### 1. Spring Boot Application Core (`SpringWebfluxApplication.kt`)

- Standard Spring Boot entry point, configured as a `REACTIVE` web application.

### 2. Domain Model (`exposed.workshop.springwebflux.domain.model` package)

- **Movie Schema (`MovieSchema.kt`)
  **: Defines the database tables for the movie domain (identical to the Spring MVC example):
    - `MovieTable`: Stores movie details such as `name`, `producerName`, and `releaseDate`.
    - `ActorTable`: Stores actor details including `firstName`, `lastName`, and `birthday`.
    - `ActorInMovieTable`: A join table establishing a many-to-many relationship between movies and actors.
- **Exposed DAO Entities**: `MovieEntity` and
  `ActorEntity` classes provide an object-oriented interface for interacting with the `MovieTable` and
  `ActorTable` respectively, including navigation properties for the many-to-many relationship.
- **Data Transfer Objects (`MovieRecords.kt`)**: Contains data classes like `ActorRecord` and
  `MovieWithActorRecord` for representing and transferring movie and actor data within the application.
- **Mappers (`MovieMappers.kt`)**: Provides utility functions for mapping between Exposed
  `ResultRow` objects and the domain-specific data classes.

### 3. Reactive Data Access (`exposed.workshop.springwebflux.domain.repository` package)

- **`MovieRepository.kt`
  **: Handles asynchronous database operations for movies using Exposed, returning reactive types (e.g., `Mono`,
  `Flux`).
- **`ActorRepository.kt`**: Manages asynchronous database operations for actors using Exposed, returning reactive types.

### 4. Reactive RESTful API Controllers (`exposed.workshop.springwebflux.controller` package)

- **`MovieController.kt`**: Handles reactive HTTP requests related to movie operations.
- **`ActorController.kt`**: Manages reactive HTTP requests for actor-related operations.
- **`MovieActorsController.kt`**: Provides reactive endpoints for managing the relationships between movies and actors.
- **`IndexController.kt`**: Serves the application's root or home page.

### 5. Utility Classes (`exposed.workshop.springwebflux.utils` package)

- Contains general utility functions supporting the WebFlux application.

## Getting Started:

This module provides a fully functional Spring WebFlux application showcasing asynchronous database operations with Exposed. To run this application, you would typically:

1. Configure your database connection in `application.properties` or `application.yml` (located in
   `src/main/resources`).
2. Build the project using Gradle.
3. Run the `SpringWebfluxApplication.kt` as a standard Kotlin/Spring Boot application.

This setup offers a clear blueprint for integrating Exposed into a robust Spring Boot WebFlux environment, leveraging reactive programming for efficient, non-blocking operations.
