package com.putumayo.censomotos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entidad principal para almacenamiento de motocicletas en Room Database.
 * Cada registro representa una moto censada en campo.
 */
@Entity(tableName = "motocicletas")
data class Motocicleta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val marca: String,           // Honda, Yamaha, Suzuki, Bajaj, Kawasaki, Otro
    val cilindraje: Int,         // 50-500 cc
    val modelo: Int,             // Año: 1990-2026
    val color: String,           // Roja, Azul, Negra, Blanca, Gris, etc.
    val municipio: String,       // Mocoa, Puerto Asís, Puerto Caicedo, Villagarzón

    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Retorna la fecha/hora formateada para mostrar en la UI y Excel.
     */
    fun getFechaFormateada(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO"))
        return sdf.format(Date(timestamp))
    }

    /**
     * Descripción compacta para la lista de registros.
     */
    fun getDescripcionCorta(): String = "$marca $cilindraje cc - $color - $modelo"
}
