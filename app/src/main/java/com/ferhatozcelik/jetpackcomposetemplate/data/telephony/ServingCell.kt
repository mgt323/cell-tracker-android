package com.ferhatozcelik.jetpackcomposetemplate.data.telephony

import com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellType

/**
 * Data-layer intermediate model for the registered serving cell, produced by
 * [CellInfoMapper] from raw `CellInfo` framework objects.
 *
 * Unlike the domain [com.ferhatozcelik.jetpackcomposetemplate.domain.model.CellSnapshot]
 * (whose fields are non-nullable), every radio parameter here is nullable so
 * that modem sentinel values (`CellInfo.UNAVAILABLE` / `Int.MAX_VALUE`) can be
 * mapped to `null` instead of leaking through. The repository combines this
 * with carrier-aggregation state into complete `CellSnapshot` emissions.
 */
data class ServingCell(
    val cellType: CellType,
    val band: String?,
    val rsrp: Int?,
    val rsrq: Int?,
    val bandwidthKhz: Int?
)
