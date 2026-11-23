package io.github.uveshm003.anchor.core.model

import kotlinx.serialization.Serializable

/**
 * A paired NFC sticker or QR code. Only a salted SHA-256 hash of the tag UID / QR secret is stored, as 64
 * lowercase hex characters. [modeId] is the mode a tap starts when idle (null = the default mode).
 */
@Serializable
data class Anchor(
    val id: AnchorId,
    val name: String,
    val type: AnchorType,
    val hash: String,
    val modeId: ModeId? = null,
) {
    init {
        require(name.isNotBlank()) { "Anchor name must not be blank" }
        require(SHA_256_HEX.matches(hash)) { "Anchor hash must be 64 lowercase hex characters" }
    }
}

@Serializable
enum class AnchorType { Nfc, Qr }

/** A geofence saved from the current location (no map SDK). */
@Serializable
data class Place(
    val id: PlaceId,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
) {
    init {
        require(name.isNotBlank()) { "Place name must not be blank" }
        require(latitude in LATITUDE_RANGE) { "Latitude out of range: $latitude" }
        require(longitude in LONGITUDE_RANGE) { "Longitude out of range: $longitude" }
        require(radiusMeters > 0f) { "Radius must be > 0, was $radiusMeters" }
    }
}

/** A BLE device that defines a space: in range when RSSI ≥ [rssiThreshold], out when below it by [hysteresisDb]. */
@Serializable
data class Space(
    val id: SpaceId,
    val name: String,
    val deviceAddress: String,
    val rssiThreshold: Int,
    val hysteresisDb: Int = DEFAULT_HYSTERESIS_DB,
    val dwellSeconds: Int = DEFAULT_DWELL_SECONDS,
) {
    init {
        require(name.isNotBlank()) { "Space name must not be blank" }
        require(rssiThreshold in RSSI_RANGE) { "RSSI threshold must be in $RSSI_RANGE dBm, was $rssiThreshold" }
        require(hysteresisDb >= 0) { "Hysteresis must be >= 0" }
        require(dwellSeconds >= 0) { "Dwell must be >= 0" }
    }
}

private val SHA_256_HEX = Regex("^[0-9a-f]{64}$")
private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0
private val RSSI_RANGE = -127..0
private const val DEFAULT_HYSTERESIS_DB = 5
private const val DEFAULT_DWELL_SECONDS = 30
