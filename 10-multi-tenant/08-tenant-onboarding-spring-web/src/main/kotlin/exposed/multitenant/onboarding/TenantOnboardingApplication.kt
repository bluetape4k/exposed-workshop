package exposed.multitenant.onboarding

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
class TenantOnboardingApplication

fun main(args: Array<String>) {
    runApplication<TenantOnboardingApplication>(*args)
}
