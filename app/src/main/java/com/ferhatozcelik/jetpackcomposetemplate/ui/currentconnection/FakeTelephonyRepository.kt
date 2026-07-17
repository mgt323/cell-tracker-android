package com.ferhatozcelik.jetpackcomposetemplate.ui.currentconnection

import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType
import com.ferhatozcelik.jetpackcomposetemplate.domain.model.MissingTelephonyPermissionsException
import com.ferhatozcelik.jetpackcomposetemplate.domain.repository.TelephonyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Development/`@Preview`-only fake for [TelephonyRepository].
 *
 * This is **not** the production implementation — the real one lives under
 * `data/telephony/` (`TelephonyRepositoryImpl`), built independently against
 * the same contract. This fake intentionally lives under `ui/currentconnection/`
 * (not `data/telephony/`) and is a plain, un-annotated class — no `@Inject`
 * constructor, no `@Singleton`, no Hilt module binds it — so it can never be
 * picked up by Hilt's dependency graph. It exists purely to drive
 * [CurrentConnectionViewModel] and `@Preview` composables during UI
 * development without a device/emulator radio.
 *
 * @param initialSnapshot Snapshot emitted first (and whenever [emit] is
 *   called) when [failWithMissingPermissions] is `false`.
 * @param failWithMissingPermissions When `true`, [observeCurrentCell]
 *   terminates with [MissingTelephonyPermissionsException] instead of
 *   emitting, to preview/drive the permissions-required path.
 */
class FakeTelephonyRepository(
    initialSnapshot: CellSnapshot = SAMPLE_LTE_WITH_AGGREGATION,
    private val failWithMissingPermissions: Boolean = false
) : TelephonyRepository {

    private val snapshots = MutableStateFlow(initialSnapshot)

    /** Pushes a new canned snapshot, e.g. from a `@Preview`/dev harness. */
    fun emit(snapshot: CellSnapshot) {
        snapshots.value = snapshot
    }

    override fun observeCurrentCell(): Flow<CellSnapshot> {
        if (failWithMissingPermissions) {
            return flow { throw MissingTelephonyPermissionsException() }
        }
        return snapshots
    }

    companion object {
        /** Plausible LTE serving cell with two-carrier aggregation active. */
        val SAMPLE_LTE_WITH_AGGREGATION = CellSnapshot(
            cellType = CellType.LTE,
            band = "B3",
            rsrp = -95,
            rsrq = -11,
            bandwidthKhz = 20_000,
            isCarrierAggregationActive = true,
            secondaryBands = listOf("B1", "B7")
        )

        /** Plausible 5G-NR serving cell, no carrier aggregation. */
        val SAMPLE_NR_NO_AGGREGATION = CellSnapshot(
            cellType = CellType.NR,
            band = "n78",
            rsrp = -102,
            rsrq = -13,
            bandwidthKhz = 100_000,
            isCarrierAggregationActive = false,
            secondaryBands = emptyList()
        )
    }
}
