package com.musinsa.payments.point.application.port.output.persistence

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import com.musinsa.payments.point.domain.valueobject.Money
import java.util.*

/**
 * 포인트 적립 영속성 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointAccumulationPersistencePort {
    
    /**
     * 포인트 적립 저장
     * @param accumulation 저장할 포인트 적립 엔티티
     * @return 저장된 포인트 적립 엔티티 (id 포함)
     */
    fun save(accumulation: PointAccumulation): PointAccumulation
    
    /**
     * 포인트 키로 조회
     * @param pointKey 포인트 키
     * @return 포인트 적립 엔티티 (없으면 empty)
     */
    fun findByPointKey(pointKey: String): Optional<PointAccumulation>
    
    /**
     * 회원 ID와 상태로 조회
     * @param memberId 회원 ID
     * @param status 적립 상태
     * @return 포인트 적립 엔티티 목록
     */
    fun findByMemberIdAndStatus(memberId: Long, status: PointAccumulationStatus): List<PointAccumulation>
    
    /**
     * 회원 ID로 사용 가능한 적립 건 조회
     * 상태가 ACCUMULATED이고 사용 가능 잔액이 있고 만료되지 않은 적립 건만 조회
     * 수기 지급 우선, 만료일 짧은 순으로 정렬
     * @param memberId 회원 ID
     * @return 포인트 적립 엔티티 목록
     */
    fun findAvailableAccumulationsByMemberId(memberId: Long): List<PointAccumulation>
    
    /**
     * 회원 ID의 사용 가능 금액 합계 조회
     * 상태가 ACCUMULATED이고 사용 가능 잔액이 있고 만료되지 않은 적립 건의 금액 합계
     * @param memberId 회원 ID
     * @return 사용 가능 금액 합계
     */
    fun sumAvailableAmountByMemberId(memberId: Long): Money
}

