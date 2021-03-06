package uk.nhs.nhsx.diagnosiskeydist.agspec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.diagnosiskeydist.agspec.RollingStartNumber.isRollingStartNumberValid
import java.time.Instant

class RollingStartNumberTest {

    @Test
    fun `test is rolling start number valid apple google v1`() { //ensure keys of the past 14 days are valid when submitted
        val clock = { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
        var rollingStartNumber: Long = 2666736 // 2020-09-14 00:00:00 UTC (last key in 14 day history)
        assertTrue(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                144,
            )
        )
        rollingStartNumber = 2664864 // 2020-09-01 00:00:00 UTC (first key in 14 day history)
        assertTrue(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                144,
            )
        )
        rollingStartNumber = 2664720 // 2020-08-31 00:00:00 UTC
        assertFalse(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                144,
            )
        )
    }

    @Test
    fun `test is rolling start number valid apple google v2`() { //ensure keys of the past 14 days are valid when submitted plus current days key (with rollingPeriod < 144)
        var clock = { Instant.ofEpochSecond((2667023 * 600).toLong()) } // 2020-09-15 23:50:00 UTC
        var rollingStartNumber: Long = 2666880 // 2020-09-15 00:00:00 UTC (current day submission)
        assertTrue(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                144,
            )
        )
        rollingStartNumber = 2664864 // 2020-09-01 00:00:00 UTC
        assertTrue(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                144,
            )
        )
        rollingStartNumber = 2664864 // 2020-09-01 00:00:00 UTC
        assertFalse(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                72,
            )
        )
        clock = { Instant.ofEpochSecond((2666952 * 600).toLong()) } // 2020-09-15 12:00:00 UTC
        assertTrue(
            isRollingStartNumberValid(
                clock,
                rollingStartNumber,
                72,
            )
        )
    }
}
