package com.tealium.media.segments

import com.tealium.media.ChapterKey
import java.util.*

class Chapter(var name: String,
              var position: Int? = null,
              var duration: Long? = null,
              var skipped: Boolean? = false,
              var metadata: Any? = null,
              var startTime: Long? = null,
              private val uuid: String = UUID.randomUUID().toString()) : Segment {

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            duration = System.currentTimeMillis() - it
        }
    }

    override fun skip() {
        end()
        skipped = true
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data[ChapterKey.NAME] = name

        duration?.let { data[ChapterKey.DURATION] = it }
        position?.let { data[ChapterKey.POSITION] = it }
        startTime?.let { data[ChapterKey.START_TIME] = it }
        metadata?.let { data[ChapterKey.METADATA] = it }

        return data.toMap()
    }
}