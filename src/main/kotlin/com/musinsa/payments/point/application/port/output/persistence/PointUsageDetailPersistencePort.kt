package com.musinsa.payments.point.application.port.output.persistence

import com.musinsa.payments.point.domain.entity.PointUsageDetail

/**
 * 포인트 사용 상세 영속성 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointUsageDetailPersistencePort {
    
    /**
     * 포인트 사용 상세 일괄 저장
     * @param details 저장할 포인트 사용 상세 엔티티 목록
     * @return 저장된 포인트 사용 상세 엔티티 목록 (id 포함)
     */
    fun saveAll(details: List<PointUsageDetail>): List<PointUsageDetail>
    
    /**
     * 포인트 사용 키로 조회
     * @param pointKey 포인트 사용 키
     * @return 포인트 사용 상세 엔티티 목록
     */
    fun findByUsagePointKey(pointKey: String): List<PointUsageDetail>
    
    /**
     * 포인트 적립 키로 조회
     * @param pointKey 포인트 적립 키
     * @return 포인트 사용 상세 엔티티 목록
     */
    fun findByAccumulationPointKey(pointKey: String): List<PointUsageDetail>

    /**
     * 테스트 헬퍼: 저장된 모든 상세 내역 조회
     * @return 모든 포인트 사용 상세 엔티티 목록
     */
    fun findAll(): List<PointUsageDetail>

    /**
     * 테스트 헬퍼: ID로 상세 내역 삭제
     * @param id 삭제할 상세 내역 ID
     */
    fun deleteById(id: Long)
}

