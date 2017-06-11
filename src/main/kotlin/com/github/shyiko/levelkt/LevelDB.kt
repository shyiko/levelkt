package com.github.shyiko.levelkt

import java.io.Closeable
import java.util.function.Consumer

interface LevelDB : LevelDBSnapshot, Closeable {

    fun snapshot(): LevelDBSnapshot
    fun put(key: ByteArray, value: ByteArray, sync: Boolean = false)
    fun del(key: ByteArray, sync: Boolean = false)
    fun batch(sync: Boolean = false, builder: LevelDBBatch.() -> Unit)
    fun property(key: String): String
    fun approximateSize(start: ByteArray, end: ByteArray): Long
    fun compactRange(start: ByteArray, end: ByteArray)
    fun destroy()

    // java8-compat

    fun put(key: ByteArray, value: ByteArray) = put(key, value, false)
    fun del(key: ByteArray) = del(key, false)
    fun batch(builder: Consumer<LevelDBBatch>, sync: Boolean)
    fun batch(builder: Consumer<LevelDBBatch>) = batch(builder, false)

}
