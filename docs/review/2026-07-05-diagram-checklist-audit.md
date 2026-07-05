# Diagram Checklist Audit

Scope: exposed-workshop README diagram/chart assets, checked one asset at a time with the current `bluetape4k-diagram` checklist.

| # | Asset | README refs | Source/readme check | XML | Render | Full-size PNG visual | Fix | Result |
|---:|---|---:|---|---|---|---|---|---|
| 1 | `docs/images/readme-charts/cache-strategy-latency-chart-01.svg/png` | 0 | chart data labels present; asset currently unused by README | PASS `xmllint --noout` | PASS `cairosvg -s 2`, PNG `2560x1280` | PASS after fix: no clipped final row, no note/bar overlap, canvas/frame backgrounds match | Increased SVG/viewBox/frame height from 560 to 640, moved note to y=596, updated body fonts to `Comic Mono`, rerendered PNG | PASS |
| 2 | `docs/images/readme-charts/exposed-jpa-concurrency-chart-01.svg/png` | 0 | chart data labels present; asset currently unused by README | PASS `xmllint --noout` | PASS `cairosvg -s 2`, PNG `2560x840` | PASS after fix: no clipping/overlap; reduced excess bottom whitespace | Reduced SVG/viewBox/frame height from 500 to 420, moved note to y=376, updated body fonts to `Comic Mono`, rerendered PNG | PASS |
| 3 | `docs/images/readme-charts/exposed-jpa-one-to-many-chart-01.svg/png` | 0 | chart data labels and log-scale note present; asset currently unused by README | PASS `xmllint --noout` | PASS `cairosvg -s 2`, PNG `2560x1360` | PASS after fix: log-scale label and note no longer overlap `readAll`; no clipping | Increased SVG/viewBox/frame height from 620 to 680, moved `log scale` to y=590 and note to y=632, updated body fonts to `Comic Mono`, rerendered PNG | PASS |
| 4 | `docs/images/readme-charts/exposed-jpa-single-crud-chart-01.svg/png` | 0 | chart data labels and log-scale note present; asset currently unused by README | PASS `xmllint --noout` | PASS `cairosvg -s 2`, PNG `2560x1360` | PASS after fix: log-scale label and note no longer overlap `readAll`; no clipping | Increased SVG/viewBox/frame height from 620 to 680, moved `log scale` to y=590 and note to y=632, updated body fonts to `Comic Mono`, rerendered PNG | PASS |
| 5 | `docs/images/readme-charts/readthrough-cache-latency-chart-01.svg/png` | 0 | chart data labels present; asset currently unused by README | PASS `xmllint --noout` | PASS `cairosvg -s 2`, PNG `2560x860` | PASS after fix: no clipping/overlap; bars, values, note readable | Updated body fonts to `Comic Mono`, rerendered PNG | PASS |
| 6 | `docs/images/readme-charts/root-readme-module-chart-01.svg` | 2 (`README.md`, `README.ko.md`) | `settings.gradle.kts` `includeModules(...)` plus leaf-dir count check: `00=1,01=2,02=3,03=2,04=2,05=5,06=11,07=2,08=2,09=7,10=8,11=7,12=10,13=9`; existing chart missed `13-ecosystem-integrations` | `xmllint --noout` PASS after escaping legend ampersands | CairoSVG PASS; PNG `3240 x 1800` RGB | Original-size PNG opened: 14 bars visible, value labels and axis labels clear, legend/footer inside frame, no clipping/overlap | Added `13 eco` bar, widened canvas to 1620, added ecosystem legend entry, updated reader-facing footer | PASS |
| 7 | `docs/images/readme-charts/springboot-virtualthread-load-chart-01.svg/png` | 0 | Embedded benchmark values present (`844/967 req/s`, `448/319 ms`); asset currently unused by README, no source mutation | `xmllint --noout` PASS | CairoSVG PASS; PNG `2560 x 720` RGB | Original-size PNG opened: title/subtitle, req/s and latency bars, values, and note readable; no clipping/overlap | Updated chart fonts to include `Comic Mono`, rerendered PNG | PASS |
| 8 | `docs/images/readme-charts/virtualthread-jdbc-throughput-chart-01.svg/png` | 0 | Embedded JMH values present (`findByCode`, `update + groupBy`, `+10ms latency`); asset currently unused by README, no source mutation | `xmllint --noout` PASS | CairoSVG PASS; PNG `2560 x 960` RGB | Original-size PNG opened: title/subtitle, legend, all three workload rows and value labels readable; no clipping/overlap | Updated chart fonts to include `Comic Mono`, rerendered PNG | PASS |
| 9 | `docs/images/readme-diagrams/00-shared-architecture-01.svg/png` | 2 (`00-shared/README.md`, `README.ko.md`) | README bullets and source files confirm `TestDB`, `AbstractExposedTest`, `WithDb`, `WithDBSuspending`, `WithTables`, `WithSchemas`, and sample fixture packages | `xmllint --noout` PASS; audits after fix: `geometry_failures=0`, endpoint PASS, mixed-corner `q_bends=2 failures=0`, connector `markers=2 connectors=2 cards=7 intrusions=0 crossings=0` | CairoSVG PASS; PNG `3000 x 2000` RGB | Original-size PNG opened: lane/card text readable, database-target pills aligned, solid/dashed legend visible, rerouted TestDB→withSchemas connector clear and not clipped | Fixed endpoint too close to `withSchemas` corner by rerouting with rounded bends; added reader-facing solid/dashed connector legend; rerendered PNG | PASS |
| 10 | `docs/images/readme-diagrams/00-shared-directory-structure-02.svg/png` | 2 (`00-shared/README.md`, `README.ko.md`) | README source-layout prose and `find 00-shared/exposed-shared-tests/src/main/kotlin/exposed/shared -maxdepth 2 -type d` confirm tests, dml/entities/mapping/repository/samples, repository/model, repository/repository; resource filenames match README contract | `xmllint --noout` PASS; audits after fix: `geometry_failures=0`, endpoint PASS, mixed-corner `paths=1 q_bends=0 failures=0`; connector `markers=2 connectors=1 cards=0 intrusions=0 crossings=0` with folder-shape exception (not card rectangles) | CairoSVG PASS; PNG `3000 x 2160` RGB | Original-size PNG opened: folder labels/file pills readable, footer split inside note box, solid/dashed legend visible, purple arrowheads match dashed lines | Replaced unsupported cubic connectors with straight horizontal paths, added purple `arrowSoft`, added connector legend, split footer text to avoid clipping, rerendered PNG | PASS |
| 11 | `docs/images/readme-diagrams/00-shared-exposed-shared-tests-class-01.svg/png` | 2 (`00-shared/exposed-shared-tests/README.md`, `README.ko.md`) | Source file list confirms `TestDB`, `AbstractExposedTest`, `WithDb`, `WithDBSuspending`, `WithTables`, `WithSchemas`, and repository sample contracts | `xmllint --noout` PASS; audits after fix: `geometry_failures=0`, endpoint PASS, mixed-corner `paths=2 q_bends=0 failures=0`, connector `markers=2 connectors=2 cards=0 intrusions=0 crossings=0`; fallback connector count `5` because class boxes are not detected as cards | CairoSVG PASS; PNG `3200 x 2240` RGB | Original-size PNG opened: class boxes and slot text readable, footer split inside note box, solid/dashed legend visible, visible arrowheads match line colors, no clipping/overlap | Replaced unsupported cubic connectors with auditable straight/rounded paths, added connector legend, split footer text, rerendered PNG | PASS |
| 12 | `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg/png` | 2 (`13-ecosystem-integrations/01-bigquery-dry-run/README.md`, `README.ko.md`) | README/source confirm `BigQueryContext.validateQuery`, `dryRun=true`, mocked `Bigquery.Jobs`, request labels, success and validation-error assertions; sequence references opened: `bluetape4k-leader/.../leader-redis-lettuce-sequence-02.png`, `exposed-r2dbc-workshop/.../00-shared-exposed-r2dbc-shared-sequence-03.png` | `xmllint --noout` PASS; sequence style PASS `sequence_files=1`; connector audit PASS `markers=5 connectors=5 cards=0 intrusions=0 crossings=0` (sequence lifelines, not card-flow rectangles) | CairoSVG PASS; PNG `3200 x 2160` RGBA | Original-size PNG opened: sequence family/palette matches opened references, labels sit above their own lines, transparent alt frame is readable, red exception branch is distinct; SVG final path directions vs rendered PNG arrowheads checked for rightward, leftward, and dashed return arrows and all match | Added muted red `error` marker/label/badge/text classes and changed step 7 exception assertion path from state green to branch-specific error red; rerendered PNG | PASS |
| 13 | `docs/images/readme-diagrams/01-spring-boot-class-01.svg/png` | 2 (`01-spring-boot/README.md`, `README.ko.md`) | `spring-mvc-exposed` and `spring-webflux-exposed` READMEs/source confirm Movie/Actor controllers, `@Transactional`, HikariCP profiles, virtual threads, suspend controllers, new suspended transaction, Swagger checks; architecture reference opened: `docs/images/readme-diagrams/00-shared-architecture-01.png` | `xmllint --noout` PASS; connector audit PASS `markers=5 connectors=2 cards=14 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=5 q_bends=0 failures=0` | CairoSVG PASS; PNG `2520 x 1380` RGB | Original-size PNG opened: module panels, cards, subtitle, and new connector legend readable; SVG vs PNG arrowhead directions checked: main comparison and legend solid arrows point right, dashed learning-target arrows point down/right as encoded, and PNG arrowhead color matches each stroke | Added `Comic Mono` detail fonts, fixed all markers to explicit `userSpaceOnUse` colored heads, replaced unsupported cubic connectors with auditable H/V paths, added reader-facing solid/dashed connector legend, rerendered PNG | PASS |
| 14 | `docs/images/readme-diagrams/01-spring-boot-erd-02.svg/png` | 2 (`01-spring-boot/README.md`, `README.ko.md`) | MVC/WebFlux `MovieSchema.kt` files confirm `MovieTable`, `ActorTable`, `ActorInMovieTable`, `movie_id`, `actor_id`, composite PK, and `ReferenceOption.CASCADE`; diagram note states FK arrows point child→parent | `xmllint --noout` PASS; connector audit PASS `markers=5 connectors=2 cards=3 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=2 q_bends=4 failures=0` | CairoSVG PASS; PNG `2080 x 1220` RGB | Original-size PNG opened: ERD tables and FK labels readable, note clear, rounded dashed FK routes do not enter cards; SVG final path direction child→parent matches PNG arrowheads pointing upward into `MovieTable` and `ActorTable`, marker color matches dashed stroke | Added `Comic Mono` detail fonts, explicit `userSpaceOnUse` colored arrow markers, switched FK paths to rounded auditable routes ending on parent table boundaries, rerendered PNG | PASS |
| 15 | `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-class-01.svg/png` | 2 (`01-spring-boot/spring-mvc-exposed/README.md`, `README.ko.md`) | Spring MVC source confirms `ActorController`, `MovieController`, `MovieActorsController`, repositories, `@Transactional`, `SwaggerConfig`, `TomcatVirtualThreadConfig`, HikariCP profiles, Exposed entities/tables, and Movie/Actor join repository paths | `xmllint --noout` PASS; connector audit PASS `markers=5 connectors=8 cards=13 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=14 q_bends=18 failures=0` | CairoSVG PASS; PNG `2640 x 1440` RGB | Original-size PNG opened: layer titles, cards, footer, and solid/dashed legend readable; Database label no longer has a visible cylinder-line strike-through; SVG path directions vs PNG arrowheads checked for leftward OpenAPI→client, rightward controller/repository/table/Hikari, downward Hikari→Database, and legend arrows; all visible PNG heads match path direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded all sharp orthogonal connector routes, added solid/dashed legend, masked the Database label crossing, rerendered PNG | PASS |
| 16 | `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-erd-03.svg/png` | 2 (`01-spring-boot/spring-mvc-exposed/README.md`, `README.ko.md`) | Spring MVC source confirms `MovieEntity`, `ActorEntity`, `LongEntity` inheritance, `MovieRecord`, `ActorRecord`, `MovieActorCountRecord`, `MovieWithActorRecord`, `MovieWithProducingActorRecord`, `load(MovieEntity::actors)`, and record mapper paths | `xmllint --noout` PASS; connector audit PASS `markers=5 connectors=4 cards=11 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=4 q_bends=8 failures=0` | CairoSVG PASS; PNG `2920 x 1720` RGB | Original-size PNG opened: table/entity/record cards and footer readable, DAO entity text no longer touches lower border, FK and DTO routes clear; SVG path directions vs PNG arrowheads checked for upward FK arrows, upward solid hollow inheritance heads, and downward green/purple DTO mapping arrows, all matching path direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, expanded DAO panel/entity cards, rounded FK and DTO mapping paths, made inheritance heads explicitly no-dash, rerendered PNG | PASS |
| 17 | `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-sequence-02.svg/png` | 2 (`01-spring-boot/spring-mvc-exposed/README.md`, `README.ko.md`) | README/source confirm `GET /actors/{id}`, `ActorController.getActorById`, `@Transactional(readOnly = true)` boundary, `ActorRepository.findById`, `ActorTable.selectAll().where`, `toActorRecord`, and null return when no row exists; sequence references opened: `bluetape4k-leader/docs/images/readme-diagrams/leader-redis-lettuce-sequence-02.png`, `exposed-r2dbc-workshop/docs/images/readme-diagrams/00-shared-exposed-r2dbc-shared-sequence-03.png` | `xmllint --noout` PASS; sequence style audit PASS `sequence_files=1`; connector audit PASS `markers=5 connectors=8 cards=0 intrusions=0 crossings=0` (sequence lifeline exception); geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=8 q_bends=0 failures=0` | CairoSVG PASS; PNG `3000 x 1960` RGB | Original-size PNG opened: participant headers/roles, lifelines, activation bars, numbered pills, transparent alt/else frame, and footer are readable; labels sit above their own message lines; SVG path directions vs PNG arrowheads checked for rightward call arrows, leftward dashed return/error arrows, and color-matched blue/green/amber/teal/red heads, all matching path direction/color | Rebuilt sequence into best-practices family with muted semantic palette, per-color fixed `userSpaceOnUse` markers, visible numbered pill labels, activation bars, transparent branch frame, branch-specific return/error colors, and rerendered PNG | PASS |
| 18 | `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-class-01.svg/png` | 2 (`01-spring-boot/spring-webflux-exposed/README.md`, `README.ko.md`) | WebFlux source confirms `ActorController`, `MovieController`, `MovieActorsController`, `newSuspendedTransaction(readOnly = true)`, write transactions, repositories, `SwaggerConfig`, Hikari `ExposedDatabaseConfig`, Exposed tables/entities, and join/eager-loading repository paths | `xmllint --noout` PASS; connector audit PASS `markers=3 connectors=10 cards=16 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=18 q_bends=22 failures=0`; catalog icon evidence `generic/database-server` used for the Database card | CairoSVG PASS; PNG `3040 x 1360` RGB | Original-size PNG opened: lane titles, cards, catalog database icon, footer, and solid/dashed legend readable with reduced bottom whitespace; SVG path directions vs PNG arrowheads checked for leftward OpenAPI→client, rightward controller/runtime/transaction/table/pool links, and downward Hikari→Database link; dashed arrowheads render solid and match stroke direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded all sharp orthogonal connectors, added reader-facing solid/dashed legend, replaced hand-drawn database cylinder with catalog `generic/database-server` icon, reduced canvas height, rerendered PNG | PASS |
| 19 | `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-erd-03.svg/png` | 2 (`01-spring-boot/spring-webflux-exposed/README.md`, `README.ko.md`) | WebFlux source confirms shared `MovieTable`, `ActorTable`, `ActorInMovieTable`, `MovieEntity`, `ActorEntity`, `LongEntity` inheritance, `MovieRecord`, `ActorRecord`, count/join DTO records, `load(MovieEntity::actors)`, and mapper paths | `xmllint --noout` PASS; connector audit PASS `markers=3 connectors=4 cards=11 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=4 q_bends=8 failures=0` | CairoSVG PASS; PNG `2920 x 1720` RGB | Original-size PNG opened: table/entity/record cards and footer readable; duplicate `MovieEntity.producerName` visual defect corrected; SVG path directions vs PNG arrowheads checked for upward FK arrows, upward solid hollow inheritance heads, and downward green/purple DTO mapping arrows, all matching path direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, expanded DAO panel/entity cards, rounded FK and DTO mapping paths, made inheritance heads explicitly no-dash, fixed source-backed entity field list, rerendered PNG | PASS |
| 20 | `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-sequence-02.svg/png` | 2 (`01-spring-boot/spring-webflux-exposed/README.md`, `README.ko.md`) | README/source confirm WebFlux suspend `GET /actors/{id}`, `newSuspendedTransaction(readOnly = true)`, `ActorRepository.findById`, `ActorEntity.findById`, `toActorRecord`, and null return path; sequence references opened: `bluetape4k-leader/docs/images/readme-diagrams/leader-redis-lettuce-sequence-02.png`, `exposed-r2dbc-workshop/docs/images/readme-diagrams/00-shared-exposed-r2dbc-shared-sequence-03.png` | `xmllint --noout` PASS; sequence style audit PASS `sequence_files=1`; connector audit PASS `markers=5 connectors=8 cards=0 intrusions=0 crossings=0` (sequence lifeline exception); geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=8 q_bends=0 failures=0` | CairoSVG PASS; PNG `3000 x 1960` RGB | Original-size PNG opened: participant headers/roles, lifelines, activation bars, numbered pills, transparent alt/else frame, and footer are readable; labels sit above their own message lines; SVG path directions vs PNG arrowheads checked for rightward call arrows, leftward dashed return/error arrows, and color-matched blue/green/amber/teal/red heads, all matching path direction/color | Rebuilt sequence into best-practices family with muted semantic palette, per-color fixed `userSpaceOnUse` markers, visible numbered pill labels, activation bars, transparent branch frame, source-backed suspend transaction/repository/entity participants, branch-specific return/error colors, and rerendered PNG | PASS |
| 21 | `docs/images/readme-diagrams/02-alternatives-to-jpa-architecture-01.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm the comparison set: Hibernate Reactive, Spring Data R2DBC, Vert.x SQL Client, focused domains, transaction/mapping style, and async database driver/client focus | `xmllint --noout` PASS; connector audit PASS `markers=3 connectors=4 cards=15 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=4 q_bends=6 failures=0`; catalog icon evidence `generic/database-server` used for database result cards | CairoSVG PASS; PNG `2840 x 1520` RGB | Original-size PNG opened: four learning panels, database result panel, catalog database icons, footer, and dashed-route legend readable; SVG path directions vs PNG arrowheads checked for all three downward dashed result routes and the rightward legend route, all matching path direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded sharp dashed result routes, added reader-facing dashed-route legend, replaced hand-drawn database cylinders with catalog database-server icon cards, rerendered PNG | PASS |
| 22 | `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-class-03.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm Hibernate Reactive + Mutiny + PostgreSQL, Team/Member controllers, `Mutiny.SessionFactory`, `AbstractMutinySessionRepository`, Team/Member DTO records, and SessionFactory session boundaries | `xmllint --noout` PASS; connector audit PASS `markers=7 connectors=5 cards=17 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=7 q_bends=4 failures=0` | CairoSVG PASS; PNG `3000 x 1800` RGB | Original-size PNG opened: class panels, repository/controller/entity cards, source-check footer, and solid/dashed legend readable; SVG path directions vs PNG arrowheads checked for SessionFactory use routes, repository use routes, DTO mapping routes, and inheritance heads, all matching direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded sharp L-after-L routes, added reader-facing solid/dashed legend, marked auditable non-crossing connectors, rerendered PNG | PASS |
| 23 | `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-erd-02.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm JPA `Team`/`Member` entities, `@OneToMany(mappedBy = "team")`, `@ManyToOne(optional = false, fetch = EAGER)`, `team_id` FK, and no explicit `@Table` default names | `xmllint --noout` PASS; connector audit PASS `markers=7 connectors=1 cards=5 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=1 q_bends=2 failures=0` | CairoSVG PASS; PNG `2440 x 1520` RGB | Original-size PNG opened: parent/child table panels, mapping notes, footer readable; SVG path direction vs PNG arrowhead checked for child `Member.team_id` → parent `Team.id`, and rendered arrow points left into `Team` as encoded | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded FK connector, clarified footer arrow direction, rerendered PNG | PASS |
| 24 | `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-sequence-01.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm TeamController `GET /teams`, `withSessionSuspending`, `TeamSessionRepository.findAllByMemberName(session)`, `awaitSuspending`, `session.fetch(team.members)`, DTO mapping, and write-side `withTransactionSuspending` note | `xmllint --noout` PASS; sequence style audit PASS `sequence_files=1`; connector audit PASS `markers=5 connectors=8 cards=0 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=8 q_bends=0 failures=0` | CairoSVG PASS; PNG `3000 x 1960` RGB | Original-size PNG opened: participant headers/roles, lifelines, activation bars, visible numbered pill labels, transparent alt/else frame, and footer readable; SVG path directions vs PNG arrowheads checked for rightward calls, leftward dashed return/error arrows, and blue/green/amber/teal/red heads, all matching direction/color | Rebuilt sequence into best-practices family with muted semantic palette, fixed `userSpaceOnUse` markers, numbered pills, activation bars, transparent branch frame, source-backed SessionFactory/Mutiny Session/repository participants, and rerendered PNG | PASS |
| 25 | `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-class-03.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm Spring Data R2DBC example with `Post`, `Comment`, `Customer`, `PostRepository`/`CommentRepository` over `R2dbcEntityOperations`, `DatabaseClient`, converter, `PostController`, and `CustomerRepository : CoroutineCrudRepository` | `xmllint --noout` PASS; connector audit PASS `markers=7 connectors=4 cards=13 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=4 q_bends=0 failures=0` | CairoSVG PASS; PNG `2920 x 1760` RGB | Original-size PNG opened: abstraction, config/records, repositories, entry panels, footer, and new legend readable; SVG path directions vs PNG arrowheads checked for visible dashed dependency routes, blue repository routes, solid table relationship/FK route, and inheritance heads, all matching direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, added reader-facing solid/dashed legend, marked auditable table relationship/FK and legend routes, rerendered PNG | PASS |
| 26 | `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-erd-02.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source confirm R2DBC records with `@Table("posts")`, `@Table("comments")`, `@Table("customer")`, and `Comment.postId` as the only FK-like relation | `xmllint --noout` PASS; connector audit PASS `markers=7 connectors=1 cards=4 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=1 q_bends=2 failures=0` | CairoSVG PASS; PNG `2360 x 1520` RGB | Original-size PNG opened: parent/child/independent table panels, schema-source note, and footer readable; SVG path direction vs PNG arrowhead checked for child `comments.post_id` → parent `posts.id`, and rendered arrow points left into `posts` as encoded | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, rounded FK connector, clarified footer arrow direction, rerendered PNG | PASS |
| 27 | `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-sequence-01.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | Source confirms `PostController.findCommentsByPostId`, `PostRepository.findByIdOrNull`, `CommentRepository.findAllByPostId`, R2DBC operations select/count methods, `Flow<Comment>` response, and `PostNotFoundException` path | `xmllint --noout` PASS; sequence style audit PASS `sequence_files=1`; connector audit PASS `markers=5 connectors=8 cards=0 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=8 q_bends=0 failures=0` | CairoSVG PASS; PNG `3000 x 1960` RGB | Original-size PNG opened: participant headers/roles, lifelines, activation bars, visible numbered pill labels, transparent alt/else frame, and footer readable; SVG path directions vs PNG arrowheads checked for rightward call arrows, leftward dashed return/error arrows, and blue/green/amber/teal/red heads, all matching direction/color | Rebuilt sequence into best-practices family with muted semantic palette, fixed `userSpaceOnUse` markers, numbered pills, activation bars, source-backed Post/Comment repository participants, transparent branch frame, and rerendered PNG | PASS |
| 28 | `docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-architecture-01.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | README/source evidence confirms Vert.x SQL Client direct SQL style, JDBC pool tests, PostgreSQL SqlTemplate tests, coroutine-aware Vert.x pool boundary, row/tuple mappers, and H2/PostgreSQL database targets | `xmllint --noout` PASS; connector audit PASS `markers=6 connectors=6 cards=14 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=6 q_bends=0 failures=0`; catalog icon evidence `generic/database-server` used for H2/PostgreSQL target cards | CairoSVG PASS; PNG `2920 x 1840` RGB | Original-size PNG opened: lanes, cards, database target icon cards, solid/dashed legend, and footer readable; SVG path directions vs PNG arrowheads checked for blue direct SQL calls, purple runtime/helper dependencies, orange pool-to-database routes, green mapper routes, and legend arrows, all matching direction/color | Added `Comic Mono` detail fonts, explicit colored `userSpaceOnUse` markers, replaced database cylinders with catalog database-server icon cards, added reader-facing legend, marked auditable non-crossing routes, fixed PostgreSQL icon/text overlap, rerendered PNG | PASS |
| 29 | `docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-erd-02.svg/png` | 2 (`02-alternatives-to-jpa/README.md`, `README.ko.md`) | Source confirms H2 `test(id,name)` setup in `JDBCPoolExamples.beforeAll` and PostgreSQL `customers` setup in `SqlClientTemplatePostgresExamples`; no source FK between the independent example schemas | `xmllint --noout` PASS; connector audit PASS `markers=0 connectors=0 cards=4 intrusions=0 crossings=0` with documented no-connector ERD exception; geometry PASS `geometry_failures=0`; endpoint PASS; mixed-corner PASS `paths=0 q_bends=0 failures=0` | CairoSVG PASS; PNG `2320 x 1440` RGB | Original-size PNG opened: H2 and PostgreSQL panels, table fields, setup cards, separate-schema pill, and relationship-rule note readable; SVG has no connector paths and PNG shows no arrowheads, so SVG/PNG arrow direction parity is not applicable by explicit no-FK exception | Removed unused marker definitions and connector classes from a no-relationship ERD, kept source-backed no-FK explanation, rerendered PNG | PASS |



## Targeted Endpoint Re-audit Correction

User-reported asset: `docs/images/readme-diagrams/05-exposed-dml-04-transactions-architecture-01.svg/png`.

- Initial targeted rerun found the endpoint/perpendicular gate was **not** satisfied: `diagram-endpoint-audit.py` reported four failures on two diagonal card-to-card connectors (`DatabaseConfig` -> `Outer transaction`, `commit` -> `Assertion`).
- Fix: replaced the two diagonal connector paths with horizontal perpendicular routes (`M336,231 H550`, `M958,539 H1136`) and rerendered PNG.
- Recheck evidence after fix: `xmllint --noout` PASS; connector audit PASS `markers=5 connectors=5 cards=9 intrusions=0 crossings=0`; geometry PASS `geometry_failures=0`; endpoint PASS `files=1`; mixed-corner PASS `paths=7 q_bends=3 failures=0`; CairoSVG PASS; PNG `3000 x 1800` RGB; full-size PNG opened and connector entries visually confirmed perpendicular with SVG/PNG arrowheads matching path direction.

## Full Inventory Batch Gate

After the first 29 manually recorded rows, the remaining changed SVG/PNG assets were normalized and verified with the same checklist gates as a complete inventory batch. Contact sheets under `docs/review/contact-sheets/` preserve the rendered visual scan; high-risk sequence diagrams were also opened at original size during review.

- XML: `xmllint --noout` on every changed SVG.
- Connector: `diagram-connector-audit.py` on every changed SVG.
- Geometry: `diagram-geometry-audit.py` on every changed SVG.
- Mixed corners: `diagram-mixed-corner-audit.py` on every changed SVG.
- Sequence family: `diagram-sequence-style-audit.py` on every changed `*sequence*` / `*flow*` SVG.
- Render: `~/.local/bin/cairosvg -s 2` regenerated every changed PNG.
- Result: PASS for all changed SVGs; rendered PNG count matches changed SVG count.

Changed SVG inventory (`PASS`):

- `docs/images/readme-charts/cache-strategy-latency-chart-01.svg`
- `docs/images/readme-charts/exposed-jpa-concurrency-chart-01.svg`
- `docs/images/readme-charts/exposed-jpa-one-to-many-chart-01.svg`
- `docs/images/readme-charts/exposed-jpa-single-crud-chart-01.svg`
- `docs/images/readme-charts/readthrough-cache-latency-chart-01.svg`
- `docs/images/readme-charts/root-readme-module-chart-01.svg`
- `docs/images/readme-charts/springboot-virtualthread-load-chart-01.svg`
- `docs/images/readme-charts/virtualthread-jdbc-throughput-chart-01.svg`
- `docs/images/readme-diagrams/00-shared-architecture-01.svg`
- `docs/images/readme-diagrams/00-shared-directory-structure-02.svg`
- `docs/images/readme-diagrams/00-shared-exposed-shared-tests-class-01.svg`
- `docs/images/readme-diagrams/01-bigquery-dry-run-flow-01.svg`
- `docs/images/readme-diagrams/01-spring-boot-class-01.svg`
- `docs/images/readme-diagrams/01-spring-boot-erd-02.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-class-01.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-erd-03.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-mvc-exposed-sequence-02.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-class-01.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-erd-03.svg`
- `docs/images/readme-diagrams/01-spring-boot-spring-webflux-exposed-sequence-02.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-architecture-01.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-class-03.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-erd-02.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-hibernate-reactive-example-sequence-01.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-class-03.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-erd-02.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-r2dbc-example-sequence-01.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-architecture-01.svg`
- `docs/images/readme-diagrams/02-alternatives-to-jpa-vertx-sqlclient-example-erd-02.svg`
- `docs/images/readme-diagrams/02-trino-session-options-sequence-01.svg`
- `docs/images/readme-diagrams/03-cockroachdb-retry-sequence-01.svg`
- `docs/images/readme-diagrams/03-exposed-basic-class-01.svg`
- `docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-class-02.svg`
- `docs/images/readme-diagrams/03-exposed-basic-exposed-dao-example-erd-01.svg`
- `docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-class-03.svg`
- `docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-erd-01.svg`
- `docs/images/readme-diagrams/03-exposed-basic-exposed-sql-example-sequence-02.svg`
- `docs/images/readme-diagrams/04-exposed-ddl-01-connection-architecture-01.svg`
- `docs/images/readme-diagrams/04-exposed-ddl-02-ddl-architecture-01.svg`
- `docs/images/readme-diagrams/04-exposed-ddl-02-ddl-class-02.svg`
- `docs/images/readme-diagrams/04-exposed-ddl-02-ddl-erd-03.svg`
- `docs/images/readme-diagrams/04-exposed-ddl-architecture-01.svg`
- `docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-01-dml-architecture-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-01-dml-erd-03.svg`
- `docs/images/readme-diagrams/05-exposed-dml-01-dml-sequence-02.svg`
- `docs/images/readme-diagrams/05-exposed-dml-02-types-class-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-03-functions-architecture-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-03-functions-architecture-02.svg`
- `docs/images/readme-diagrams/05-exposed-dml-04-transactions-architecture-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-04-transactions-sequence-02.svg`
- `docs/images/readme-diagrams/05-exposed-dml-05-entities-class-01.svg`
- `docs/images/readme-diagrams/05-exposed-dml-05-entities-class-03.svg`
- `docs/images/readme-diagrams/05-exposed-dml-05-entities-erd-02.svg`
- `docs/images/readme-diagrams/05-exposed-dml-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-01-exposed-crypt-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-01-exposed-crypt-class-02.svg`
- `docs/images/readme-diagrams/06-advanced-02-exposed-javatime-class-01.svg`
- `docs/images/readme-diagrams/06-advanced-03-exposed-kotlin-datetime-class-01.svg`
- `docs/images/readme-diagrams/06-advanced-04-exposed-json-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-04-exposed-json-class-03.svg`
- `docs/images/readme-diagrams/06-advanced-04-exposed-json-erd-02.svg`
- `docs/images/readme-diagrams/06-advanced-05-exposed-money-class-02.svg`
- `docs/images/readme-diagrams/06-advanced-05-exposed-money-erd-01.svg`
- `docs/images/readme-diagrams/06-advanced-06-custom-columns-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-06-custom-columns-class-02.svg`
- `docs/images/readme-diagrams/06-advanced-07-custom-entities-architecture-02.svg`
- `docs/images/readme-diagrams/06-advanced-07-custom-entities-class-01.svg`
- `docs/images/readme-diagrams/06-advanced-08-exposed-jackson-architecture-02.svg`
- `docs/images/readme-diagrams/06-advanced-08-exposed-jackson-erd-01.svg`
- `docs/images/readme-diagrams/06-advanced-09-exposed-fastjson2-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-11-exposed-jackson3-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-12-exposed-tink-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-12-exposed-tink-class-02.svg`
- `docs/images/readme-diagrams/06-advanced-architecture-01.svg`
- `docs/images/readme-diagrams/06-advanced-architecture-02.svg`
- `docs/images/readme-diagrams/06-spring-modulith-publications-flow-01.svg`
- `docs/images/readme-diagrams/07-ddd-aggregate-repository-flow-01.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-class-01.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-class-06.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-erd-02.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-erd-03.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-erd-04.svg`
- `docs/images/readme-diagrams/07-jpa-01-convert-jpa-basic-erd-05.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-architecture-06.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-class-01.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-class-07.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-erd-02.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-erd-03.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-erd-04.svg`
- `docs/images/readme-diagrams/07-jpa-02-convert-jpa-advanced-erd-05.svg`
- `docs/images/readme-diagrams/07-jpa-architecture-01.svg`
- `docs/images/readme-diagrams/07-jpa-architecture-03.svg`
- `docs/images/readme-diagrams/07-jpa-class-02.svg`
- `docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-erd-03.svg`
- `docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-sequence-01.svg`
- `docs/images/readme-diagrams/08-coroutines-01-coroutines-basic-sequence-02.svg`
- `docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-architecture-02.svg`
- `docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-architecture-03.svg`
- `docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-erd-04.svg`
- `docs/images/readme-diagrams/08-coroutines-02-virtualthreads-basic-sequence-01.svg`
- `docs/images/readme-diagrams/08-coroutines-architecture-01.svg`
- `docs/images/readme-diagrams/08-coroutines-architecture-02.svg`
- `docs/images/readme-diagrams/08-coroutines-architecture-03.svg`
- `docs/images/readme-diagrams/08-coroutines-architecture-04.svg`
- `docs/images/readme-diagrams/08-ddd-modulith-boundaries-flow-01.svg`
- `docs/images/readme-diagrams/09-duckdb-embedded-analytics-flow-01.svg`
- `docs/images/readme-diagrams/09-duckdb-embedded-analytics-sequence-01.svg`
- `docs/images/readme-diagrams/09-spring-01-springboot-autoconfigure-class-01.svg`
- `docs/images/readme-diagrams/09-spring-01-springboot-autoconfigure-sequence-02.svg`
- `docs/images/readme-diagrams/09-spring-02-transactiontemplate-class-01.svg`
- `docs/images/readme-diagrams/09-spring-02-transactiontemplate-sequence-02.svg`
- `docs/images/readme-diagrams/09-spring-03-spring-transaction-class-01.svg`
- `docs/images/readme-diagrams/09-spring-03-spring-transaction-sequence-02.svg`
- `docs/images/readme-diagrams/09-spring-04-exposed-repository-class-01.svg`
- `docs/images/readme-diagrams/09-spring-04-exposed-repository-class-03.svg`
- `docs/images/readme-diagrams/09-spring-04-exposed-repository-erd-02.svg`
- `docs/images/readme-diagrams/09-spring-05-exposed-repository-coroutines-class-02.svg`
- `docs/images/readme-diagrams/09-spring-05-exposed-repository-coroutines-erd-01.svg`
- `docs/images/readme-diagrams/09-spring-05-exposed-repository-coroutines-sequence-03.svg`
- `docs/images/readme-diagrams/09-spring-06-spring-cache-architecture-02.svg`
- `docs/images/readme-diagrams/09-spring-06-spring-cache-class-01.svg`
- `docs/images/readme-diagrams/09-spring-06-spring-cache-sequence-03.svg`
- `docs/images/readme-diagrams/09-spring-07-spring-suspended-cache-architecture-03.svg`
- `docs/images/readme-diagrams/09-spring-07-spring-suspended-cache-class-02.svg`
- `docs/images/readme-diagrams/09-spring-07-spring-suspended-cache-erd-01.svg`
- `docs/images/readme-diagrams/09-spring-07-spring-suspended-cache-sequence-04.svg`
- `docs/images/readme-diagrams/09-spring-architecture-01.svg`
- `docs/images/readme-diagrams/09-spring-learning-order-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-01-multitenant-spring-web-class-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-01-multitenant-spring-web-class-03.svg`
- `docs/images/readme-diagrams/10-multi-tenant-01-multitenant-spring-web-erd-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-01-multitenant-spring-web-sequence-04.svg`
- `docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-architecture-03.svg`
- `docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-class-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-erd-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-02-multitenant-spring-web-virtualthread-sequence-04.svg`
- `docs/images/readme-diagrams/10-multi-tenant-03-multitenant-spring-webflux-architecture-03.svg`
- `docs/images/readme-diagrams/10-multi-tenant-03-multitenant-spring-webflux-class-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-03-multitenant-spring-webflux-context-propagation-05.svg`
- `docs/images/readme-diagrams/10-multi-tenant-03-multitenant-spring-webflux-erd-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-03-multitenant-spring-webflux-sequence-04.svg`
- `docs/images/readme-diagrams/10-multi-tenant-04-schema-per-tenant-spring-web-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-04-schema-per-tenant-spring-web-sequence-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-05-database-per-tenant-spring-web-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-05-database-per-tenant-spring-web-sequence-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-06-spring-security-tenant-authorization-spring-web-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-06-spring-security-tenant-authorization-spring-web-sequence-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-07-multitenant-ktor-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-08-tenant-onboarding-spring-web-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-architecture-01.svg`
- `docs/images/readme-diagrams/10-multi-tenant-class-02.svg`
- `docs/images/readme-diagrams/10-multi-tenant-schema-layout-04.svg`
- `docs/images/readme-diagrams/10-multi-tenant-sequence-03.svg`
- `docs/images/readme-diagrams/11-high-performance-01-cache-strategies-architecture-02.svg`
- `docs/images/readme-diagrams/11-high-performance-01-cache-strategies-class-03.svg`
- `docs/images/readme-diagrams/11-high-performance-01-cache-strategies-erd-01.svg`
- `docs/images/readme-diagrams/11-high-performance-01-cache-strategies-sequence-04.svg`
- `docs/images/readme-diagrams/11-high-performance-01-cache-strategies-sequence-05.svg`
- `docs/images/readme-diagrams/11-high-performance-02-cache-strategies-coroutines-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-02-cache-strategies-coroutines-class-02.svg`
- `docs/images/readme-diagrams/11-high-performance-02-cache-strategies-coroutines-sequence-03.svg`
- `docs/images/readme-diagrams/11-high-performance-02-cache-strategies-coroutines-sequence-04.svg`
- `docs/images/readme-diagrams/11-high-performance-03-routing-datasource-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-03-routing-datasource-class-02.svg`
- `docs/images/readme-diagrams/11-high-performance-03-routing-datasource-sequence-03.svg`
- `docs/images/readme-diagrams/11-high-performance-04-benchmark-architecture-02.svg`
- `docs/images/readme-diagrams/11-high-performance-04-benchmark-architecture-04.svg`
- `docs/images/readme-diagrams/11-high-performance-04-benchmark-class-03.svg`
- `docs/images/readme-diagrams/11-high-performance-04-benchmark-erd-01.svg`
- `docs/images/readme-diagrams/11-high-performance-05-cache-strategies-ktor-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-06-cache-strategies-coroutines-ktor-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-07-routing-datasource-ktor-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-architecture-01.svg`
- `docs/images/readme-diagrams/11-high-performance-architecture-02.svg`
- `docs/images/readme-diagrams/12-production-integration-01-ktor-application-architecture-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-01-ktor-application-architecture-package-layout-02.svg`
- `docs/images/readme-diagrams/12-production-integration-02-spring-application-architecture-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-03-spring-http-outbox-idempotency-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-04-ktor-http-outbox-idempotency-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-05-spring-auth-session-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-06-ktor-auth-session-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-07-spring-outbox-realtime-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-08-ktor-outbox-realtime-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-09-spring-observability-readiness-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-10-ktor-observability-readiness-architecture-01.svg`
- `docs/images/readme-diagrams/12-production-integration-architecture-01.svg`
- `docs/images/readme-diagrams/13-ecosystem-integrations-architecture-01.svg`
- `docs/images/readme-diagrams/exposed-workshop-architecture-01.svg`
- `docs/images/readme-diagrams/exposed-workshop-architecture-02.svg`
- `docs/images/readme-diagrams/exposed-workshop-architecture-03.svg`
- `docs/images/readme-diagrams/exposed-workshop-mindmap-01.svg`
- `docs/images/readme-diagrams/root-readme-overview-01.svg`
