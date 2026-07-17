package com.mgt323.celltracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single persisted cell/radio measurement sample, per `.cursor/rules/android-telephony.mdc`.
 */
@Entity(tableName = "cell_measurement_table")
data class CellMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val cellType: String,
    val pci: Int,
    val band: String,
    val rsrp: Int,
    val rsrq: Int,
    // kHz, matching CellIdentityLte/Nr.getBandwidth()
    val bandwidth: Int,
    val isPrimaryServingCell: Boolean
)
