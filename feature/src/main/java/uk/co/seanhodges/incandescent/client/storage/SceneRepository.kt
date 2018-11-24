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
    fun insertScene(scene: SceneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAction(action: SceneActionEntity)

    @Transaction
    fun insertSceneWithActions(scene: SceneEntity, actions: List<SceneActionEntity>) {
        insertScene(scene)

        actions.forEach { action ->
            action.sceneId = scene.id
            insertAction(action)
        }
    }
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
    var id: Int? = -1

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
    var sceneId: Int? = -1
}

data class SceneWithActions(

        @Embedded
        var scene: SceneEntity? = null,

        @Relation(parentColumn = "id", entityColumn = "scene_id", entity = SceneActionEntity::class)
        var actions: List<SceneActionEntity>? = null
)