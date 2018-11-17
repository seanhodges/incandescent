package uk.co.seanhodges.incandescent.client.storage

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Embedded
import java.io.Serializable

@Dao
interface SceneDao {

    @Transaction
    @Query("SELECT * FROM scene ORDER BY chosen_count DESC, id")
    fun loadAllWithActions(): LiveData<List<SceneWithActions>>

    @Query("SELECT count(id) FROM scene")
    fun count(): Int

    @Query("UPDATE scene SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSceneWithActions(room: SceneEntity, devices: List<SceneActionEntity>)
}

@Entity(tableName = "scene", indices = arrayOf(
        Index("chosen_count", name = "idx_scene_chosen_count"),
        Index("title", name = "idx_scene_title", unique = true)
))
data class SceneEntity(

        @PrimaryKey(autoGenerate = true)
        var id: Int,

        @ColumnInfo(name = "title")
        var title: String,

        @ColumnInfo(name = "chosen_count")
        var chosenCount: Int = 0
) : Serializable

@Entity(tableName = "scene_action")
data class SceneActionEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "feature_value")
        var type: String,

        @ForeignKey(entity = SceneEntity::class,
                parentColumns = ["id"],
                childColumns = ["scene_id"])
        @ColumnInfo(name = "scene_id")
        var sceneId: Int
) : Serializable

data class SceneWithActions(

        @Embedded
        var scene: SceneEntity? = null,

        @Relation(parentColumn = "id", entityColumn = "scene_id", entity = SceneActionEntity::class)
        var actions: List<SceneActionEntity>? = null
)