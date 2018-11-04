package uk.co.seanhodges.incandescent.client.selection

import androidx.room.Room
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.runner.AndroidJUnitRunner
import org.hamcrest.CoreMatchers.*
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
@MediumTest
class DeviceRepositoryTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().context
    private val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    private lateinit var repo : DeviceRepository

    @Before
    fun setUp() {
        repo = DeviceRepository(ctx, db)
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    @Test
    fun itDetectsANewDb() {
        assertThat(repo.isNewDB(), `is`(equalTo(true)))
    }

    @Test
    fun itDetectsAnExistingDb() {
        assertThat(repo.isNewDB(), `is`(equalTo(true)))
    }

    @Test
    fun itStoresACollectionOfRoomsAndDevices() {
        val result = MutableList<RoomWithDevices>(2, init = {
            RoomWithDevices(aRoom(it), Arrays.asList(aDevice(1, it)))
        })
        repo.addRoomAndDevices(result.get(0))
        repo.addRoomAndDevices(result.get(1))

        assertThat(db.roomDao().count(), `is`(equalTo(2)))
        assertThat(repo.getAllRooms()[0].room?.id, `is`(equalTo("1")))
        assertThat(repo.getAllRooms()[1].room?.id, `is`(equalTo("0")))
    }

    @Test
    fun itRetrievesACollectionOfRoomsInSortedOrder() {
        repeat(2) {
            db.roomDao().insertRoomAndDevices(aRoomWithChosenCount(it, 1 + it), emptyList())
        }

        assertThat(repo.getAllRooms().size, `is`(equalTo(2)))
        assertThat(repo.getAllRooms()[0].room?.id, `is`(equalTo("1")))
        assertThat(repo.getAllRooms()[1].room?.id, `is`(equalTo("0")))
    }

    @Test
    fun itRetrievesACollectionOfDevicesInSortedOrder() {
        repeat(2) { deviceId ->
            db.roomDao().insertRoomAndDevices(aRoom(0), listOf(aDeviceWithChosenCount(deviceId, 0, 1 + deviceId)))
        }

        assertThat(repo.getAllRooms()[0].getDevicesInOrder().size, `is`(equalTo(2)))
        assertThat(repo.getAllRooms()[0].getDevicesInOrder()[0].id, `is`(equalTo("01")))
        assertThat(repo.getAllRooms()[0].getDevicesInOrder()[1].id, `is`(equalTo("00")))
    }

    private val aRoom: (Int) -> RoomEntity = { id ->
        RoomEntity("$id", "Room $id") }

    private val aRoomWithChosenCount: (Int, Int) -> RoomEntity = { id, pos ->
        RoomEntity("$id", "Room $id", pos) }

    private val aDevice: (Int, Int) -> DeviceEntity = { id, roomId ->
        DeviceEntity("$roomId$id", "Device $id", "light", "", "", "$roomId") }

    private val aDeviceWithChosenCount: (Int, Int, Int) -> DeviceEntity = { id, roomId, pos ->
        DeviceEntity("$roomId$id", "Device $id", "light", "", "", "$roomId", pos) }
}