package com.musinsa.payments.point.application.service

import com.musinsa.payments.point.application.port.output.persistence.PointAccumulationPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsageDetailPersistencePort
import com.musinsa.payments.point.application.port.output.persistence.PointUsagePersistencePort
import com.musinsa.payments.point.domain.entity.PointAccumulationStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * PointUsageService 동시성 테스트
 * 비관적 락이 정상적으로 동작하는지 검증
 */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PointUsageServiceConcurrencyTest(
    private val pointAccumulationService: PointAccumulationService,
    private val pointUsageService: PointUsageService,
    private val pointCancellationService: PointCancellationService,
    private val pointQueryService: PointQueryService,
    private val pointAccumulationPersistencePort: PointAccumulationPersistencePort,
    private val pointUsagePersistencePort: PointUsagePersistencePort,
    private val pointUsageDetailPersistencePort: PointUsageDetailPersistencePort
) : BehaviorSpec({

    extension(SpringExtension)

    beforeContainer {
        // 각 Given 블록 전에 데이터 초기화
        val allUsageDetails = pointUsageDetailPersistencePort.findAll()
        allUsageDetails.forEach { detail ->
            detail.id?.let { pointUsageDetailPersistencePort.deleteById(it) }
        }

        val allUsages = pointUsagePersistencePort.findAll()
        allUsages.forEach { usage ->
            usage.id?.let { pointUsagePersistencePort.deleteById(it) }
        }

        val allAccumulations = pointAccumulationPersistencePort
            .findByMemberIdAndStatus(1L, PointAccumulationStatus.ACCUMULATED)
        allAccumulations.forEach { accumulation ->
            accumulation.id?.let { pointAccumulationPersistencePort.deleteById(it) }
        }
    }

    Given("회원에게 10000원의 포인트가 적립되어 있을 때") {
        val memberId = 1L
        val initialAmount = 10000L

        When("10개의 스레드가 동시에 1000원씩 사용을 시도하면") {
            // 초기 적립
            pointAccumulationService.accumulate(memberId, initialAmount, null, false)

            val threadCount = 10
            val usageAmount = 1000L
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val results = ConcurrentHashMap<Int, Result<Unit>>()

            repeat(threadCount) { index ->
                executorService.submit {
                    try {
                        pointUsageService.use(memberId, "ORDER-$index", usageAmount)
                        results[index] = Result.success(Unit)
                    } catch (e: Exception) {
                        results[index] = Result.failure(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()
            executorService.awaitTermination(5, TimeUnit.SECONDS)

            Then("모든 사용이 성공해야 한다") {
                val successCount = results.values.count { it.isSuccess }
                successCount shouldBe threadCount
            }

            Then("최종 잔액은 0원이어야 한다") {
                val balance = pointQueryService.getBalance(memberId)
                balance.availableBalance shouldBe 0L
            }

            Then("사용 상세 내역의 총 개수는 10개여야 한다 (10개 사용 건 × 1개 적립 건)") {
                val allDetails = pointUsageDetailPersistencePort.findAll()
                allDetails.size shouldBe threadCount  // 10개 사용 건, 각각 1개 적립 건에서 사용
            }
        }

        When("11개의 스레드가 동시에 1000원씩 사용을 시도하면 (잔액 부족)") {
            // 초기 적립
            pointAccumulationService.accumulate(memberId, initialAmount, null, false)

            val threadCount = 11
            val usageAmount = 1000L
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val results = ConcurrentHashMap<Int, Result<Unit>>()

            repeat(threadCount) { index ->
                executorService.submit {
                    try {
                        pointUsageService.use(memberId, "ORDER-$index", usageAmount)
                        results[index] = Result.success(Unit)
                    } catch (e: Exception) {
                        results[index] = Result.failure(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()

            Then("10개는 성공하고 1개는 실패해야 한다") {
                val successCount = results.values.count { it.isSuccess }
                val failureCount = results.values.count { it.isFailure }

                successCount shouldBe 10
                failureCount shouldBe 1
            }

            Then("최종 잔액은 0원이어야 한다") {
                val balance = pointQueryService.getBalance(memberId)
                balance.availableBalance shouldBe 0L
            }
        }
    }

    Given("회원에게 여러 건의 포인트가 적립되어 있을 때") {
        val memberId = 2L

        When("여러 스레드가 동시에 포인트를 사용하면") {
            // 초기 적립 (5건 * 2000원 = 10000원)
            repeat(5) {
                pointAccumulationService.accumulate(memberId, 2000L, null, false)
            }

            val threadCount = 5
            val usageAmount = 2000L
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val results = ConcurrentHashMap<Int, Result<Unit>>()

            repeat(threadCount) { index ->
                executorService.submit {
                    try {
                        pointUsageService.use(memberId, "ORDER-$index", usageAmount)
                        results[index] = Result.success(Unit)
                    } catch (e: Exception) {
                        results[index] = Result.failure(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()

            Then("모든 사용이 성공해야 한다") {
                val successCount = results.values.count { it.isSuccess }
                successCount shouldBe threadCount
            }

            Then("최종 잔액은 0원이어야 한다") {
                val balance = pointQueryService.getBalance(memberId)
                balance.availableBalance shouldBe 0L
            }
        }
    }

    Given("동시에 포인트 사용과 사용 취소가 발생할 때") {
        val memberId = 3L
        val initialAmount = 10000L

        When("한 스레드는 사용, 다른 스레드는 취소를 시도하면") {
            // 초기 적립
            pointAccumulationService.accumulate(memberId, initialAmount, null, false)

            // 먼저 5000원 사용
            val firstUsage = pointUsageService.use(memberId, "ORDER-FIRST", 5000L)
            val firstPointKey = firstUsage.pointKey

            val threadCount = 2
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val results = ConcurrentHashMap<String, Result<Unit>>()

            // 스레드 1: 5000원 추가 사용
            executorService.submit {
                try {
                    pointUsageService.use(memberId, "ORDER-SECOND", 5000L)
                    results["use"] = Result.success(Unit)
                } catch (e: Exception) {
                    results["use"] = Result.failure(e)
                } finally {
                    latch.countDown()
                }
            }

            // 스레드 2: 첫 번째 사용 취소
            executorService.submit {
                try {
                    Thread.sleep(50) // 약간의 지연
                    pointCancellationService.cancelUsage(firstPointKey, null, "동시성 테스트")
                    results["cancel"] = Result.success(Unit)
                } catch (e: Exception) {
                    results["cancel"] = Result.failure(e)
                } finally {
                    latch.countDown()
                }
            }

            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()

            Then("모든 작업이 성공해야 한다") {
                results.values.all { it.isSuccess } shouldBe true
            }

            Then("최종 잔액은 5000원이어야 한다 (10000 - 5000 사용 + 5000 취소 복원 - 5000 사용)") {
                val balance = pointQueryService.getBalance(memberId)
                balance.availableBalance shouldBe 5000L
            }
        }
    }
})
