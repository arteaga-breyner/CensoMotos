package com.putumayo.censomotos.data.repository

import androidx.lifecycle.LiveData
import com.putumayo.censomotos.data.dao.MotocicletaDao
import com.putumayo.censomotos.data.dao.ResumenCampo
import com.putumayo.censomotos.data.dao.ResumenCilindraje
import com.putumayo.censomotos.data.dao.ResumenMunicipio
import com.putumayo.censomotos.data.entity.Motocicleta
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que abstrae el acceso a datos.
 * Los ViewModels interactúan solo con esta clase.
 */
class MotocicletaRepository(private val dao: MotocicletaDao) {

    // Flujo reactivo de todas las motos (se actualiza automáticamente)
    val todasLasMotos: Flow<List<Motocicleta>> = dao.obtenerTodas()
    val totalMotos: LiveData<Int> = dao.contarTodas()

    // ── CRUD ─────────────────────────────────────────────────────────────────

    suspend fun insertar(moto: Motocicleta): Long = dao.insertar(moto)

    suspend fun actualizar(moto: Motocicleta) = dao.actualizar(moto)

    suspend fun eliminar(moto: Motocicleta) = dao.eliminar(moto)

    suspend fun eliminarPorId(id: Long) = dao.eliminarPorId(id)

    suspend fun obtenerPorId(id: Long): Motocicleta? = dao.obtenerPorId(id)

    // ── Estadísticas ─────────────────────────────────────────────────────────

    suspend fun obtenerTodasSync(): List<Motocicleta> = dao.obtenerTodasSync()

    suspend fun contarPorMunicipioAgrupado(): List<ResumenMunicipio> =
        dao.contarPorMunicipioAgrupado()

    suspend fun contarPorMarca(): List<ResumenCampo> = dao.contarPorMarca()

    suspend fun contarPorCilindraje(): List<ResumenCilindraje> = dao.contarPorCilindraje()

    suspend fun contarPorColor(): List<ResumenCampo> = dao.contarPorColor()

    suspend fun contarTodasSync(): Int = dao.contarTodasSync()

    // ── Validación de duplicados ─────────────────────────────────────────────

    suspend fun esDuplicado(
        marca: String,
        cilindraje: Int,
        modelo: Int,
        color: String,
        municipio: String,
        excludeId: Long = 0
    ): Boolean {
        return dao.contarDuplicados(marca, cilindraje, modelo, color, municipio, excludeId) > 0
    }
}
