package exposed.multitenant.onboarding.web

import exposed.multitenant.onboarding.model.OnboardTenantCommand
import exposed.multitenant.onboarding.model.TenantRecord
import exposed.multitenant.onboarding.service.TenantOnboardingService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tenants")
class TenantOnboardingController(
    private val service: TenantOnboardingService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun onboard(@RequestBody command: OnboardTenantCommand): TenantRecord =
        service.onboard(command)

    @GetMapping("/{tenantId}")
    fun find(@PathVariable tenantId: String): TenantRecord? =
        service.find(tenantId)
}
