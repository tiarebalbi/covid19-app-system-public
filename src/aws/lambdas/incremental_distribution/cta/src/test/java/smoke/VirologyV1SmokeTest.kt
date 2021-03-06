package smoke

import org.assertj.core.api.Assertions.assertThat
import org.http4k.client.JavaHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import smoke.actors.ApiVersion.V1
import smoke.actors.MobileApp
import smoke.actors.TestLab
import smoke.env.SmokeTests
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyResultSource.Npex
import uk.nhs.nhsx.virology.VirologyUploadHandler.VirologyTokenExchangeSource
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.Available
import uk.nhs.nhsx.virology.exchange.CtaExchangeResult.NotFound
import uk.nhs.nhsx.virology.lookup.VirologyLookupResult
import uk.nhs.nhsx.domain.TestEndDate
import uk.nhs.nhsx.domain.TestResult.Negative
import uk.nhs.nhsx.domain.TestResult.Positive

class VirologyV1SmokeTest {

    private val client = JavaHttpClient()
    private val config = SmokeTests.loadConfig()
    private val mobileApp = MobileApp(client, config)
    private val testLab = TestLab(client, config)

    @ParameterizedTest
    @EnumSource(VirologyResultSource::class)
    fun `order, upload and poll via different sources`(source: VirologyResultSource) {
        val orderResponse = mobileApp.orderTest(V1)

        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )
        val pollingToken = orderResponse.testResultPollingToken
        val testResponse = (mobileApp.pollForTestResult(pollingToken, V1) as VirologyLookupResult.Available).response

        assertThat(testResponse.testResult).isEqualTo(Positive)
        assertThat(testResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 4, 23))
        assertThat(testResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @ParameterizedTest
    @EnumSource(VirologyTokenExchangeSource::class)
    fun `lab token gen and ctaExchange via different sources`(source: VirologyTokenExchangeSource) {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Positive,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = source,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val exchangeResponse = mobileApp.exchange(ctaToken, V1)

        val ctaExchangeResponse = (exchangeResponse as Available).ctaExchangeResponse
        assertThat(ctaExchangeResponse.testResult).isEqualTo(Positive)
        assertThat(ctaExchangeResponse.testEndDate).isEqualTo(TestEndDate.of(2020, 11, 19))
        assertThat(ctaExchangeResponse.testKit).isEqualTo(LAB_RESULT)
    }

    @Test
    fun `test result can be polled more than twice`() {
        val orderResponse = mobileApp.orderTest(V1)
        testLab.uploadTestResult(
            token = orderResponse.tokenParameterValue,
            result = Positive,
            source = Npex,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        repeat(times = 3) {
            val pollingToken = orderResponse.testResultPollingToken
            val testResponse = (mobileApp.pollForTestResult(pollingToken, V1) as VirologyLookupResult.Available).response
            assertThat(testResponse.testResult).isEqualTo(Positive)
        }
    }

    @Test
    fun `not found when exchanging cta token more than twice`() {
        val ctaToken = testLab.generateCtaTokenFor(
            testResult = Negative,
            testEndDate = TestEndDate.of(2020, 11, 19),
            source = VirologyTokenExchangeSource.Eng,
            apiVersion = V1,
            testKit = LAB_RESULT
        )

        val firstCall = mobileApp.exchange(ctaToken, V1)
        assertThat(firstCall).isInstanceOf(Available::class.java)

        val secondCall = mobileApp.exchange(ctaToken, V1)
        assertThat(secondCall).isInstanceOf(Available::class.java)

        val thirdCall = mobileApp.exchange(ctaToken, V1)
        assertThat(thirdCall).isInstanceOf(NotFound::class.java)
    }

}
