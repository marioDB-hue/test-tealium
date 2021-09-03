package com.tealium.core

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.persistence.DataLayer
import com.tealium.core.persistence.Expiry
import com.tealium.dispatcher.TealiumEvent
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TealiumTests {

    lateinit var tealium: Tealium
    lateinit var mockDataLayer: DataLayer
    lateinit var mockTealium: Tealium
    val application = ApplicationProvider.getApplicationContext<Application>()
    val configWithNoModules = TealiumConfig(
        application,
        "test",
        "test",
        Environment.DEV
    )
    val configWithQRTraceDisabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.qrTraceEnabled = false
            return config
        }

    val configWithQRTraceEnabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.qrTraceEnabled = true
            return config
        }

    val configWithDeepLinkingDisabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.deepLinkTrackingEnabled = false
            return config
        }

    val configWithDeepLinkingEnabled
        get(): TealiumConfig {
            val config = configWithNoModules
            config.deepLinkTrackingEnabled = true
            return config
        }

    @Before
    fun setUp() {
        mockDataLayer = mockk(relaxed = true)
        tealium = Tealium.create("name", configWithNoModules)
        mockTealium = spyk(tealium)
    }

    @After
    fun tearDown() {
        Tealium.names().forEach {
            Tealium.destroy(it)
        }
    }

    @Test
    fun testConfig_LogLevel_SetFromEnvironment() {
        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            )
        )
        assertEquals(Logger.logLevel, LogLevel.PROD)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.QA
            )
        )
        assertEquals(Logger.logLevel, LogLevel.QA)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.DEV
            )
        )
        assertEquals(Logger.logLevel, LogLevel.DEV)
    }

    @Test
    fun testConfig_LogLevel_OverridesEnvironment() {
        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.PROD
            ).apply { logLevel = LogLevel.DEV }
        )
        assertEquals(Logger.logLevel, LogLevel.DEV)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.QA
            ).apply { logLevel = LogLevel.PROD }
        )
        assertEquals(Logger.logLevel, LogLevel.PROD)

        Tealium.create(
            "loglevel", TealiumConfig(
                application,
                configWithNoModules.accountName,
                configWithNoModules.profileName,
                Environment.DEV
            ).apply { logLevel = LogLevel.QA }
        )
        assertEquals(Logger.logLevel, LogLevel.QA)
    }

    @Test
    fun testVisitorIdIsGenerated() {
        assertNotNull(tealium.visitorId)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testVisitorIdIsReset() {
        val vid = tealium.visitorId
        assertNotNull(vid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
        val storedVid = tealium.dataLayer.getString("tealium_visitor_id")

        val resetVid = tealium.resetVisitorId()
        val storedResetVid = tealium.dataLayer.getString("tealium_visitor_id")
        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(32, tealium.visitorId.length)
        assertEquals(tealium.visitorId, tealium.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun existingVisitorId() {
        val config = TealiumConfig(
            application,
            "testAccount",
            "testProfile",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        val test = Tealium.create("tester", config)

        val vid = test.visitorId
        assertNotNull(vid)
        assertEquals("testExistingVisitorId", test.visitorId)
        assertEquals(test.visitorId, test.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun resetExistingVisitorId() {
        val config = TealiumConfig(
            application,
            "testAccount2",
            "testProfile2",
            Environment.DEV
        )
        config.existingVisitorId = "testExistingVisitorId"
        val teal = Tealium.create("tester", config)

        val vid = teal.visitorId
        val storedVid = teal.dataLayer.getString("tealium_visitor_id")
        assertEquals("testExistingVisitorId", vid)

        val resetVid = teal.resetVisitorId()
        val storedResetVid = teal.dataLayer.getString("tealium_visitor_id")

        assertNotEquals(vid, resetVid)
        assertNotEquals(storedVid, storedResetVid)
        assertEquals(teal.visitorId, teal.dataLayer.getString("tealium_visitor_id"))
    }

    @Test
    fun testCallbackGetsExecuted() = runBlocking {
        var hasBeenCalled = false

        val tealium = Tealium.create("name", configWithNoModules) {
            hasBeenCalled = true
        }
        if (!hasBeenCalled) {
            delay(1000)
            assertTrue(hasBeenCalled)
        }
    }

    @Test
    fun testHandleDeepLink() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithDeepLinkingEnabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val queryParams = mapOf<String, Any>(
            "_dfgsdftwet" to "sdgfdgfdfg",
            "_fbclid" to "1234567",
            "tealium" to "cdh",
            "bool" to true,
            "int" to 123
        )
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val builtURI = uri.build()
        runBlocking {
            mockActivityObserverListener.handleDeepLink(builtURI)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                "deep_link_url",
                builtURI.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkDoesNotRunFromActivityResumeIfDisabled() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithDeepLinkingDisabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val queryParams = mapOf<String, Any>(
            "_dfgsdftwet" to "sdgfdgfdfg",
            "_fbclid" to "1234567",
            "tealium" to "cdh",
            "bool" to true,
            "int" to 123
        )
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val activity: Activity = mockk()
        val intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            mockActivityObserverListener.onActivityResumed(activity)
            delay(100)
        }
        verify(exactly = 0) {
            mockDataLayer.putString(
                "deep_link_url",
                builtURI.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleDeepLinkFromActivityResume() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithDeepLinkingEnabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val queryParams = mapOf<String, Any>(
            "_dfgsdftwet" to "sdgfdgfdfg",
            "_fbclid" to "1234567",
            "tealium" to "cdh",
            "bool" to true,
            "int" to 123
        )
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val activity: Activity = mockk()
        val intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            mockActivityObserverListener.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                "deep_link_url",
                builtURI.toString(),
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleJoinTraceFromActivityResume() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val traceId = "abc123"
        val queryParams = mapOf<String, Any>("tealium_trace_id" to traceId)
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val activity: Activity = mockk()
        val intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            mockActivityObserverListener.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 1) {
            mockDataLayer.putString(
                CoreConstant.TRACE_ID,
                traceId,
                Expiry.SESSION
            )
        }
    }

    @Test
    fun testHandleLeaveTraceFromActivityResume() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        every { context.tealium } returns mockTealium
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val traceId = "abc123"
        val queryParams = mapOf<String, Any>(
            "tealium_trace_id" to traceId,
            CoreConstant.LEAVE_TRACE_QUERY_PARAM to "true"
        )
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val activity: Activity = mockk()
        val intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            mockActivityObserverListener.onActivityResumed(activity)
            delay(100)
        }
        verify(exactly = 1) { mockDataLayer.remove(CoreConstant.TRACE_ID) }
    }

    @Test
    fun testKillVisitorSessionFromActivityResume() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        every { context.track(TealiumEvent("kill_visitor_session")) } just Runs

        runBlocking {
            val mockActivityObserverListener = DeepLinkHandler(context)


            val builder = Uri.Builder()
            val traceId = "abc123"
            val queryParams = mapOf<String, Any>(
                "tealium_trace_id" to traceId,
                CoreConstant.KILL_VISITOR_SESSION to "true"
            )
            val uri = builder.scheme("https")
                .authority("tealium.com")
                .path("/")

            queryParams.forEach { entry ->
                uri.appendQueryParameter(entry.key, entry.value.toString())
            }
            val activity: Activity = mockk()
            val intent = Intent()
            val builtURI = uri.build()
            intent.data = builtURI
            every { activity.intent } returns intent
            runBlocking {
                mockActivityObserverListener.onActivityResumed(activity)
                delay(100)
            }

            verify(exactly = 1) {
                context.track(
                    withArg { dispatch ->
                        assertEquals("kill_visitor_session", dispatch["tealium_event"])
                        assertEquals("kill_visitor_session", dispatch["event"])
                    })
            }
        }
    }

    @Test
    fun testHandleJoinTraceDoesNotRunFromActivityResumeIfDisabled() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceDisabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)

        val builder = Uri.Builder()
        val traceId = "abc123"
        val queryParams = mapOf<String, Any>("tealium_trace_id" to traceId)
        val uri = builder.scheme("https")
            .authority("tealium.com")
            .path("/")

        queryParams.forEach { entry ->
            uri.appendQueryParameter(entry.key, entry.value.toString())
        }
        val activity: Activity = mockk()
        val intent = Intent()
        val builtURI = uri.build()
        intent.data = builtURI
        every { activity.intent } returns intent
        runBlocking {
            mockActivityObserverListener.onActivityResumed(activity)
            delay(100)
        }

        verify(exactly = 0) { mockDataLayer.putString("tealium_trace_id", traceId, Expiry.SESSION) }
    }

    @Test
    fun testJoinTrace() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)
        mockActivityObserverListener.joinTrace("abc123")
        verify(exactly = 1) { mockDataLayer.putString("cp.trace_id", "abc123", Expiry.SESSION) }
    }

    @Test
    fun testLeaveTrace() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        val mockActivityObserverListener = DeepLinkHandler(context)
        mockActivityObserverListener.leaveTrace()
        verify(exactly = 1) { mockDataLayer.remove("cp.trace_id") }
    }

    @Test
    fun testKillVisitorSessionTriggersTrackCall() {
        val context = mockk<TealiumContext>()
        every { context.config } returns configWithQRTraceEnabled
        every { context.dataLayer } returns mockDataLayer
        every { context.track(TealiumEvent("kill_visitor_session")) } just Runs
        val mockActivityObserverListener = DeepLinkHandler(context)
        mockActivityObserverListener.killTraceVisitorSession()

        verify(exactly = 1) {
            context.track(
                withArg { dispatch ->
                    assertEquals("kill_visitor_session", dispatch["tealium_event"])
                    assertEquals("kill_visitor_session", dispatch["event"])
                })
        }
    }

    @Test
    fun testCompanion_CreateAndGet() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
    }

    @Test
    fun testCompanion_CreateAndDestroy() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))
    }

    @Test
    fun testCompanion_CreateMultipleWithSameConfig() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        val instance2 = Tealium.create("instance_2", configWithNoModules)
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
        assertSame(instance2, Tealium["instance_2"])
        assertSame(instance2, Tealium.get("instance_2"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))

        assertNotNull(Tealium["instance_2"])
        assertNotNull(Tealium.get("instance_2"))
    }

    @Test
    fun testCompanion_CreateMultipleWithDifferentConfig() {
        val instance = Tealium.create("instance_1", configWithNoModules)
        val instance2 = Tealium.create(
            "instance_2",
            TealiumConfig(application, "test2", "test2", Environment.DEV)
        )
        assertSame(instance, Tealium["instance_1"])
        assertSame(instance, Tealium.get("instance_1"))
        assertSame(instance2, Tealium["instance_2"])
        assertSame(instance2, Tealium.get("instance_2"))

        Tealium.destroy("instance_1")
        assertNull(Tealium["instance_1"])
        assertNull(Tealium.get("instance_1"))

        assertNotNull(Tealium["instance_2"])
        assertNotNull(Tealium.get("instance_2"))
    }

    @Test
    fun testCompanion_NamesReturnsAllNames() {
        Tealium.create("instance_1", configWithNoModules)
        Tealium.create("instance_2", TealiumConfig(application, "test2", "test2", Environment.DEV))
        val names = Tealium.names()

        assertTrue(names.contains("instance_1"))
        assertTrue(names.contains("instance_2"))
    }
}