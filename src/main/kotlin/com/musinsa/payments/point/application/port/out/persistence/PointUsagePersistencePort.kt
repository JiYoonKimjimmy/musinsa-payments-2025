package com.musinsa.payments.point.application.port.out.persistence

import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.domain.valueobject.OrderNumber
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * 포인트 사용 영속성 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointUsagePersistencePort {
    
    /**
     * 포인트 사용 저장
     * @param usage 저장할 포인트 사용 엔티티
     * @return 저장된 포인트 사용 엔티티 (id 포함)
     */
    fun save(usage: PointUsage): PointUsage
    
    /**
     * 포인트 키로 조회
     * @param pointKey 포인트 키
     * @return 포인트 사용 엔티티 (없으면 empty)
     */
    fun findByPointKey(pointKey: String): Optional<PointUsage>
    
    /**
     * 회원 ID와 주문번호로 조회
     * @param memberId 회원 ID
     * @param orderNumber 주문번호
     * @return 포인트 사용 엔티티 목록
     */
    fun findByMemberIdAndOrderNumber(memberId: Long, orderNumber: OrderNumber): List<PointUsage>
    
    /**
     * 회원 ID로 사용 내역 조회 (페이징)
     * 주문번호 필터링 옵션 포함
     * @param memberId 회원 ID
     * @param orderNumber 주문번호 (null이면 전체 조회)
     * @param pageable 페이징 정보
     * @return 포인트 사용 엔티티 페이지
     */
    fun findUsageHistoryByMemberId(
        memberId: Long,
        orderNumber: String?,
        pageable: Pageable
    ): Page<PointUsage>
}

