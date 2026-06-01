package com.putumayo.censomotos.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.putumayo.censomotos.data.database.CensoDatabase
import com.putumayo.censomotos.data.entity.Motocicleta
import com.putumayo.censomotos.data.repository.MotocicletaRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MotocicletaRepository(
        CensoDatabase.getInstance(application).motocicletaDao()
    )

    // LiveData/Flow expuestos a la UI
    val todasLasMotos = repository.todasLasMotos.asLiveData()
    val totalMotos: LiveData<Int> = repository.totalMotos

    private val _resumenMunicipios = MutableLiveData<Map<String, Int>>()
    val resumenMunicipios: LiveData<Map<String, Int>> = _resumenMunicipios

    private val _mensajeUI = MutableLiveData<String?>()
    val mensajeUI: LiveData<String?> = _mensajeUI

    init {
        cargarResumenMunicipios()
    }

    fun cargarResumenMunicipios() {
        viewModelScope.launch {
            val lista = repository.contarPorMunicipioAgrupado()
            _resumenMunicipios.value = lista.associate { it.municipio to it.total }
        }
    }

    fun eliminarMoto(moto: Motocicleta) {
        viewModelScope.launch {
            repository.eliminar(moto)
            cargarResumenMunicipios()
            _mensajeUI.value = "Registro eliminado"
        }
    }

    fun limpiarMensaje() {
        _mensajeUI.value = null
    }

    suspend fun obtenerTodasSync() = repository.obtenerTodasSync()
    suspend fun contarPorMarca() = repository.contarPorMarca()
    suspend fun contarPorCilindraje() = repository.contarPorCilindraje()
    suspend fun contarPorColor() = repository.contarPorColor()
}
