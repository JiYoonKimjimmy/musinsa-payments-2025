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
     * 포인트 적립 일괄 저장
     * 배치 처리를 통해 성능을 최적화합니다.
     * @param accumulations 저장할 포인트 적립 엔티티 목록
     * @return 저장된 포인트 적립 엔티티 목록 (id 포함)
     */
    fun saveAll(accumulations: List<PointAccumulation>): List<PointAccumulation>
    
    /**
     * ID로 조회
     * @param id 포인트 적립 ID
     * @return 포인트 적립 엔티티 (없으면 empty)
     */
    fun findById(id: Long): Optional<PointAccumulation>
    
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

    /**
     * ID로 조회 (비관적 락 적용)
     * @param id 포인트 적립 ID
     * @return 포인트 적립 엔티티 (없으면 empty)
     */
    fun findByIdWithLock(id: Long): Optional<PointAccumulation>

    /**
     * ID 목록으로 조회 (비관적 락 적용)
     * N+1 문제를 방지하기 위한 배치 조회 메서드입니다.
     * @param ids 포인트 적립 ID 목록
     * @return 포인트 적립 엔티티 맵 (ID를 키로 사용)
     */
    fun findByIdsWithLock(ids: List<Long>): Map<Long, PointAccumulation>

    /**
     * 회원 ID로 사용 가능한 적립 건 조회 (비관적 락 적용)
     * 상태가 ACCUMULATED이고 사용 가능 잔액이 있고 만료되지 않은 적립 건만 조회
     * 수기 지급 우선, 만료일 짧은 순으로 정렬
     * @param memberId 회원 ID
     * @return 포인트 적립 엔티티 목록
     */
    fun findAvailableAccumulationsByMemberIdWithLock(memberId: Long): List<PointAccumulation>

    /**
     * 테스트 헬퍼: 저장된 모든 적립 건 조회
     * @return 모든 포인트 적립 엔티티 목록
     */
    fun findAll(): List<PointAccumulation>

    /**
     * 테스트 헬퍼: ID로 적립 건 삭제
     * @param id 삭제할 적립 건 ID
     */
    fun deleteById(id: Long)
}

