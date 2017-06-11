package com.github.shyiko.levelkt

interface LevelDBBatch {

    fun put(key: ByteArray, value: ByteArray): LevelDBBatch
    fun del(key: ByteArray): LevelDBBatch

}
