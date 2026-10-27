package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AeroPulseDatabase
import com.example.data.repository.AeroPulseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AeroPulseDatabase
    private lateinit var repository: AeroPulseRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AeroPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AeroPulseRepository(database.aeroPulseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context matches AeroPulse`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AeroPulse", appName)
    }

    @Test
    fun `seed database and persist custom station and barometer logs`() = runBlocking {
        repository.ensureSeeded()
        val initialStations = repository.stationsFlow.first()
        val initialProfiles = repository.activityProfilesFlow.first()

        assertEquals(6, initialStations.size)
        assertEquals(5, initialProfiles.size)
        assertTrue(initialStations.any { it.name == "Chamonix Aiguille" && it.isPrimary })

        // Add a custom station and verify primary switch
        val addedId = repository.addStation(
            name = "Aoraki Mount Cook",
            region = "Canterbury",
            country = "New Zealand",
            latitude = -43.5950,
            longitude = 170.1418,
            elevationMeters = 760,
            makePrimary = true
        )
        val updatedStations = repository.stationsFlow.first()
        assertEquals(7, updatedStations.size)
        val primary = updatedStations.first { it.isPrimary }
        assertEquals(addedId, primary.id)
        assertEquals("Aoraki Mount Cook", primary.name)

        // Log barometer snapshot
        repository.logBarometerSnapshot(
            pressureHpa = 928.5,
            altitudeMeters = 760.0,
            note = "Hooker Valley Trailhead"
        )
        val logs = repository.barometerLogsFlow.first()
        assertEquals(1, logs.size)
        assertEquals("Hooker Valley Trailhead", logs.first().note)
    }
}
