package at.bitfire.icsdroid

import at.bitfire.icsdroid.utils.iterator
import at.bitfire.icsdroid.utils.mapJSONObjects
import at.bitfire.icsdroid.utils.matches
import at.bitfire.icsdroid.utils.toJSONArray
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class JsonUtilsTest {
    @Test
    fun testMatches() {
        val array = JSONArray().apply {
            put("value1")
            put(3)
            put(false)
            put("value2")
            put(3.0)
            put(5L)
        }
        Assert.assertTrue(array.matches(array))
    }

    @Test
    fun testIterator() {
        val array = JSONArray().apply {
            put("value1")
            put(3)
            put(false)
            put("value2")
            put(3.0)
            put(5L)
        }
        for ((index, item) in array.iterator())
            Assert.assertEquals(item, array.get(index))
    }

    @Test
    fun testMapObjects() {
        val array = JSONArray().apply {
            put(JSONObject().apply { put("test", "value") })
            put(JSONObject().apply { put("test2", 2) })
        }
        for ((index, obj) in array.mapJSONObjects().withIndex())
            Assert.assertEquals(obj, array.getJSONObject(index))
    }

    @Test
    fun testObjectsToArray() {
        val list = listOf(
            JSONObject().apply { put("test", "value") },
            JSONObject().apply { put("testing", "value2") },
        )
        val array = list.toJSONArray()
        Assert.assertEquals(array.getJSONObject(0), list[0])
        Assert.assertEquals(array.getJSONObject(1), list[1])
    }
}