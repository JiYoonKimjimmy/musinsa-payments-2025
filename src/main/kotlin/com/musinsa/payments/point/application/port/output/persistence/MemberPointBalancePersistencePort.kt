package com.musinsa.payments.point.application.port.output.persistence

import com.musinsa.payments.point.domain.entity.MemberPointBalance
import java.util.*

/**
 * 회원 포인트 잔액 영속성 포트 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface MemberPointBalancePersistencePort {
    
    /**
     * 회원 ID로 잔액 조회
     * @param memberId 회원 ID
     * @return 회원 포인트 잔액 (없으면 empty)
     */
    fun findByMemberId(memberId: Long): Optional<MemberPointBalance>
    
    /**
     * 회원 ID로 잔액 조회 (비관적 락 적용)
     * @param memberId 회원 ID
     * @return 회원 포인트 잔액 (없으면 empty)
     */
    fun findByMemberIdWithLock(memberId: Long): Optional<MemberPointBalance>
    
    /**
     * 회원 포인트 잔액 저장
     * @param balance 저장할 회원 포인트 잔액
     * @return 저장된 회원 포인트 잔액
     */
    fun save(balance: MemberPointBalance): MemberPointBalance
    
    /**
     * 모든 회원 포인트 잔액 조회
     * @return 회원 포인트 잔액 목록
     */
    fun findAll(): List<MemberPointBalance>
    
    /**
     * 여러 회원의 잔액 조회
     * @param memberIds 회원 ID 목록
     * @return 회원 포인트 잔액 목록
     */
    fun findByMemberIds(memberIds: List<Long>): List<MemberPointBalance>
}

