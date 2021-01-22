package com.tealium.autotracking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tealium.core.*
import com.tealium.core.messaging.EventRouter
import com.tealium.core.messaging.MessengerService
import com.tealium.core.network.HttpClient
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//@RunWith(AndroidJUnit4::class)
class AutoTrackingInstrumentedTests {

    @RelaxedMockK
    lateinit var mockTealium: Tealium

    @RelaxedMockK
    lateinit var mockEvents: MessengerService

    lateinit var tealiumContext: TealiumContext
    val application = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val config = TealiumConfig(application, "account", "profile", Environment.DEV)
        tealiumContext = TealiumContext(config, "visitor-1", Logger, mockk(), HttpClient(config), mockEvents, mockTealium)
    }


    @Test
    fun factory_ReturnsNewInstance() {
        val autoTracking1 = AutoTracking.create(tealiumContext)
        val autoTracking2 = AutoTracking.create(tealiumContext)

        assertNotNull(autoTracking1)
        assertNotNull(autoTracking2)
        assertNotSame(autoTracking1, autoTracking2)
    }

    @Test
    fun extension_ReturnsModule() = runBlocking {
        val config = TealiumConfig(application, "tealiummobile", "test", Environment.DEV)
        config.modules.add(Modules.AutoTracking)
        val tealium = Tealium.create("test", config)

        delay(1500)
        assertNotNull(tealium.autoTracking)
    }
}