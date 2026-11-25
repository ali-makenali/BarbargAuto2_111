package ir.barbarg.auto.net

import org.json.JSONArray
import org.json.JSONObject

/**
 * Kotlin representations of structures explicitly present in the supplied
 * UTCMS pages. Wire names are preserved where the contract specifies them.
 * Serialization is intentionally kept separate until the official wire format is verified.
 */

/**
 * BOLGoodCnt fields transcribed from the supplied UTCMS page 12.
 * Image is kept as raw bytes; its wire encoding is not assumed here.
 */
data class BOLGoodCnt(
    val GoodID: Int,
    val WeightKg: Float,
    val Value: Float,
    val PackingTypeID: Int,
    val GoodtypeID: Int,
    val Description: String? = null,
    val Image: ByteArray? = null
)

/**
 * GPSCnt fields transcribed from the supplied UTCMS page 12.
 * The document does not mark any GPS property as mandatory.
 */
data class GPSCnt(
    val Latitude: Float? = null,
    val Longitude: Float? = null,
    val Altitude: Float? = null,
    val Bearing: Float? = null,
    val NumberOfSatellite: Byte? = null,
    val PDOP: Byte? = null,
    val GPSSpeed: Byte? = null,
    val GPSMaxSpeed: Byte? = null,
    val GPSTotalTraveledDistance: Int? = null
)

data class KeyValueCnt(val key: Int, val value: String) {
    companion object {
        fun fromJson(json: JSONObject) = KeyValueCnt(json.optInt("Key"), json.optString("Value"))
    }
}

data class ProvinceCityCnt(
    val provinceCode: Int,
    val provinceDesc: String,
    val cityCode: String,
    val cityDesc: String
) {
    companion object {
        fun fromJson(json: JSONObject) = ProvinceCityCnt(
            json.optInt("PROVINCECODE"),
            json.optString("PROVINCEDESC"),
            json.optString("CITYCODE"),
            json.optString("CITYDESC")
        )
    }
}

data class ExceptionCnt(val errCode: Int, val errDesc: String) {
    companion object {
        fun fromJson(json: JSONObject) = ExceptionCnt(
            json.optInt("ErrCode", -1),
            json.optString("ErrDesc")
        )
    }
}

data class RegisterInfoCnt(val idNo: Int, val userType: Int) {
    companion object {
        fun fromJson(json: JSONObject) = RegisterInfoCnt(
            json.optInt("IDNo"),
            json.optInt("UserType")
        )
    }
}

data class ResponseBOLCnt(
    val responseStatus: String,
    val responseMessage: String,
    val traceCode: String,
    val state: String
) {
    companion object {
        fun fromJson(json: JSONObject) = ResponseBOLCnt(
            json.optString("ResponseStatus"),
            json.optString("ResponseMessage"),
            json.optString("TraceCode"),
            json.optString("State")
        )
    }
}

/** Fields documented for SystemOptionCnt on pages 19-21. */
data class SystemOptionCnt(
    val option01A: Boolean = false,
    val option02A: Boolean = false,
    val option03A: Boolean = false,
    val option04A: Boolean = false,
    val option05A: Boolean = false,
    val option06A: Boolean = false,
    val optionA17: Boolean = false,
    val optionA72: Boolean = false,
    val optionA73: Boolean = false,
    val optionA74: Boolean = false,
    val optionA75: Boolean = false,
    val optionA76: Boolean = false,
    val optionA77: Boolean = false,
    val optionA78: Boolean = false,
    val optionA79: Boolean = false,
    val optionA18: Boolean = false,
    val optionA82: Boolean = false,
    val optionA83: Boolean = false,
    val optionA84: Boolean = false,
    val optionA85: Boolean = false,
    val optionA86: Boolean = false,
    val optionA87: Boolean = false,
    val optionA88: Boolean = false,
    val optionA89: Boolean = false,
    val option01B: Boolean = false,
    val option02B: Boolean = false,
    val option31B: Boolean = false,
    val option32B: Boolean = false,
    val option33B: Boolean = false,
    val option34B: Boolean = false,
    val option35B: Boolean = false,
    val option36B: Boolean = false,
    val option37B: Boolean = false,
    val option38B: Boolean = false,
    val option39B: Boolean = false,
    val option41B: Boolean = false,
    val option42B: Boolean = false,
    val option43B: Boolean = false,
    val option44B: Boolean = false,
    val option45B: Boolean = false,
    val option46B: Boolean = false,
    val option47B: Boolean = false,
    val option48B: Boolean = false,
    val option49B: Boolean = false,
    val c01: String = "",
    val c02: String = "",
    val c03: String = "",
    val c04: Boolean = false,
    val c05: Boolean = false,
    val d01: Boolean = false,
    val d02: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): SystemOptionCnt {
            fun bool(name: String) = json.optBoolean(name, false)
            fun str(name: String) = if (json.isNull(name)) "" else json.optString(name, "")
            return SystemOptionCnt(
                option01A = bool("01A"), option02A = bool("02A"),
                option03A = bool("03A"), option04A = bool("04A"),
                option05A = bool("05A"), option06A = bool("06A"),
                optionA17 = bool("A17"), optionA72 = bool("A72"),
                optionA73 = bool("A73"), optionA74 = bool("A74"),
                optionA75 = bool("A75"), optionA76 = bool("A76"),
                optionA77 = bool("A77"), optionA78 = bool("A78"),
                optionA79 = bool("A79"), optionA18 = bool("A18"),
                optionA82 = bool("A82"), optionA83 = bool("A83"),
                optionA84 = bool("A84"), optionA85 = bool("A85"),
                optionA86 = bool("A86"), optionA87 = bool("A87"),
                optionA88 = bool("A88"), optionA89 = bool("A89"),
                option01B = bool("01B"), option02B = bool("02B"),
                option31B = bool("31B"), option32B = bool("32B"),
                option33B = bool("33B"), option34B = bool("34B"),
                option35B = bool("35B"), option36B = bool("36B"),
                option37B = bool("37B"), option38B = bool("38B"),
                option39B = bool("39B"), option41B = bool("41B"),
                option42B = bool("42B"), option43B = bool("43B"),
                option44B = bool("44B"), option45B = bool("45B"),
                option46B = bool("46B"), option47B = bool("47B"),
                option48B = bool("48B"), option49B = bool("49B"),
                c01 = str("C01"), c02 = str("C02"), c03 = str("C03"),
                c04 = bool("C04"), c05 = bool("C05"),
                d01 = bool("D01"), d02 = bool("D02")
            )
        }
    }
}

/** Unwraps common ASMX { "d": ... } envelopes without guessing payload schemas. */
object UtcmsJson {
    fun unwrap(raw: String): Any? {
        val root = JSONObject(raw)
        if (!root.has("d") || root.isNull("d")) return root
        val data = root.get("d")
        return if (data is String) {
            val trimmed = data.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> data
            }
        } else data
    }

    fun keyValueList(value: Any?): List<KeyValueCnt> {
        val array = value as? JSONArray ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(KeyValueCnt.fromJson(it)) }
            }
        }
    }

    fun provinceCityList(value: Any?): List<ProvinceCityCnt> {
        val array = value as? JSONArray ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(ProvinceCityCnt.fromJson(it)) }
            }
        }
    }
}
