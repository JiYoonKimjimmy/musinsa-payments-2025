package com.musinsa.payments.point.application.port.out

import com.musinsa.payments.point.domain.valueobject.PointKey

/**
 * 포인트 키 생성기 인터페이스
 * Application 레이어에서 정의하는 아웃바운드 포트입니다.
 * Infrastructure 레이어에서 이 인터페이스를 구현합니다.
 */
interface PointKeyGenerator {
    
    /**
     * 포인트 키 생성
     * @return 생성된 포인트 키
     */
    fun generate(): PointKey
}

