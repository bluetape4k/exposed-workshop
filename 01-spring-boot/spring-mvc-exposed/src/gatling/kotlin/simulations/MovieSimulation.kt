package simulations

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MovieSimulation: Simulation() {

    val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("*/*")

    val scn = scenario("Movie Simulation")
        .exec(
            http("Get Movie By Id")
                .get("/movies/1")
                .check(status().`is`(200))
        )
        .exec(
            http("Search Movie by Producer Name")
                .get("/movies?producerName=Johnny")
                .check(status().`is`(200))
        )

    init {
        setUp(
            scn.injectClosed(CoreDsl.rampConcurrentUsers(10).to(20).during(10.seconds.toJavaDuration()))
            // scn.injectOpen(CoreDsl.constantUsersPerSec(400.0).during(10.seconds.toJavaDuration()))
        ).protocols(httpProtocol)
    }
}
