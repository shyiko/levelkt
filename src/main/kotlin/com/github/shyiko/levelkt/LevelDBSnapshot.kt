package com.github.shyiko.levelkt

import java.io.Closeable

interface LevelDBSnapshot : Closeable {

    fun containsKey(key: ByteArray, verifyChecksum: Boolean = false, fillCache: Boolean = true): Boolean

    fun keySlice(start: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<ByteArray>
    fun keySlice(start: ByteArray, end: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<ByteArray>
    fun keySliceByPrefix(prefix: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<ByteArray>
    fun keyCursor(verifyChecksum: Boolean = false, fillCache: Boolean = true): LevelDBCursor<ByteArray>

    fun slice(start: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<LevelDBRecord>
    fun slice(start: ByteArray, end: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<LevelDBRecord>
    fun sliceByPrefix(prefix: ByteArray, offset: Int = 0, limit: Int = 0,
        verifyChecksum: Boolean = false, fillCache: Boolean = true): List<LevelDBRecord>
    fun cursor(verifyChecksum: Boolean = false, fillCache: Boolean = true): LevelDBCursor<LevelDBRecord>

    fun get(key: ByteArray, verifyChecksum: Boolean = false, fillCache: Boolean = true): ByteArray?

    // java8-compat

    fun containsKey(key: ByteArray): Boolean = containsKey(key, false, true)
    fun containsKey(key: ByteArray, verifyChecksum: Boolean): Boolean = containsKey(key, verifyChecksum, true)

    fun keySlice(start: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<ByteArray> =
        keySlice(start, offset, limit, verifyChecksum, true)
    fun keySlice(start: ByteArray, offset: Int, limit: Int): List<ByteArray> =
        keySlice(start, offset, limit, false, true)
    fun keySlice(start: ByteArray, offset: Int): List<ByteArray> =
        keySlice(start, offset, 0, false, true)
    fun keySlice(start: ByteArray): List<ByteArray> =
        keySlice(start, 0, 0, false, true)
    fun keySlice(start: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<ByteArray> =
        keySlice(start, 0, 0, verifyChecksum, fillCache)
    fun keySlice(start: ByteArray, verifyChecksum: Boolean): List<ByteArray> =
        keySlice(start, 0, 0, verifyChecksum, true)

    fun keySlice(start: ByteArray, end: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<ByteArray> =
        keySlice(start, end, offset, limit, verifyChecksum, true)
    fun keySlice(start: ByteArray, end: ByteArray, offset: Int, limit: Int): List<ByteArray> =
        keySlice(start, end, offset, limit, false, true)
    fun keySlice(start: ByteArray, end: ByteArray, offset: Int): List<ByteArray> =
        keySlice(start, end, offset, 0, false, true)
    fun keySlice(start: ByteArray, end: ByteArray): List<ByteArray> =
        keySlice(start, end, 0, 0, false, true)
    fun keySlice(start: ByteArray, end: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<ByteArray> =
        keySlice(start, end, 0, 0, verifyChecksum, fillCache)
    fun keySlice(start: ByteArray, end: ByteArray, verifyChecksum: Boolean): List<ByteArray> =
        keySlice(start, end, 0, 0, verifyChecksum, true)

    fun keySliceByPrefix(prefix: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<ByteArray> =
        keySliceByPrefix(prefix, offset, limit, verifyChecksum, true)
    fun keySliceByPrefix(prefix: ByteArray, offset: Int, limit: Int): List<ByteArray> =
        keySliceByPrefix(prefix, offset, limit, false, true)
    fun keySliceByPrefix(prefix: ByteArray, offset: Int): List<ByteArray> =
        keySliceByPrefix(prefix, offset, 0, false, true)
    fun keySliceByPrefix(prefix: ByteArray): List<ByteArray> =
        keySliceByPrefix(prefix, 0, 0, false, true)
    fun keySliceByPrefix(prefix: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<ByteArray> =
        keySliceByPrefix(prefix, 0, 0, verifyChecksum, fillCache)
    fun keySliceByPrefix(prefix: ByteArray, verifyChecksum: Boolean): List<ByteArray> =
        keySliceByPrefix(prefix, 0, 0, verifyChecksum, true)

    fun keyCursor(verifyChecksum: Boolean): LevelDBCursor<ByteArray> =
        keyCursor(verifyChecksum, true)
    fun keyCursor(): LevelDBCursor<ByteArray> =
        keyCursor(false, true)

    fun slice(start: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<LevelDBRecord> =
        slice(start, offset, limit, verifyChecksum, true)
    fun slice(start: ByteArray, offset: Int, limit: Int): List<LevelDBRecord> =
        slice(start, offset, limit, false, true)
    fun slice(start: ByteArray, offset: Int): List<LevelDBRecord> =
        slice(start, offset, 0, false, true)
    fun slice(start: ByteArray): List<LevelDBRecord> =
        slice(start, 0, 0, false, true)
    fun slice(start: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<LevelDBRecord> =
        slice(start, 0, 0, verifyChecksum, fillCache)
    fun slice(start: ByteArray, verifyChecksum: Boolean): List<LevelDBRecord> =
        slice(start, 0, 0, verifyChecksum, true)

    fun slice(start: ByteArray, end: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<LevelDBRecord> =
        slice(start, end, offset, limit, verifyChecksum, true)
    fun slice(start: ByteArray, end: ByteArray, offset: Int, limit: Int): List<LevelDBRecord> =
        slice(start, end, offset, limit, false, true)
    fun slice(start: ByteArray, end: ByteArray, offset: Int): List<LevelDBRecord> =
        slice(start, end, offset, 0, false, true)
    fun slice(start: ByteArray, end: ByteArray): List<LevelDBRecord> =
        slice(start, end, 0, 0, false, true)
    fun slice(start: ByteArray, end: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<LevelDBRecord> =
        slice(start, end, 0, 0, verifyChecksum, fillCache)
    fun slice(start: ByteArray, end: ByteArray, verifyChecksum: Boolean): List<LevelDBRecord> =
        slice(start, end, 0, 0, verifyChecksum, true)

    fun sliceByPrefix(prefix: ByteArray, offset: Int, limit: Int, verifyChecksum: Boolean): List<LevelDBRecord> =
        sliceByPrefix(prefix, offset, limit, verifyChecksum, true)
    fun sliceByPrefix(prefix: ByteArray, offset: Int, limit: Int): List<LevelDBRecord> =
        sliceByPrefix(prefix, offset, limit, false, true)
    fun sliceByPrefix(prefix: ByteArray, offset: Int): List<LevelDBRecord> =
        sliceByPrefix(prefix, offset, 0, false, true)
    fun sliceByPrefix(prefix: ByteArray): List<LevelDBRecord> =
        sliceByPrefix(prefix, 0, 0, false, true)
    fun sliceByPrefix(prefix: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): List<LevelDBRecord> =
        sliceByPrefix(prefix, 0, 0, verifyChecksum, fillCache)
    fun sliceByPrefix(prefix: ByteArray, verifyChecksum: Boolean): List<LevelDBRecord> =
        sliceByPrefix(prefix, 0, 0, verifyChecksum, true)

    fun cursor(verifyChecksum: Boolean): LevelDBCursor<LevelDBRecord> =
        cursor(verifyChecksum, true)
    fun cursor(): LevelDBCursor<LevelDBRecord> =
        cursor(false, true)

    fun get(key: ByteArray, verifyChecksum: Boolean): ByteArray? = get(key, verifyChecksum, true)
    fun get(key: ByteArray): ByteArray? = get(key, false, true)

}
