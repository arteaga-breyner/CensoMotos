package com.putumayo.censomotos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.putumayo.censomotos.data.entity.Motocicleta
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre la tabla motocicletas.
 * Usa Flow/LiveData para observación reactiva de cambios.
 */
@Dao
interface MotocicletaDao {

    // ── Inserciones ──────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(moto: Motocicleta): Long

    // ── Actualizaciones ──────────────────────────────────────────────────────

    @Update
    suspend fun actualizar(moto: Motocicleta)

    // ── Eliminaciones ────────────────────────────────────────────────────────

    @Delete
    suspend fun eliminar(moto: Motocicleta)

    @Query("DELETE FROM motocicletas WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    @Query("DELETE FROM motocicletas")
    suspend fun eliminarTodas()

    // ── Consultas básicas ────────────────────────────────────────────────────

    @Query("SELECT * FROM motocicletas ORDER BY timestamp DESC")
    fun obtenerTodas(): Flow<List<Motocicleta>>

    @Query("SELECT * FROM motocicletas ORDER BY timestamp DESC")
    suspend fun obtenerTodasSync(): List<Motocicleta>

    @Query("SELECT * FROM motocicletas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Motocicleta?

    // ── Consultas por municipio ──────────────────────────────────────────────

    @Query("SELECT * FROM motocicletas WHERE municipio = :municipio ORDER BY timestamp DESC")
    fun obtenerPorMunicipio(municipio: String): Flow<List<Motocicleta>>

    @Query("SELECT COUNT(*) FROM motocicletas WHERE municipio = :municipio")
    suspend fun contarPorMunicipio(municipio: String): Int

    // ── Estadísticas ────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM motocicletas")
    fun contarTodas(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM motocicletas")
    suspend fun contarTodasSync(): Int

    @Query("SELECT municipio, COUNT(*) as total FROM motocicletas GROUP BY municipio ORDER BY total DESC")
    suspend fun contarPorMunicipioAgrupado(): List<ResumenMunicipio>

    @Query("SELECT marca, COUNT(*) as total FROM motocicletas GROUP BY marca ORDER BY total DESC")
    suspend fun contarPorMarca(): List<ResumenCampo>

    @Query("SELECT cilindraje, COUNT(*) as total FROM motocicletas GROUP BY cilindraje ORDER BY total DESC")
    suspend fun contarPorCilindraje(): List<ResumenCilindraje>

    @Query("SELECT color, COUNT(*) as total FROM motocicletas GROUP BY color ORDER BY total DESC")
    suspend fun contarPorColor(): List<ResumenCampo>

    // ── Detección de duplicados ──────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM motocicletas
        WHERE marca = :marca
        AND cilindraje = :cilindraje
        AND modelo = :modelo
        AND color = :color
        AND municipio = :municipio
        AND id != :excludeId
    """)
    suspend fun contarDuplicados(
        marca: String,
        cilindraje: Int,
        modelo: Int,
        color: String,
        municipio: String,
        excludeId: Long = 0
    ): Int
}

// ── Clases de resultado para consultas agrupadas ─────────────────────────────

data class ResumenMunicipio(
    val municipio: String,
    val total: Int
)

data class ResumenCampo(
    val marca: String,
    val total: Int
)

data class ResumenCilindraje(
    val cilindraje: Int,
    val total: Int
)
