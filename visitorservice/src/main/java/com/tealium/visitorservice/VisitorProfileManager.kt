package com.tealium.visitorservice

import com.tealium.core.*
import com.tealium.core.messaging.*
import com.tealium.core.network.ResourceRetriever
import com.tealium.dispatcher.Dispatch
import kotlinx.coroutines.delay
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VisitorUpdatedMessenger(private val visitorProfile: VisitorProfile) : Messenger<VisitorUpdatedListener>(VisitorUpdatedListener::class) {
    override fun deliver(listener: VisitorUpdatedListener) {
        listener.onVisitorUpdated(visitorProfile)
    }
}

interface VisitorUpdatedListener : ExternalListener {
    fun onVisitorUpdated(visitorProfile: VisitorProfile)
}

interface VisitorProfileManager {
    val visitorProfile: VisitorProfile
    suspend fun requestVisitorProfile()
}

class VisitorManager(private val context: TealiumContext,
                            private val refreshInterval: Long =
                                    context.config.visitorServiceRefreshInterval
                                            ?: DEFAULT_REFRESH_INTERVAL,
                            private val visitorServiceUrl: String =
                                    context.config.overrideVisitorServiceUrl
                                            ?: DEFAULT_VISITOR_SERVICE_TEMPLATE,
                            private val loader: Loader = JsonLoader(context.config.application)) : VisitorProfileManager, DispatchSendListener, BatchDispatchSendListener {

    private val file = File(context.config.tealiumDirectory, VISITOR_PROFILE_FILENAME)

    val isUpdating = AtomicBoolean(false)
    private var lastUpdate: Long = -1L
    private var visitorId = context.visitorId
        set(value) {
            if (field != value) {
                field = value
                // URL needs updating
                resourceRetriever = createResourceRetriever()
            }
        }
    private var resourceRetriever: ResourceRetriever = createResourceRetriever()

    override val visitorProfile: VisitorProfile
        get() = _visitorProfile

    private var _visitorProfile: VisitorProfile = loadCachedProfile() ?: VisitorProfile()
        private set(value) {
            field = value
            context.events.send(VisitorUpdatedMessenger(value))
        }

    private fun createResourceRetriever(): ResourceRetriever {
        return ResourceRetriever(context.config, generateVisitorServiceUrl(), context.httpClient).apply {
            useIfModifed = false
            maxRetries = 1
            refreshInterval = 0
        }
    }

    internal fun generateVisitorServiceUrl(): String {
        return visitorServiceUrl.replace(PLACEHOLDER_ACCOUNT, context.config.accountName)
                .replace(PLACEHOLDER_PROFILE, context.config.profileName)
                .replace(PLACEHOLDER_VISITOR_ID, visitorId)
    }

    fun loadCachedProfile(): VisitorProfile? {
        return loader.loadFromFile(file)?.let {
            return try {
                val jsonObject = JSONObject(it)
                VisitorProfile.fromJson(jsonObject)
            } catch (jex: JSONException) {
                Logger.dev(BuildConfig.TAG, "Failed to read cached visitor profile.")
                null
            }
        }
    }

    fun saveVisitorProfile(visitorProfile: VisitorProfile) {
        file.writeText(VisitorProfile.toJson(visitorProfile).toString(), Charsets.UTF_8)
    }

    override suspend fun onDispatchSend(dispatch: Dispatch) {
        updateProfile()
    }

    override suspend fun onBatchDispatchSend(dispatches: List<Dispatch>) {
        updateProfile()
    }

    suspend fun updateProfile() {
        if (refreshIntervalReached()) {
            requestVisitorProfile()
        } else {
            Logger.dev(BuildConfig.TAG, "Visitor Profile refresh interval not reached, will not update.")
        }
    }

    override suspend fun requestVisitorProfile() {
        // no need if it's already being updated, but we won't adhere to the refreshInterval here.
        if (isUpdating.compareAndSet(false, true)) {
            // Check for any updates to visitorId.
            visitorId = context.visitorId

            for (i in 1..5) {
                Logger.dev(BuildConfig.TAG, "Fetching visitor profile for ${context.visitorId}.")

                val json = resourceRetriever.fetch()
                if (json != null && json != "{}") {
                    Logger.dev(BuildConfig.TAG, "Fetched visitor profile: $json.")

                    val newProfile = VisitorProfile.fromJson(JSONObject(json))
                    if (visitorProfile.totalEventCount != newProfile.totalEventCount) {
                        lastUpdate = System.currentTimeMillis()
                        saveVisitorProfile(newProfile)
                        _visitorProfile = newProfile
                        break
                    } else {
                        Logger.dev(BuildConfig.TAG, "Visitor Profile found but it was stale.")
                    }
                } else {
                    Logger.dev(BuildConfig.TAG, "Invalid visitor profile found.")
                }
                // back off a bit
                delay(750L * i)
            }
            isUpdating.set(false)

        } else {
            Logger.dev(BuildConfig.TAG, "Visitor profile is already being updated.")
        }
    }

    private fun refreshIntervalReached(): Boolean {
        return lastUpdate + TimeUnit.SECONDS.toMillis(refreshInterval) <= System.currentTimeMillis()
    }

    companion object {
        const val VISITOR_PROFILE_FILENAME = "visitor_profile.json"
        const val DEFAULT_REFRESH_INTERVAL = 300L

        // url replacememts
        const val PLACEHOLDER_ACCOUNT = "{{account}}"
        const val PLACEHOLDER_PROFILE = "{{profile}}"
        const val PLACEHOLDER_VISITOR_ID = "{{visitorId}}"

        const val DEFAULT_VISITOR_SERVICE_TEMPLATE = "https://visitor-service.tealiumiq.com/$PLACEHOLDER_ACCOUNT/$PLACEHOLDER_PROFILE/$PLACEHOLDER_VISITOR_ID"
    }
}