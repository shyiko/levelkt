package com.github.shyiko.levelkt.jna

import com.github.shyiko.levelkt.LevelDBMap
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.nio.file.Files

class JNALevelDBMapTest {

    private fun openLevelDBMap() = object : LevelDBMap<String, String>(
        Files.createTempDirectory("levelkt-jna-map-test-")
    ){
        override fun serializeKey(obj: String): ByteArray = obj.toByteArray()
        override fun serializeValue(obj: String): ByteArray = obj.toByteArray()
        override fun deserializeKey(obj: ByteArray): String = String(obj)
        override fun deserializeValue(obj: ByteArray): String = String(obj)
    }

    @Test
    fun testEmptyMapSize() {
        openLevelDBMap().use { levelDBMap ->
            assertThat(levelDBMap.size == 0).isTrue()
        }
    }

    @Test
    fun testIsEmpty() {
        openLevelDBMap().use { levelDBMap ->
            assertThat(levelDBMap.isEmpty()).isTrue()
        }
    }

    @Test
    fun testContainsKey() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap.containsKey("key1")).isTrue()
            assertThat(levelDBMap.containsKey("key2")).isTrue()
            assertThat(levelDBMap.containsKey("key3")).isTrue()
            assertThat(levelDBMap.containsKey("key4")).isTrue()
            assertThat(levelDBMap.containsKey("key_that_doesn't_exist")).isFalse()
        }
    }

    @Test
    fun testContainsValue() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap.containsValue("value3")).isTrue()
            assertThat(levelDBMap.containsValue("value_that_doesn't_exist")).isFalse()
        }
    }

    @Test
    fun testGet() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap["key1"] == "value1").isTrue()
            assertThat(levelDBMap["key2"] == "value2").isTrue()
            assertThat(levelDBMap["key3"] == "value3").isTrue()
            assertThat(levelDBMap["key4"] == "value3").isTrue()
            assertThat(levelDBMap["key5"] == null).isTrue()
        }
    }

    @Test
    fun testSize() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3"
            ))

            assertThat(levelDBMap.size == 4).isTrue()
        }
    }

    @Test
    fun testRemove() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap.remove("key1")).isEqualTo("value1")
            assertThat(levelDBMap.remove("key1")).isEqualTo(null)
        }
    }

    @Test
    fun testContainsKeyAfterRemove() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            levelDBMap.remove("key2")
            levelDBMap.remove("key5") // no effect

            assertThat(levelDBMap.containsKey("key1")).isTrue()
            assertThat(levelDBMap.containsKey("key2")).isFalse()
            assertThat(levelDBMap.containsKey("key3")).isTrue()
            assertThat(levelDBMap.containsKey("key4")).isTrue()
            assertThat(levelDBMap.containsKey("key_that_doesn't_exist")).isFalse()
        }
    }

    @Test
    fun testIsEmptyAfterRemove() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2"
            ))

            levelDBMap.remove("key1")
            levelDBMap.remove("key2")

            assertThat(levelDBMap.isEmpty()).isTrue()
        }
    }

    @Test
    fun testIsEmptyAfterClear() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2"
            ))

            levelDBMap.clear()

            assertThat(levelDBMap.isEmpty()).isTrue()
        }
    }

    @Test
    fun testGetKeys() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap.keys == setOf("key1", "key2", "key3", "key4")).isTrue()
        }
    }

    @Test
    fun testGetValues() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            assertThat(levelDBMap.values == listOf("value1", "value2", "value3", "value3")).isTrue()
        }
    }

    @Test
    fun testGetEntries() {
        openLevelDBMap().use { levelDBMap ->

            val content = mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            )

            levelDBMap.putAll(content)

            val entries = levelDBMap.entries
            
            assertThat(entries.size == 4).isTrue()
            assertThat(entries.associate { Pair(it.key, it.value) } == content).isTrue()
        }
    }

    @Test
    fun testModifyEntries() {
        openLevelDBMap().use { levelDBMap ->

            levelDBMap.putAll(mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "key4" to "value3" // same value as key3
            ))

            levelDBMap.entries.find { it.key == "key4" }!!.setValue("value4")

            assertThat(levelDBMap["key4"] == "value4").isTrue()
        }
    }
}
