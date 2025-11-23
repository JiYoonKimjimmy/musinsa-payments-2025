package com.musinsa.payments.point.presentation.web.mapper

import com.musinsa.payments.point.domain.entity.PointAccumulation
import com.musinsa.payments.point.domain.entity.PointUsage
import com.musinsa.payments.point.presentation.web.dto.response.*
import org.springframework.data.domain.Page

/**
 * 포인트 DTO 매퍼
 * 도메인 엔티티와 DTO 간 변환을 담당합니다.
 */
object PointDtoMapper {
    
    /**
     * PointAccumulation → AccumulatePointResponse 변환
     */
    fun toAccumulatePointResponse(accumulation: PointAccumulation): AccumulatePointResponse {
        return AccumulatePointResponse(
            pointKey = accumulation.pointKey,
            memberId = accumulation.memberId,
            amount = accumulation.amount.toLong(),
            availableAmount = accumulation.availableAmount.toLong(),
            expirationDate = accumulation.expirationDate,
            isManualGrant = accumulation.isManualGrant,
            status = accumulation.status.name,
            createdAt = accumulation.createdAt
        )
    }
    
    /**
     * PointAccumulation → CancelAccumulationResponse 변환
     */
    fun toCancelAccumulationResponse(accumulation: PointAccumulation): CancelAccumulationResponse {
        return CancelAccumulationResponse(
            pointKey = accumulation.pointKey,
            memberId = accumulation.memberId,
            amount = accumulation.amount.toLong(),
            availableAmount = accumulation.availableAmount.toLong(),
            expirationDate = accumulation.expirationDate,
            isManualGrant = accumulation.isManualGrant,
            status = accumulation.status.name,
            createdAt = accumulation.createdAt,
            updatedAt = accumulation.updatedAt
        )
    }
    
    /**
     * PointUsage → UsePointResponse 변환
     */
    fun toUsePointResponse(usage: PointUsage): UsePointResponse {
        return UsePointResponse(
            pointKey = usage.pointKey,
            memberId = usage.memberId,
            orderNumber = usage.orderNumber.value,
            totalAmount = usage.totalAmount.toLong(),
            cancelledAmount = usage.cancelledAmount.toLong(),
            status = usage.status.name,
            createdAt = usage.createdAt
        )
    }
    
    /**
     * PointUsage → CancelUsageResponse 변환
     */
    fun toCancelUsageResponse(usage: PointUsage): CancelUsageResponse {
        return CancelUsageResponse(
            pointKey = usage.pointKey,
            memberId = usage.memberId,
            orderNumber = usage.orderNumber.value,
            totalAmount = usage.totalAmount.toLong(),
            cancelledAmount = usage.cancelledAmount.toLong(),
            status = usage.status.name,
            createdAt = usage.createdAt,
            updatedAt = usage.updatedAt
        )
    }
    
    /**
     * PointBalanceResult → PointBalanceResponse 변환
     */
    fun toPointBalanceResponse(
        memberId: Long,
        totalBalance: Long,
        availableBalance: Long,
        expiredBalance: Long,
        accumulations: List<PointAccumulation>
    ): PointBalanceResponse {
        return PointBalanceResponse(
            memberId = memberId,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            expiredBalance = expiredBalance,
            accumulations = accumulations.map { toPointAccumulationItem(it) }
        )
    }
    
    /**
     * PointAccumulation → PointAccumulationItem 변환
     */
    private fun toPointAccumulationItem(accumulation: PointAccumulation): PointAccumulationItem {
        return PointAccumulationItem(
            pointKey = accumulation.pointKey,
            amount = accumulation.amount.toLong(),
            availableAmount = accumulation.availableAmount.toLong(),
            expirationDate = accumulation.expirationDate,
            isManualGrant = accumulation.isManualGrant,
            status = accumulation.status.name
        )
    }
    
    /**
     * Page<PointUsage> → PointUsageHistoryResponse 변환
     */
    fun toPointUsageHistoryResponse(page: Page<PointUsage>): PointUsageHistoryResponse {
        return PointUsageHistoryResponse(
            content = page.content.map { toPointUsageHistoryItem(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }
    
    /**
     * PointUsage → PointUsageHistoryItem 변환
     */
    private fun toPointUsageHistoryItem(usage: PointUsage): PointUsageHistoryItem {
        return PointUsageHistoryItem(
            pointKey = usage.pointKey,
            memberId = usage.memberId,
            orderNumber = usage.orderNumber.value,
            totalAmount = usage.totalAmount.toLong(),
            cancelledAmount = usage.cancelledAmount.toLong(),
            remainingAmount = usage.getRemainingAmount().toLong(),
            status = usage.status.name,
            createdAt = usage.createdAt,
            updatedAt = usage.updatedAt
        )
    }
    
    /**
     * Page<T> → PageResponse<T> 변환
     */
    fun <T> toPageResponse(page: Page<T>): PageResponse<T> {
        return PageResponse(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }
}

