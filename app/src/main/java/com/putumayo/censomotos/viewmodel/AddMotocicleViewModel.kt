package com.putumayo.censomotos.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.putumayo.censomotos.data.database.CensoDatabase
import com.putumayo.censomotos.data.entity.Motocicleta
import com.putumayo.censomotos.data.repository.MotocicletaRepository
import com.putumayo.censomotos.utils.ValidationHelper
import kotlinx.coroutines.launch

class AddMotocicleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MotocicletaRepository(
        CensoDatabase.getInstance(application).motocicletaDao()
    )

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    private val _errorMensaje = MutableLiveData<String?>()
    val errorMensaje: LiveData<String?> = _errorMensaje

    private val _advertenciaDuplicado = MutableLiveData<Motocicleta?>()
    val advertenciaDuplicado: LiveData<Motocicleta?> = _advertenciaDuplicado

    /**
     * Guarda una moto con validación completa.
     * @param forzar Si es true, guarda aunque sea duplicado.
     */
    fun guardarMoto(
        marca: String,
        cilindrajeStr: String,
        modeloStr: String,
        color: String,
        municipio: String,
        editId: Long = 0,
        forzar: Boolean = false
    ) {
        val errores = ValidationHelper.validarTodo(marca, cilindrajeStr, modeloStr, color, municipio)
        if (errores.isNotEmpty()) {
            _errorMensaje.value = errores.joinToString("\n")
            return
        }

        val cilindraje = cilindrajeStr.toInt()
        val modelo = modeloStr.toInt()

        viewModelScope.launch {
            // Verificar duplicado si no se está forzando
            if (!forzar) {
                val esDup = repository.esDuplicado(marca, cilindraje, modelo, color, municipio, editId)
                if (esDup) {
                    _advertenciaDuplicado.value = Motocicleta(
                        marca = marca, cilindraje = cilindraje,
                        modelo = modelo, color = color, municipio = municipio
                    )
                    return@launch
                }
            }

            val moto = Motocicleta(
                id = editId,
                marca = marca,
                cilindraje = cilindraje,
                modelo = modelo,
                color = color,
                municipio = municipio
            )
            repository.insertar(moto)
            _guardadoExitoso.value = true
        }
    }

    fun cargarParaEditar(id: Long, callback: (Motocicleta?) -> Unit) {
        viewModelScope.launch {
            callback(repository.obtenerPorId(id))
        }
    }

    fun limpiarEstado() {
        _errorMensaje.value = null
        _advertenciaDuplicado.value = null
        _guardadoExitoso.value = false
    }
}
