package simulations

import io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ActorSimulation: Simulation() {

    val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("*/*")

    val scn = scenario("Actor Simulation")
        .exec(
            http("Get Actor By Id")
                .get("/actors/1")
                .check(status().`is`(200))
        )
        .exec(
            http("Actor Search Last Name")
                .get("/actors?lastName=Depp")
                .check(status().`is`(200))
        )
        .exec(
            http("Actor Search First Name")
                .get("/actors?firstName=Angelina")
                .check(status().`is`(200))
        )


    init {
        setUp(
            scn.injectClosed(rampConcurrentUsers(10).to(200).during(10.seconds.toJavaDuration()))
            // scn.injectOpen(constantUsersPerSec(200.0).during(10.seconds.toJavaDuration()))
        ).protocols(httpProtocol)
    }
}
