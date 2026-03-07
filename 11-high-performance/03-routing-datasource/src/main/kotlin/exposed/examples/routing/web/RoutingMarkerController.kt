package exposed.examples.routing.web

import exposed.examples.routing.context.TenantContext
import exposed.examples.routing.domain.RoutingMarkerRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 현재 요청 컨텍스트가 어떤 데이터소스로 라우팅되는지 확인하는 API입니다.
 */
@RestController
@RequestMapping("/routing")
class RoutingMarkerController(
    private val markerRepository: RoutingMarkerRepository,
) {

    /**
     * read-write 트랜잭션 경로에서 마커를 조회합니다.
     */
    @GetMapping("/marker")
    @Transactional(readOnly = false)
    fun getReadWriteMarker(): RoutingMarkerResponse =
        RoutingMarkerResponse(
            tenant = TenantContext.currentTenant() ?: "default",
            readOnly = false,
            marker = markerRepository.findCurrentMarker(),
        )

    /**
     * read-only 트랜잭션 경로에서 마커를 조회합니다.
     */
    @GetMapping("/marker/readonly")
    @Transactional(readOnly = true)
    fun getReadOnlyMarker(): RoutingMarkerResponse =
        RoutingMarkerResponse(
            tenant = TenantContext.currentTenant() ?: "default",
            readOnly = true,
            marker = markerRepository.findCurrentMarker(),
        )

    /**
     * 현재 read-write 라우팅 대상의 마커 값을 갱신합니다.
     */
    @PatchMapping("/marker")
    @Transactional(readOnly = false)
    fun updateMarker(@RequestBody request: UpdateMarkerRequest): RoutingMarkerResponse {
        markerRepository.resetAndInsert(request.marker)
        return getReadWriteMarker()
    }
}

/**
 * 라우팅 결과 조회 응답 모델입니다.
 */
data class RoutingMarkerResponse(
    /** 요청 처리에 사용된 tenant 식별자입니다. */
    val tenant: String,
    /** 라우팅 대상이 read-only 경로였는지 여부입니다. */
    val readOnly: Boolean,
    /** 선택된 데이터소스에 저장된 현재 마커 값입니다. */
    val marker: String?,
)

/**
 * 마커 갱신 요청 모델입니다.
 */
data class UpdateMarkerRequest(
    /** read-write 라우팅 대상에 저장할 새 마커 값입니다. */
    val marker: String,
)
