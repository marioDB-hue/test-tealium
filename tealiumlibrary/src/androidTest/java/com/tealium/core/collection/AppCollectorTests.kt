package com.tealium.core.collection

import AppCollectorConstants.APP_UUID
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import com.tealium.test.OpenForTesting
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OpenForTesting
class AppCollectorTests {

    @MockK
    lateinit var tealiumContext: TealiumContext

    @MockK
    lateinit var dataLayer: DataLayer

    @MockK
    lateinit var config: TealiumConfig

    lateinit var context: Application

    val account = "teal-account"
    val profile = "teal-profile"
    val environment = Environment.DEV
    val dataSource = "teal-data-source"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        every { config.accountName } returns account
        every { config.profileName } returns profile
        every { config.environment } returns environment
        every { config.dataSourceId } returns dataSource
        every { config.application } returns context
        every { tealiumContext.config } returns config
        every { tealiumContext.dataLayer } returns dataLayer
    }

    @Test
    fun testAppCollector() = runBlocking {
        val appCollector = AppCollector(context, tealiumContext.dataLayer)
        val data = appCollector.collect()

        assertNotNull(data[AppCollectorConstants.APP_NAME])
        assertNotNull(data[AppCollectorConstants.APP_BUILD])
        assertNotNull(data[AppCollectorConstants.APP_VERSION])

        assertTrue(data[AppCollectorConstants.APP_RDNS] is String)
        assertTrue((data[AppCollectorConstants.APP_RDNS] as String).startsWith("com.tealium"))

        assertTrue(data[AppCollectorConstants.APP_MEMORY_USAGE] is Long)
        assertTrue((data[AppCollectorConstants.APP_MEMORY_USAGE] as Long) > 0)
    }

    @Test
    fun testAppUuid_GetsReturnedWhenNotNull() {
        every { dataLayer.getString(APP_UUID) } returns "my_uuid"
        val appCollector = AppCollector(context.applicationContext, tealiumContext.dataLayer)
        assertEquals("my_uuid", appCollector.appUuid)
    }

    @Test
    fun testAppUuid_GetsGeneratedWhenNull() {
        every { dataLayer.getString(APP_UUID) } returns null
        every { dataLayer.putString(any(), any(), any()) } just Runs
        val appCollector = AppCollector(context.applicationContext, tealiumContext.dataLayer)
        assertNotNull(appCollector.appUuid)
        assertEquals(36, appCollector.appUuid.length)
    }

    @Test
    fun testFactoryCreation() {
        val factory = AppCollector
        assertNotNull(factory.create(tealiumContext))
    }
}