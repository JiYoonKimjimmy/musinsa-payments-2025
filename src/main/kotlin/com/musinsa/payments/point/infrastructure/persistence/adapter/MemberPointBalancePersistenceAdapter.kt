package com.musinsa.payments.point.infrastructure.persistence.adapter

import com.musinsa.payments.point.application.port.output.persistence.MemberPointBalancePersistencePort
import com.musinsa.payments.point.domain.entity.MemberPointBalance
import com.musinsa.payments.point.infrastructure.persistence.jpa.mapper.PointEntityMapper
import com.musinsa.payments.point.infrastructure.persistence.jpa.repository.MemberPointBalanceJpaRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * 회원 포인트 잔액 영속성 어댑터
 * Infrastructure 레이어에서 MemberPointBalancePersistencePort 인터페이스를 구현합니다.
 */
@Component
class MemberPointBalancePersistenceAdapter(
    private val memberPointBalanceJpaRepository: MemberPointBalanceJpaRepository,
    private val pointEntityMapper: PointEntityMapper
) : MemberPointBalancePersistencePort {
    
    override fun findByMemberId(memberId: Long): Optional<MemberPointBalance> {
        return memberPointBalanceJpaRepository.findByMemberId(memberId)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findByMemberIdWithLock(memberId: Long): Optional<MemberPointBalance> {
        return memberPointBalanceJpaRepository.findByMemberIdWithLock(memberId)
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun save(balance: MemberPointBalance): MemberPointBalance {
        val entity = pointEntityMapper.toEntity(balance)
        val savedEntity = memberPointBalanceJpaRepository.save(entity)
        return pointEntityMapper.toDomain(savedEntity)
    }
    
    override fun findAll(): List<MemberPointBalance> {
        return memberPointBalanceJpaRepository.findAll()
            .map { pointEntityMapper.toDomain(it) }
    }
    
    override fun findByMemberIds(memberIds: List<Long>): List<MemberPointBalance> {
        return memberPointBalanceJpaRepository.findByMemberIdIn(memberIds)
            .map { pointEntityMapper.toDomain(it) }
    }
}

