package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import androidx.lifecycle.LiveData
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class DeviceRepositoryTest {

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var roomDao: RoomDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var deviceFeatureDao: DeviceFeatureDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
                context, AppDatabase::class.java).build()
        roomDao = db.roomDao()
        deviceDao = db.deviceDao()
        deviceFeatureDao = db.deviceFeatureDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun itStoresAndRetrievesADevice() {
        roomDao.insertRoomAndDevices(aRoom(), listOf(aDevice("Power socket")))
        val result = deviceDao.findByRoomAndDeviceName("Living room", "Power socket")
        assertThat(result.id, equalTo("1"))
    }

    @Test
    @Throws(Exception::class)
    fun itListsRoomsAndDevices() {
        roomDao.insertRoomAndDevices(aRoom(), listOf(aDevice("Power socket")))

        val result = roomDao.loadAllWithDevices().blockingObserve()!!
        assertThat(result.size, equalTo(1))
        assertThat(result[0].room?.id, equalTo("1"))
    }

    @Test
    @Throws(Exception::class)
    fun itStoresAndRetrievesHeatingCommands() {
        roomDao.insertRoomAndDevices(aRoom(), listOf(aDevice("Heating")))
        deviceFeatureDao.insertHeatingFeatures(aHeatingFeatureset())

        val result = deviceFeatureDao.getHeatingFeatures("1")
        assertThat(result?.currentTempCmd, equalTo("123"))
    }

    private fun aRoom() = RoomEntity("1", "Living room")

    private fun aDevice(title: String) =
            DeviceEntity("1", title = title, roomId = "1",
                    powerCommand = null,
                    dimCommand = null,
                    powerUsageCommand = null,
                    energyConsumptionCommand = null)

    private fun aHeatingFeatureset(): FeaturesetHeatingEntity =
            FeaturesetHeatingEntity(0, "1",
                    "123", "456", "789", "987", "654")

    private fun <T> LiveData<T>.blockingObserve(): T? {
        var value: T? = null
        val latch = CountDownLatch(1)

        val observer = androidx.lifecycle.Observer<T> { t ->
            value = t
            latch.countDown()
        }

        observeForever(observer)

        latch.await(2, TimeUnit.SECONDS)
        return value
    }

}