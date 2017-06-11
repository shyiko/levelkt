package com.github.shyiko.levelkt

data class LevelDBRecord(val key: ByteArray, val value: ByteArray) {

    override fun equals(other: Any?): Boolean =
        other is LevelDBRecord && key.contentEquals(other.key) && value.contentEquals(other.value)

    override fun hashCode(): Int = 31 * key.contentHashCode() + value.contentHashCode()

}
