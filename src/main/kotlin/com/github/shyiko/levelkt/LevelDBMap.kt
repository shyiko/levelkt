package com.github.shyiko.levelkt

import com.github.shyiko.levelkt.jna.JNALevelDB
import java.io.Closeable
import java.nio.file.Path

/**
 * @param db the JNALevelDB
 */
abstract class LevelDBMap<K, V>(private val db: JNALevelDB) : MutableMap<K, V>, Closeable {

    /**
     * @param path the path of the database
     * @param config the JNALevelDB configurations
     */
    constructor(path: Path, config: JNALevelDB.Config = JNALevelDB.Config()): this(JNALevelDB(path, config))

    /**
     * The mutable entry of this map.
     *
     * @property key the key
     * @property values the value
     */
    inner class MutableEntry(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {

        /**
         * Changes the value associated with the key of this entry.
         *
         * @return the previous value corresponding to the key.
         */
        override fun setValue(newValue: V): V = this@LevelDBMap.put(key, newValue)!!
    }

    /**
     * @param obj the key
     *
     * @return the byte array representation of the [obj]
     */
    abstract fun serializeKey(obj: K): ByteArray

    /**
     * @param obj the value
     *
     * @return the byte array representation of the [obj]
     */
    abstract fun serializeValue(obj: V): ByteArray

    /**
     * @param obj the byte array representation of the key
     *
     * @return the key
     */
    abstract fun deserializeKey(obj: ByteArray): K

    /**
     * @param obj the byte array representation of the value
     *
     * @return the key
     */
    abstract fun deserializeValue(obj: ByteArray): V

    /**
     * Returns `true` if the map contains the specified [key].
     */
    override fun containsKey(key: K): Boolean = this.db.containsKey(this.serializeKey(key))

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     */
    override fun containsValue(value: V): Boolean {

        val cursor = this.db.keyCursor()
        var element = cursor.next()

        while (element != null) {

            val key = deserializeKey(element)

            if (this[key] == value) {

                cursor.close()
                return true
            }

            element = cursor.next()
        }

        cursor.close()

        return false
    }

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    override fun get(key: K): V? = this.db.get(this.serializeKey(key))?.let { this.deserializeValue(it) }

    /**
     * Returns `true` if the map is empty (contains no elements), `false` otherwise.
     */
    override fun isEmpty(): Boolean = this.db.keyCursor().use { it.first() == null }

    /**
     * Returns a [MutableSet] of all key/value pairs in this map.
     */
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() {

        val entries = mutableSetOf<MutableMap.MutableEntry<K, V>>()

        this.db.keyCursor().use { cursor ->

            var element = cursor.next()

            while (element != null) {

                val key = deserializeKey(element)

                entries.add(MutableEntry(key, this[key]!!))

                element = cursor.next()
            }
        }

        return entries
    }

    /**
     * @return the number of entries in this map
     */
    override val size: Int get() {

        var counter = 0

        this.db.keyCursor().use { cursor ->
            while (cursor.next() != null) counter++
        }

        return counter
    }

    /**
     * Returns a [MutableSet] of all keys in this map.
     */
    override val keys: MutableSet<K> get() {

        val keys = mutableSetOf<K>()

        this.db.keyCursor().use { cursor ->
            var element = cursor.next()

            while (element != null) {
                keys.add(deserializeKey(element))
                element = cursor.next()
            }
        }

        return keys
    }

    /**
     * Returns a [MutableCollection] of all values in this map. Note that this collection may contain duplicate values.
     */
    override val values: MutableCollection<V> get() {

        val values = mutableListOf<V>()

        this.db.keyCursor().use { cursor ->
            var element = cursor.next()

            while (element != null) {

                val key = deserializeKey(element)

                values.add(this[key]!!)

                element = cursor.next()
            }
        }

        return values
    }

    /**
     * Removes all elements from this map.
     */
    override fun clear() {

        this.db.keyCursor().use { cursor ->
            var key = cursor.next()

            while (key != null) {
                this.db.del(key)
                key = cursor.next()
            }
        }
    }

    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    override fun put(key: K, value: V): V? {

        val prev: V? = this[key]

        this.db.put(serializeKey(key), serializeValue(value))

        return prev
    }

    /**
     * Updates this map with key/value pairs from the specified map [from].
     */
    override fun putAll(from: Map<out K, V>) {

        from.forEach { key, value ->
            this[key] = value
        }
    }

    /**
     * Removes the specified key and its corresponding value from this map.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    override fun remove(key: K): V? {

        val prev: V? = this[key]

        this.db.del(serializeKey(key))

        return prev
    }

    /**
     * Close the DB.
     */
    override fun close() = this.db.close()
}
