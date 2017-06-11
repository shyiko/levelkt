package com.github.shyiko.levelkt

import java.io.Closeable

interface LevelDBCursor<out T> : Closeable {

    fun first(): T?
    fun prev(): T?
    fun seek(key: ByteArray): T?
    fun next(): T?
    fun last(): T?

}
