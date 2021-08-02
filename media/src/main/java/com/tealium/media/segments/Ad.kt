package com.tealium.media.segments

import com.tealium.media.AdKey
import com.tealium.media.ChapterKey
import com.tealium.media.Media
import com.tealium.media.SegmentKey
import java.util.*

data class Ad(val id: String? = null,
              val name: String? = null,
              var duration: Double? = null,
              var position: Int? = null,
              var advertiser: String? = null,
              var creativeId: String? = null,
              var campaignId: String? = null,
              var placementId: String? = null,
              var siteId: String? = null,
              var creativeUrl: String? = null,
              var numberOfLoads: Int? = null,
              var pod: String? = null,
              var playerName: String? = null,
              var metadata: Map<String, Any>? = null) : Segment {

    val uuid: String = UUID.randomUUID().toString()

    private val adName: String = name ?: uuid
    private var startTime: Long? = null
    private var skipped: Boolean = false

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            if (duration == null) {
                duration = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
            }
        }
    }

    override fun skip() {
        end()
        startTime?.let {
            skipped = true
        }
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf(
                AdKey.NAME to adName,
                AdKey.UUID to uuid,
                AdKey.SKIPPED to skipped
        )

        id?.let { data[AdKey.ID] = it }
        position?.let { data[AdKey.POSITION] = it }
        advertiser?.let { data[AdKey.ADVERTISER] = it }
        creativeId?.let { data[AdKey.CREATIVE_ID] = it }
        campaignId?.let { data[AdKey.CAMPAIGN_ID] = it }
        placementId?.let { data[AdKey.PLACEMENT_ID] = it }
        siteId?.let { data[AdKey.SITE_ID] = it }
        creativeUrl?.let { data[AdKey.CREATIVE_URL] = it }
        numberOfLoads?.let { data[AdKey.NUMBER_OF_LOADS] = it }
        pod?.let { data[AdKey.POD] = it }
        playerName?.let { data[AdKey.PLAYER_NAME] = it }
        duration?.let { data[AdKey.DURATION] = it }
        metadata?.let { data[SegmentKey.METADATA] = it.toMap() }

        return data.toMap()
    }
}