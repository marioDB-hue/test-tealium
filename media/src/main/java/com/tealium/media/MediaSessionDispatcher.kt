package com.tealium.media

import com.tealium.core.TealiumContext
import com.tealium.dispatcher.TealiumEvent
import com.tealium.media.segments.Segment

class MediaSessionDispatcher(private val context: TealiumContext) : MediaDispatcher {

    @Suppress("UNCHECKED_CAST")
    override fun track(event: String, mediaContent: MediaContent, segment: Segment?, customData: Map<String, Any>?) {
        val data = mutableMapOf<String, Any>()

        data.putAll(mediaContent.toMap())
        customData?.let {
            data.putAll(it)
        }

        segment?.let {
            // merge MediaContent metadata with Chapter metadata (chapter data overwrites mediaContent data)
            if (segment.segmentInfo().containsKey(ChapterKey.METADATA)) {
                (segment.segmentInfo()[ChapterKey.METADATA] as? Map<*,*>)?.let { map ->
                    mediaContent.metadata.putAll(map as Map<String, Any>)
                }
            }

            data.putAll(segment.segmentInfo().filter { (key, value) ->
                key != ChapterKey.METADATA
            })
        }

        context.track(TealiumEvent(event, data))
    }
}