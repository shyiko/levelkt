<h1 align="center">
levelkt
</h1>

<p align="center">
<a href="https://travis-ci.org/shyiko/levelkt"><img src="https://travis-ci.org/shyiko/levelkt.svg?branch=master" alt="Build Status"></a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22levelkt%22"><img src="http://img.shields.io/badge/maven_central-0.1.0-blue.svg?style=flat" alt="Maven Central"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="ktlint"></a>
</p>

<p align="center">
<a href="https://github.com/google/leveldb">LevelDB</a> client for Kotlin and/or Java 8+.
</p>

Initially project aimed to provide an alternative API for [fusesource/leveldbjni](https://github.com/fusesource/leveldbjni)'s JNI layer fixing some of the issues around thread-safety, inefficient backward traversal, absent key-only iteration, Java 8 support. Later on, leveldbjni was replaced 
with [protonail/leveldb-jna](https://github.com/protonail/leveldb-jna). 

Compared to pure leveldb-jna, levelkt 
- **hides the complexity** associated with managing LevelDB Read/Write options   
(while **optimizing number of JNA calls**),
- provides convenient **API for range/prefix queries, existence checks** and 
- makes it easier **not to leak resources** (you only need to close LevelDB, LevelDBSnapshot and LevelDBCursor instances (it will also warn you if you don't)).

## Usage

#### Kotlin

```xml
<dependency>
  <groupId>com.github.shyiko.levelkt</groupId>
  <artifactId>levelkt</artifactId>
  <version>0.1.0</version>
</dependency>
```

> Main.kt

```kotlin
import com.github.shyiko.levelkt.jna.JNALevelDB

fun main(args: Array<String>) {
    JNALevelDB("/tmp/0.leveldb").use { levelDB ->
    
        levelDB.put("key1".toByteArray(), "value1".toByteArray())
        levelDB.put("key2.1".toByteArray(), "value2.1".toByteArray())
        levelDB.put("key2.2".toByteArray(), "value2.2".toByteArray())
        levelDB.put("key3".toByteArray(), "value3".toByteArray())

        assert(levelDB.get("key1".toByteArray())?.let { String(it) } == "value1")

        assert(levelDB.keySlice("key".toByteArray(), offset = 1, limit = 2).map { String(it) } == 
            listOf("key2.1", "key2.2"))
        assert(levelDB.keySlice("key2".toByteArray(), "key3".toByteArray()).map { String(it) } == 
            listOf("key2.1", "key2.2"))
        assert(levelDB.keySliceByPrefix("key2".toByteArray()).map { String(it) } == 
            listOf("key2.1", "key2.2"))

        levelDB.keyCursor().use { cursor ->
            assert(cursor.seek("key2".toByteArray())?.let { String(it) } == "key2.1")
            assert(cursor.next()?.let { String(it) } == "key2.2")
        }

        // slice/sliceByPrefix/cursor look exactly the same as keySlice/keySliceByPrefix/keyCursor
        // the only difference - they operate with LevelDBRecord instead of ByteArray

        levelDB.batch {
            put("key1".toByteArray(), "value1.updated".toByteArray())
            del("key3".toByteArray())
        }

        levelDB.del("key1".toByteArray())
        
    }
}
```

#### Java 8+

> `com.github.shyiko.levelkt:levelkt` with `kalvanized` classifier is identical to the regular `com.github.shyiko.levelkt:levelkt`
  except that you don't need `org.jetbrains.kotlin:kotlin-stdlib` (or its transitive dependencies).

```xml
<dependency>
  <groupId>com.github.shyiko.levelkt</groupId>
  <artifactId>levelkt</artifactId>
  <version>0.1.0</version>
  <classifier>kalvanized</classifier>
</dependency>
```

> Main.java

```java
import com.github.shyiko.levelkt.LevelDB;
import com.github.shyiko.levelkt.jna.JNALevelDB;

public class Main {

    public static void main(String[] args) throws Exception {
        try (LevelDB levelDB = new JNALevelDB("/tmp/0.levedb")) {

            levelDB.batch(batch -> batch
                .put("key1".getBytes(), "value1".getBytes())
                .put("key2.1".getBytes(), "value2.1".getBytes())
                .put("key2.2".getBytes(), "value2.2".getBytes())
                .put("key3".getBytes(), "value3".getBytes())
            );

            assert levelDB
                .sliceByPrefix("key2".getBytes())
                .stream()
                .map(record -> new String(record.getKey()) + "=" + new String(record.getValue()))
                .collect(Collectors.toList())
                .equals(Arrays.asList("key2.1=value2.1", "key2.2=value2.2"));

            // for more, see Kotlin example above (API is mostly the same)

        }
    }
}
```

## Development

```sh
git clone https://github.com/shyiko/levelkt && cd levelkt
./mvnw # shows how to build, test, etc. project
```

## Legal

All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2017 Stanley Shyiko.
