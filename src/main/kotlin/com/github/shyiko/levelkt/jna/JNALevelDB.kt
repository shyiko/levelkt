package com.github.shyiko.levelkt.jna

import com.github.shyiko.levelkt.LevelDB
import com.github.shyiko.levelkt.LevelDBBatch
import com.github.shyiko.levelkt.LevelDBCursor
import com.github.shyiko.levelkt.LevelDBException
import com.github.shyiko.levelkt.LevelDBRecord
import com.github.shyiko.levelkt.LevelDBSnapshot
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays
import java.util.LinkedList
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
import com.protonail.leveldb.jna.KeyValuePair as NativePair
import com.protonail.leveldb.jna.LevelDB as NativeLevelDB
import com.protonail.leveldb.jna.LevelDBCompressionType as NativeLevelDBCompressionType
import com.protonail.leveldb.jna.LevelDBException as NativeLevelDBException
import com.protonail.leveldb.jna.LevelDBIteratorBase as AbstractNativeLevelDBIterator
import com.protonail.leveldb.jna.LevelDBKeyIterator as NativeLevelDBKeyIterator
import com.protonail.leveldb.jna.LevelDBKeyValueIterator as NativeLevelDBIterator
import com.protonail.leveldb.jna.LevelDBOptions as NativeLevelDBOptions
import com.protonail.leveldb.jna.LevelDBReadOptions as NativeLevelDBReadOptions
import com.protonail.leveldb.jna.LevelDBWriteBatch as NativeLevelDBWriteBatch
import com.protonail.leveldb.jna.LevelDBWriteOptions as NativeLevelDBWriteOptions
import com.protonail.leveldb.jna.Range as NativeRange

/**
 * LevelDB backed by [protonail/leveldb-jna](https://github.com/protonail/leveldb-jna).
 */
class JNALevelDB @JvmOverloads constructor(
    private val path: Path,
    private val config: Config = Config()
) : LevelDB {

    /**
     * @see <a href="https://github.com/google/leveldb/blob/a2fb086d07b7dbd9c4a59fe57646bd465841edd5/include/leveldb/options.h#L31">leveldb::Options</a>
     */
    data class Config(
        val autoRepair: Boolean = false,
        val createIfMissing: Boolean = true, // NOTE: create_if_missing is set to false by default in google/leveldb
        val errorIfExists: Boolean = false,
        val paranoidChecks: Boolean = false,
        val compression: Boolean = true,
        val writeBufferSize: Long = 4 * 1024 * 1204, // 4mb
        val maxOpenFiles: Int = 1000,
        val blockSize: Long = 4096,
        val blockRestartInterval: Int = 16
    ) {

        // java8-compat

        constructor() : this(true)

        fun setAutoRepair(value: Boolean) = this.copy(autoRepair = value)
        fun setCreateIfMissing(value: Boolean) = this.copy(createIfMissing = value)
        fun setErrorIfExists(value: Boolean) = this.copy(errorIfExists = value)
        fun setParanoidChecks(value: Boolean) = this.copy(paranoidChecks = value)
        fun setCompression(value: Boolean) = this.copy(compression = value)
        fun setWriteBufferSize(value: Long) = this.copy(writeBufferSize = value)
        fun setMaxOpenFiles(value: Int) = this.copy(maxOpenFiles = value)
        fun setBlockSize(value: Long) = this.copy(blockSize = value)
        fun setBlockRestartInterval(value: Int) = this.copy(blockRestartInterval = value)

    }

    @JvmOverloads
    constructor(path: String, config: Config = Config()) : this(Paths.get(path), config)

    private companion object {
        val IDENTITY = fun (v: ByteArray) = v
        val RECORD = fun (v: NativePair) = LevelDBRecord(v.key, v.value)
        val ACCEPT = fun (_: Any) = true

        val logger = Logger.getLogger(JNALevelDB::class.java.name)

        inline fun <R> attempt(cb: () -> R) =
            try { cb() } catch (e: NativeLevelDBException) { throw LevelDBException(e) }

        inline fun <T: AutoCloseable, R> T.use(cb: (value: T) -> R) =
            try { cb(this) } finally { this.close() }

        operator fun ByteArray.compareTo(r: ByteArray): Int {
            var i = 0
            val n = Math.min(this.size, r.size)
            while (i < n) { val d = this[i] - r[i]; if (d != 0) return d; i++ }; return this.size - r.size
        }

        fun ByteArray.startsWith(seq: ByteArray): Boolean =
            (0 until seq.size).all { this[it] == seq[it] }
    }

    /**
     * @see <a href="https://github.com/google/leveldb/blob/a2fb086d07b7dbd9c4a59fe57646bd465841edd5/include/leveldb/options.h#L161">leveldb::ReadOptions</a>
     */
    private val READ_CACHE = NativeLevelDBReadOptions()
    private val READ = NativeLevelDBReadOptions().apply { isFillCache = false }
    private val READ_VERIFY = NativeLevelDBReadOptions().apply { isVerifyChecksum = true; isFillCache = false }
    private val READ_VERIFY_CACHE = NativeLevelDBReadOptions().apply { isVerifyChecksum = true }

    /**
     * @see <a href="https://github.com/google/leveldb/blob/a2fb086d07b7dbd9c4a59fe57646bd465841edd5/include/leveldb/options.h#L187">leveldb::ReadOptions</a>
     */
    private val WRITE_ASYNC = NativeLevelDBWriteOptions()
    private val WRITE_SYNC = NativeLevelDBWriteOptions().apply { isSync = true }

    @Volatile
    private var closed = false

    private fun read(verifyChecksum: Boolean, fillCache: Boolean) =
        if (verifyChecksum) {
            if (fillCache) READ_VERIFY_CACHE else READ_VERIFY
        } else {
            if (fillCache) READ_CACHE else READ
        }
    private fun write(sync: Boolean) = if (sync) WRITE_SYNC else WRITE_ASYNC

    private fun nativeDBOpt() =
        NativeLevelDBOptions().apply {
            isCreateIfMissing = config.createIfMissing
            isErrorIfExists = config.errorIfExists
            isParanoidChecks = config.paranoidChecks
            compressionType = if (config.compression) NativeLevelDBCompressionType.SnappyCompression else
                NativeLevelDBCompressionType.NoCompression
            writeBufferSize = config.writeBufferSize
            maxOpenFiles = config.maxOpenFiles
            blockSize = config.blockSize
            blockRestartInterval = config.blockRestartInterval
        }

    private val nativeDB = nativeDBOpt().use { nativeDBOpt ->
        if (config.createIfMissing) {
            Files.createDirectories(path)
        }
        if (config.autoRepair) {
            attempt { NativeLevelDB.repair(path.toString(), nativeDBOpt) }
        }
        attempt { NativeLevelDB(path.toString(), nativeDBOpt) }
    }

    override fun containsKey(key: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): Boolean =
        attempt {
            NativeLevelDBKeyIterator(nativeDB, read(verifyChecksum, fillCache)).use { iterator ->
                iterator.seekToKey(key)
                iterator.hasNext() && Arrays.equals(key, iterator.next())
            }
        }

    override fun keySlice(
        start: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<ByteArray> =
        slice(attempt { NativeLevelDBKeyIterator(nativeDB, read(verifyChecksum, fillCache)) }, IDENTITY,
            ACCEPT, start, offset, limit)

    override fun keySlice(
        start: ByteArray,
        end: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<ByteArray> =
        slice(attempt { NativeLevelDBKeyIterator(nativeDB, read(verifyChecksum, fillCache)) }, IDENTITY,
            { it < end }, start, offset, limit)

    override fun keySliceByPrefix(
        prefix: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<ByteArray> =
        slice(attempt { NativeLevelDBKeyIterator(nativeDB, read(verifyChecksum, fillCache)) }, IDENTITY,
            { it.startsWith(prefix) }, prefix, offset, limit)

    override fun keyCursor(verifyChecksum: Boolean, fillCache: Boolean): LevelDBCursor<ByteArray> =
        JNALevelDBCursor(attempt { NativeLevelDBKeyIterator(nativeDB, read(verifyChecksum, fillCache)) }, IDENTITY)

    private fun <N, R> slice(
        iterator: AbstractNativeLevelDBIterator<N>,
        transform: (N) -> R,
        accept: (N) -> Boolean,
        start: ByteArray,
        offset: Int,
        limit: Int
    ): List<R> = attempt {
        iterator.seekToKey(start)
        val result = ArrayList<R>()
        val actualLimit = offset + if (limit <= 0) Int.MAX_VALUE else limit
        var i = 0
        while (iterator.hasNext() && i < actualLimit) {
            val entry = iterator.next()
            if (!accept(entry)) {
                break
            }
            if (offset <= i) {
                result.add(transform(entry))
            }
            i++
        }
        return result
    }

    override fun slice(
        start: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<LevelDBRecord> =
        slice(attempt { NativeLevelDBIterator(nativeDB, read(verifyChecksum, fillCache)) }, RECORD,
            ACCEPT, start, offset, limit)

    override fun slice(
        start: ByteArray,
        end: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<LevelDBRecord> =
        slice(attempt { NativeLevelDBIterator(nativeDB, read(verifyChecksum, fillCache)) }, RECORD,
            { it.key < end }, start, offset, limit)

    override fun sliceByPrefix(
        prefix: ByteArray,
        offset: Int,
        limit: Int,
        verifyChecksum: Boolean,
        fillCache: Boolean
    ): List<LevelDBRecord> =
        slice(attempt { NativeLevelDBIterator(nativeDB, read(verifyChecksum, fillCache)) }, RECORD,
            { it.key.startsWith(prefix) }, prefix, offset, limit)

    override fun cursor(verifyChecksum: Boolean, fillCache: Boolean): LevelDBCursor<LevelDBRecord> =
        JNALevelDBCursor(attempt { NativeLevelDBIterator(nativeDB, read(verifyChecksum, fillCache)) }, RECORD)

    override fun get(key: ByteArray, verifyChecksum: Boolean, fillCache: Boolean): ByteArray? =
        attempt { nativeDB.get(key, read(verifyChecksum, fillCache)) }

    override fun snapshot(): LevelDBSnapshot = throw UnsupportedOperationException()

    override fun put(key: ByteArray, value: ByteArray, sync: Boolean) {
        attempt { nativeDB.put(key, value, write(sync)) }
    }

    override fun del(key: ByteArray, sync: Boolean) {
        attempt { nativeDB.delete(key, write(sync)) }
    }

    override fun batch(sync: Boolean, builder: LevelDBBatch.() -> Unit) {
        val batch = LinkedLevelDBBatch()
        builder(batch)
        NativeLevelDBWriteBatch().use { nativeBatch ->
            for (op in batch) {
                when (op) {
                    is LinkedLevelDBBatch.Operation.Put -> nativeBatch.put(op.key, op.value)
                    is LinkedLevelDBBatch.Operation.Del -> nativeBatch.delete(op.key)
                }
            }
            attempt { nativeDB.write(nativeBatch, write(sync)) }
        }
    }

    override fun batch(builder: Consumer<LevelDBBatch>, sync: Boolean) = batch(sync) { builder.accept(this) }

    override fun property(key: String): String = attempt { nativeDB.property(key) }

    override fun approximateSize(start: ByteArray, end: ByteArray): Long =
        attempt { nativeDB.approximateSizes(NativeRange(start, end))[0] }

    override fun compactRange(start: ByteArray, end: ByteArray) {
        attempt { nativeDB.compactRange(start, end) }
    }

    override fun destroy() {
        try {
            close()
        } finally {
            nativeDBOpt().use { attempt { NativeLevelDB.destroy(path.toString(), it) } }
        }
    }

    override fun close() {
        closed = true
        attempt {
            for (nativeObj in arrayOf(
                READ_CACHE, READ, READ_VERIFY, READ_VERIFY_CACHE, WRITE_ASYNC, WRITE_SYNC
            )) {
                nativeObj.close()
            }
            nativeDB.close()
        }
    }

    protected fun finalize() {
        if (!closed) {
            val ref = "${javaClass.simpleName}@${System.identityHashCode(this)}"
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Memory leak detected ($ref wasn't properly closed)")
            }
            try { close() } catch (e: Exception) {} // scavenge whatever we can
        }
    }

    private class LinkedLevelDBBatch : LevelDBBatch, LinkedList<LinkedLevelDBBatch.Operation>() {

        @Suppress("ArrayInDataClass")
        internal sealed class Operation {
            data class Put(val key: ByteArray, val value: ByteArray) : Operation()
            data class Del(val key: ByteArray) : Operation()
        }

        override fun put(key: ByteArray, value: ByteArray) = apply { add(Operation.Put(key, value)) }
        override fun del(key: ByteArray) = apply { add(Operation.Del(key)) }

    }

    private class JNALevelDBCursor<N, R>(
        val iterator: AbstractNativeLevelDBIterator<N>,
        val transform: (N) -> R
    ) : LevelDBCursor<R> {

        companion object {
            private val logger = Logger.getLogger(JNALevelDBCursor::class.java.name)
        }

        @Volatile
        private var closed = false

        override fun first(): R? = attempt {
            iterator.seekToFirst()
            if (iterator.hasNext()) transform(iterator.next()) else null
        }

        override fun prev(): R? = throw UnsupportedOperationException()

        override fun seek(key: ByteArray): R? = attempt {
            iterator.seekToKey(key)
            if (iterator.hasNext()) transform(iterator.next()) else null
        }

        override fun next(): R? = attempt { if (iterator.hasNext()) transform(iterator.next()) else null }

        override fun last(): R? = attempt {
            iterator.seekToLast()
            if (iterator.hasNext()) transform(iterator.next()) else null
        }

        override fun close() {
            closed = true
            attempt { iterator.close() }
        }

        protected fun finalize() {
            if (!closed) {
                val ref = "${javaClass.simpleName}@${System.identityHashCode(this)}"
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("Memory leak detected ($ref wasn't properly closed)")
                }
                try { close() } catch (e: Exception) {} // scavenge whatever we can
            }
        }

    }

}
