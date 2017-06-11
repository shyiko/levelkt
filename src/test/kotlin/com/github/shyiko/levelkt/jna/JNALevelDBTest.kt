package com.github.shyiko.levelkt.jna

import com.github.shyiko.levelkt.LevelDB
import com.github.shyiko.levelkt.LevelDBRecord
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.nio.file.Files

class JNALevelDBTest {

    private fun openLevelDB() = JNALevelDB(Files.createTempDirectory("levelkt-jna-test-"))

    private fun assertThatAsAString(buf: ByteArray?) = assertThat(buf?.let { String(it) })
    private fun assertThatAsAStringList(buf: List<ByteArray>) = assertThat(buf.map { String(it) })

    private fun assertThatAsAStringPair(buf: LevelDBRecord?) =
        assertThat(buf?.let { (key, value) -> String(key) to String(value) })
    private fun assertThatAsAStringPairList(buf: List<LevelDBRecord>) =
        assertThat(buf.map { (key, value) -> String(key) to String(value) })

    private fun LevelDB.put(map: Map<String, String>) =
        batch { for ((key, value) in map) { put(key.toByteArray(), value.toByteArray()) } }

    @Test
    fun testCRUD() {
        openLevelDB().use { levelDB ->
            levelDB.put(mapOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3"))

            assertThatAsAString(levelDB.get("key1".toByteArray())).isEqualTo("value1")
            assertThatAsAString(levelDB.get("key2".toByteArray())).isNull() // exact match

            levelDB.put("key1".toByteArray(), "value1.updated".toByteArray())
            assertThatAsAString(levelDB.get("key1".toByteArray())).isEqualTo("value1.updated")
            levelDB.del("key1".toByteArray())
            assertThatAsAString(levelDB.get("key1".toByteArray())).isNull()
        }
    }

    @Test
    fun testContainsKey() {
        openLevelDB().use { levelDB ->
            levelDB.put(mapOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3"))

            assertThat(levelDB.containsKey("key1".toByteArray())).isTrue()
            assertThat(levelDB.containsKey("key2".toByteArray())).isFalse() // exact match
            assertThat(levelDB.containsKey("key_that_doesn't_exist".toByteArray())).isFalse()
        }
    }

    @Test
    fun testKeySlice() {
        openLevelDB().use { levelDB ->
            levelDB.put(mapOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3"))

            assertThatAsAStringList(levelDB.keySlice("key2".toByteArray()))
                .isEqualTo(listOf("key2.1", "key2.2", "key3"))
            assertThatAsAStringList(levelDB.keySlice("key_that_doesn't_exist".toByteArray())).isEmpty()
            assertThatAsAStringList(levelDB.keySlice("key2".toByteArray(), "key2\u007F".toByteArray()))
                .isEqualTo(listOf("key2.1", "key2.2"))
            assertThatAsAStringList(levelDB.keySlice("key".toByteArray(), "key2.2".toByteArray()))
                .isEqualTo(listOf("key1", "key2.1"))

            assertThatAsAStringList(levelDB.keySliceByPrefix("key2".toByteArray()))
                .isEqualTo(listOf("key2.1", "key2.2"))
        }
    }

    @Test
    fun testKeyCursor() {
        openLevelDB().use { levelDB ->
            levelDB.put(mapOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3"))

            levelDB.keyCursor().use { cursor ->
                assertThatAsAString(cursor.first()).isEqualTo("key1")
                assertThatAsAString(cursor.next()).isEqualTo("key2.1")
                assertThatAsAString(cursor.next()).isEqualTo("key2.2")
                assertThatAsAString(cursor.next()).isEqualTo("key3")
                assertThatAsAString(cursor.next()).isNull()
                assertThatAsAString(cursor.seek("key2".toByteArray())).isEqualTo("key2.1")
                assertThatAsAString(cursor.seek("key1".toByteArray())).isEqualTo("key1")
                assertThatAsAString(cursor.last()).isEqualTo("key3")
            }
        }
    }

    @Test
    fun testSlice() {
        openLevelDB().use { levelDB ->
            val ee = listOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3")
            levelDB.put(ee.toMap())

            val (e1, e21, e22, e3) = ee
            assertThatAsAStringPairList(levelDB.slice("key2".toByteArray())).isEqualTo(listOf(e21, e22, e3))
            assertThatAsAStringPairList(levelDB.slice("key_that_doesn't_exist".toByteArray())).isEmpty()
            assertThatAsAStringPairList(levelDB.slice("key2".toByteArray(), "key2\u007F".toByteArray()))
                .isEqualTo(listOf(e21, e22))
            assertThatAsAStringPairList(levelDB.slice("key".toByteArray(), "key2.2".toByteArray()))
                .isEqualTo(listOf(e1, e21))

            assertThatAsAStringPairList(levelDB.sliceByPrefix("key2".toByteArray()))
                .isEqualTo(listOf(e21, e22))
        }
    }

    @Test
    fun testCursor() {
        openLevelDB().use { levelDB ->
            val ee = listOf("key1" to "value1", "key2.1" to "value2.1", "key2.2" to "value2.2", "key3" to "value3")
            levelDB.put(ee.toMap())

            levelDB.cursor().use { cursor ->
                val (e1, e21, e22, e3) = ee
                assertThatAsAStringPair(cursor.first()).isEqualTo(e1)
                assertThatAsAStringPair(cursor.next()).isEqualTo(e21)
                assertThatAsAStringPair(cursor.next()).isEqualTo(e22)
                assertThatAsAStringPair(cursor.next()).isEqualTo(e3)
                assertThatAsAStringPair(cursor.next()).isNull()
                assertThatAsAStringPair(cursor.seek("key2".toByteArray())).isEqualTo(e21)
                assertThatAsAStringPair(cursor.seek("key1".toByteArray())).isEqualTo(e1)
                assertThatAsAStringPair(cursor.last()).isEqualTo(e3)
            }
        }
    }

}
