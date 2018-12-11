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

    @Transaction
    @Query("SELECT * FROM scene WHERE id = :id")
    fun findSceneById(id: Long): SceneWithActions

    @Query("SELECT count(id) FROM scene")
    fun count(): Int

    @Query("UPDATE scene SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScene(scene: SceneEntity) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAction(action: SceneActionEntity) : Long

    @Transaction
    fun insertSceneWithActions(scene: SceneEntity, actions: List<SceneActionEntity>) {
        val newId = insertScene(scene)

        actions.forEach { action ->
            action.sceneId = newId
            insertAction(action)
        }
    }

    @Delete
    fun delete(scene: SceneEntity?, actions: List<SceneActionEntity>?)
}

@Entity(tableName = "scene", indices = arrayOf(
        Index("chosen_count", name = "idx_scene_chosen_count"),
        Index("title", name = "idx_scene_title", unique = true)
))
data class SceneEntity(

        @ColumnInfo(name = "title")
        val title: String
) : Serializable {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "chosen_count")
    var chosenCount: Int = 0
}

@Entity(tableName = "scene_action")
data class SceneActionEntity(

        @PrimaryKey
        val id: String,

        @ColumnInfo(name = "feature_value")
        val value: Int
) : Serializable {

    @ForeignKey(entity = SceneEntity::class,
            parentColumns = ["id"],
            childColumns = ["scene_id"])
    @ColumnInfo(name = "scene_id")
    var sceneId: Long? = null
}

data class SceneWithActions(

        @Embedded
        var scene: SceneEntity? = null,

        @Relation(parentColumn = "id", entityColumn = "scene_id", entity = SceneActionEntity::class)
        var actions: List<SceneActionEntity>? = null
)