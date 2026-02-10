package com.jvf.monitorfunctional.database
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Insert
    suspend fun inserir(video: VideoLog)

    // Pega os últimos 50 vídeos (para não pesar a tela depois)
    @Query("SELECT * FROM historico_videos ORDER BY timestamp DESC LIMIT 50")
    fun lerTodos(): Flow<List<VideoLog>>

    @Query("DELETE FROM historico_videos")
    suspend fun limparTudo()
}