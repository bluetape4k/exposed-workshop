# 02 Alternatives to JPA: Hibernate Reactive Example

This module (
`hibernate-reactive-example`) demonstrates how to build a reactive Spring Boot application using Hibernate Reactive as the Object-Relational Mapping (ORM) solution. It serves as a practical example for those looking into alternatives to traditional JPA or Exposed for reactive and non-blocking database interactions. The module manages a simple "Team" database, including teams and their members, exposed through reactive RESTful endpoints.

## Key Features and Components:

### 1. Spring Boot Application Core (`HibernateReactiveApplication.kt`)

- Standard Spring Boot entry point, configured as a
  `REACTIVE` web application, likely leveraging Spring WebFlux for its web layer.

### 2. Domain Model (`alternatives.hibernate.reactive.example.domain.model` package)

- **Hibernate Reactive Entities (`Team.kt`, `Member.kt`)
  **: Defines the database entities using Jakarta Persistence annotations (`@Entity`, `@Id`, `@GeneratedValue`,
  `@OneToMany`, `@ManyToOne`).
    - `Team`: Represents a team with an `id` and `name`, having a one-to-many relationship with `Member` entities.
    - `Member`: Represents a team member with an `id`, `name`, `age`, and a many-to-one relationship with a `Team`.
- **Relationships**: Establishes a standard parent-child relationship between `Team` and
  `Member` entities, demonstrating how to define and manage these relationships with Hibernate Reactive.

### 3. Data Access Layer (`alternatives.hibernate.reactive.example.domain.repository` package)

- Contains repository interfaces or classes (e.g., `TeamRepository.kt`,
  `MemberRepository.kt`) responsible for performing asynchronous database operations on `Team` and
  `Member` entities using Hibernate Reactive.

### 4. RESTful API Controllers (`alternatives.hibernate.reactive.example.domain.controller` package)

- **`TeamController.kt`
  **: Handles reactive HTTP requests for managing team-related operations (e.g., fetching teams, adding new teams).
- **`MemberController.kt`
  **: Manages reactive HTTP requests for member-related operations (e.g., retrieving members, associating members with teams).
- **`IndexController.kt`**: Serves the application's root or home page.

### 5. Other Components (`alternatives.hibernate.reactive.example.domain.dto`, `mapper`, `utils` packages)

- **DTOs**: Data Transfer Objects for efficient data exchange.
- **Mappers**: Utility classes for mapping between entities and DTOs.
- **Utils**: General utility functions supporting the application.

## Getting Started:

This module provides a fully functional Spring Boot WebFlux application showcasing reactive database operations with Hibernate Reactive. To run this application, you would typically:

1. Configure your database connection in `application.properties` or `application.yml` (located in
   `src/main/resources`).
2. Build the project using Gradle.
3. Run the `HibernateReactiveApplication.kt` as a standard Kotlin/Spring Boot application.

This example provides a clear understanding of integrating Hibernate Reactive into a Spring Boot WebFlux environment, highlighting its capabilities for building scalable and non-blocking data-driven applications.
