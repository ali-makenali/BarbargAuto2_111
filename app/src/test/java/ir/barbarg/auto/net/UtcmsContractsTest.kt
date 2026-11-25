package ir.barbarg.auto.net

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UtcmsContractsTest {
    @Test
    fun modelsDocumentedGoodsAndGpsFields() {
        val good = BOLGoodCnt(
            GoodID = 12,
            WeightKg = 1250.5f,
            Value = 4f,
            PackingTypeID = 2,
            GoodtypeID = 9,
            Description = "داده آزمایشی",
            Image = byteArrayOf(1, 2, 3)
        )
        assertEquals(12, good.GoodID)
        assertEquals(1250.5f, good.WeightKg)
        assertEquals(3, good.Image?.size)

        val gps = GPSCnt(
            Latitude = 35.7f,
            Longitude = 51.4f,
            NumberOfSatellite = 8,
            GPSTotalTraveledDistance = 120
        )
        assertEquals(35.7f, gps.Latitude)
        assertEquals(8.toByte(), gps.NumberOfSatellite)
        assertEquals(120, gps.GPSTotalTraveledDistance)
    }

    @Test
    fun parsesKeyValueList() {
        val rows = UtcmsJson.keyValueList(
            JSONArray().put(JSONObject().put("Key", 7).put("Value", "کالای آزمایشی"))
        )
        assertEquals(1, rows.size)
        assertEquals(7, rows.first().key)
        assertEquals("کالای آزمایشی", rows.first().value)
    }

    @Test
    fun parsesProvinceAndCityCodesAsDocumented() {
        val row = ProvinceCityCnt.fromJson(
            JSONObject()
                .put("PROVINCECODE", 8)
                .put("PROVINCEDESC", "استان آزمایشی")
                .put("CITYCODE", "00801")
                .put("CITYDESC", "شهر آزمایشی")
        )
        assertEquals(8, row.provinceCode)
        assertEquals("00801", row.cityCode)
    }

    @Test
    fun parsesSystemOptionsWithoutRenamingWireKeys() {
        val options = SystemOptionCnt.fromJson(
            JSONObject()
                .put("01A", true)
                .put("A75", true)
                .put("C01", "5")
                .put("C04", true)
                .put("D02", true)
        )
        assertTrue(options.option01A)
        assertTrue(options.optionA75)
        assertEquals("5", options.c01)
        assertTrue(options.c04)
        assertTrue(options.d02)
        assertFalse(options.option02A)
    }

    @Test
    fun unwrapsAsmxStringContainingJsonArray() {
        val value = UtcmsJson.unwrap("""{"d":"[{\"Key\":3,\"Value\":\"نمونه\"}]"}""")
        assertTrue(value is JSONArray)
        assertEquals(3, (value as JSONArray).getJSONObject(0).getInt("Key"))
    }
}
