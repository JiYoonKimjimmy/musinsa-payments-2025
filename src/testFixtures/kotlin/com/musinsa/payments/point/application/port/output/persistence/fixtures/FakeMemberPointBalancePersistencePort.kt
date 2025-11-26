package com.musinsa.payments.point.application.port.output.persistence.fixtures

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 회원 포인트 잔액 영속성 포트의 Fake 구현체
 * 테스트에서 메모리 기반 저장소를 사용합니다.
 */
class FakeMemberPointBalancePersistencePort : MemberPointBalancePersistencePort {
    
    private val storage = ConcurrentHashMap<Long, MemberPointBalance>()
    
    override fun findByMemberId(memberId: Long): Optional<MemberPointBalance> {
        return Optional.ofNullable(storage[memberId])
    }
    
    override fun findByMemberIdWithLock(memberId: Long): Optional<MemberPointBalance> {
        // Fake 구현에서는 락 없이 조회
        return findByMemberId(memberId)
    }
    
    override fun save(balance: MemberPointBalance): MemberPointBalance {
        storage[balance.memberId] = balance
        return balance
    }
    
    override fun findAll(): List<MemberPointBalance> {
        return storage.values.toList()
    }
    
    override fun findByMemberIds(memberIds: List<Long>): List<MemberPointBalance> {
        return memberIds.mapNotNull { storage[it] }
    }
    
    /**
     * 저장소 초기화
     */
    fun clear() {
        storage.clear()
    }
    
    /**
     * 저장된 잔액 개수 조회
     */
    fun count(): Int {
        return storage.size
    }
}

