package com.ferhatozcelik.jetpackcomposetemplate.ui.home

import com.ferhatozcelik.jetpackcomposetemplate.domain.exception.PermissionMissingException
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Development/`@Preview`-only fake for [TelephonyRepository].
 *
 * This is **not** the production implementation — the real one lives under
 * `data/telephony/` (`TelephonyRepositoryImpl`). This fake lives under
 * `ui/home/` intentionally, purely to drive `@Preview` composables and
 * manual UI development/testing without a device/emulator radio.
 *
 * @param throwPermissionMissing When `true`, [observeCurrentCell] emits
 *   [PermissionMissingException] instead of sample data, to preview the
 *   permissions-required fallback path.
 */
class FakeTelephonyRepository(
    initialSnapshot: CellSnapshot = SAMPLE_SNAPSHOT,
    private val throwPermissionMissing: Boolean = false
) : TelephonyRepository {

    private val snapshots = MutableStateFlow(initialSnapshot)

    override fun observeCurrentCell(): Flow<CellSnapshot> {
        if (throwPermissionMissing) {
            return flow { throw PermissionMissingException() }
        }
        return snapshots
    }

    companion object {
        val SAMPLE_SNAPSHOT = CellSnapshot(
            cellType = CellType.LTE,
            band = "B3",
            rsrp = -95,
            rsrq = -11,
            bandwidthKhz = 20000,
            isCarrierAggregationActive = true,
            secondaryBands = listOf("B1", "B7")
        )

        val SAMPLE_SNAPSHOT_NO_AGGREGATION = SAMPLE_SNAPSHOT.copy(
            isCarrierAggregationActive = false,
            secondaryBands = emptyList()
        )
    }
}
