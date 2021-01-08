package com.tealium.adidentifier

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.Environment
import com.tealium.core.Tealium
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.persistence.DataLayer
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AdIdentifierTests {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    lateinit var tealiumContext: TealiumContext
    lateinit var config: TealiumConfig
    lateinit var dataLayer: DataLayer

    @Before
    fun setUp() {
        config = spyk(TealiumConfig(application,
                "test",
                "test",
                Environment.DEV))

        dataLayer = mockk(relaxed = true)
        tealiumContext = TealiumContext(config,
                "someTestId",
                mockk(),
                dataLayer,
                mockk(),
                mockk(),
                mockk())
    }

    @Test
    fun extension_ModuleFactoryReturnsModule() = runBlocking {
        config.modules.add(AdIdentifier)
        val tealium = Tealium.create("test", config)
        delay(1500)
        Assert.assertNotNull(tealium.adIdentifier)
    }

    @Test
    fun adInfoSuccessfulAddToDataLayer() {
        config.modules.add(AdIdentifier)
        val tealium = Tealium.create("test2", config)
        Assert.assertTrue(tealium.dataLayer.contains("google_adid"))
    }

    @Test
    fun adInfoSuccessfulRemovalFromDataLayer() {
        config.modules.add(AdIdentifier)
        val tealium = Tealium.create("test2", config)
        Assert.assertTrue(tealium.dataLayer.contains("google_adid"))

        Thread.sleep(100)
        tealium.adIdentifier?.removeAdInfo()

        Assert.assertFalse(tealium.dataLayer.contains("google_adid"))
    }
}