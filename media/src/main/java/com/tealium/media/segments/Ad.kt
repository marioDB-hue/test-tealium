package com.tealium.media.segments

import com.tealium.media.AdKey
import com.tealium.media.Media
import java.util.*

data class Ad(val id: String,
              val name: String? = null,
              var position: Int? = null,
              var advertiser: String? = null,
              var creativeId: String? = null,
              var campaignId: String? = null,
              var placementId: String? = null,
              var siteId: String? = null,
              var creativeUrl: String? = null,
              var numberOfLoads: Int? = null,
              var pod: String? = null,
              var playerName: String? = null) : Segment {

    val uuid: String = UUID.randomUUID().toString()

    private val adName: String = name ?: uuid
    private var startTime: Long? = null
    private var duration: Double? = null
    private var skipped: Boolean = false

    override fun start() {
        startTime = System.currentTimeMillis()
    }

    override fun end() {
        startTime?.let {
            duration = Media.timeMillisToSeconds(System.currentTimeMillis() - it)
        }
    }

    override fun skip() {
        end()
        skipped = true
    }

    override fun segmentInfo(): Map<String, Any> {
        val data = mutableMapOf(
                AdKey.ID to id,
                AdKey.NAME to adName,
                AdKey.SKIPPED to skipped
        )

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
        uuid.let { data[AdKey.UUID] = it }

        return data.toMap()
    }
}