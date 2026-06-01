package com.putumayo.censomotos.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.putumayo.censomotos.data.entity.Motocicleta
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta los datos del censo a CSV (compatible con Excel).
 * Sin dependencias externas — funciona en cualquier Android 8+.
 */
object CsvExporter {

    data class ResultadoExportacion(
        val exito: Boolean,
        val rutaArchivo: String = "",
        val nombreArchivo: String = "",
        val mensaje: String = ""
    )

    fun exportar(
        context: Context,
        motos: List<Motocicleta>,
        municipioFiltro: String = "Todos"
    ): ResultadoExportacion {

        if (motos.isEmpty()) {
            return ResultadoExportacion(false, mensaje = "No hay datos para exportar")
        }

        val fecha = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale("es", "CO")).format(Date())
        val municipioNombre = municipioFiltro.replace(" ", "_")
        val nombreArchivo = "Censo_Motos_${municipioNombre}_${fecha}.csv"

        return try {
            val contenido = generarCsv(motos)
            guardarArchivo(context, contenido, nombreArchivo)
        } catch (e: Exception) {
            ResultadoExportacion(false, mensaje = "Error al generar CSV: ${e.message}")
        }
    }

    private fun generarCsv(motos: List<Motocicleta>): String {
        val sb = StringBuilder()

        // BOM para que Excel abra correctamente con tildes
        sb.append('﻿')

        // Encabezado
        sb.appendLine("ID,Marca,Tipo,Cilindraje (cc),Modelo (Año),Color,Municipio,Fecha/Hora")

        // Datos
        motos.forEach { moto ->
            sb.appendLine(
                "${moto.id}," +
                "\"${moto.marca}\"," +
                "\"${moto.tipo}\"," +
                "${moto.cilindraje}," +
                "${moto.modelo}," +
                "\"${moto.color}\"," +
                "\"${moto.municipio}\"," +
                "\"${moto.getFechaFormateada()}\""
            )
        }

        // Línea en blanco
        sb.appendLine()

        // Resumen por municipio
        sb.appendLine("RESUMEN POR MUNICIPIO")
        sb.appendLine("Municipio,Total,% del Total")
        val porMunicipio = motos.groupBy { it.municipio }
            .map { (mun, lista) -> mun to lista.size }
            .sortedByDescending { it.second }
        porMunicipio.forEach { (mun, total) ->
            val pct = "%.1f".format(total * 100.0 / motos.size)
            sb.appendLine("\"$mun\",$total,$pct%")
        }
        sb.appendLine("TOTAL,${motos.size},100%")

        sb.appendLine()

        // Resumen por marca
        sb.appendLine("RESUMEN POR MARCA")
        sb.appendLine("Marca,Total")
        motos.groupBy { it.marca }
            .map { (m, l) -> m to l.size }
            .sortedByDescending { it.second }
            .forEach { (marca, total) -> sb.appendLine("\"$marca\",$total") }

        sb.appendLine()

        // Resumen por tipo
        sb.appendLine("RESUMEN POR TIPO")
        sb.appendLine("Tipo,Total")
        motos.groupBy { it.tipo.ifBlank { "Sin tipo" } }
            .map { (t, l) -> t to l.size }
            .sortedByDescending { it.second }
            .forEach { (tipo, total) -> sb.appendLine("\"$tipo\",$total") }

        sb.appendLine()

        // Resumen por color
        sb.appendLine("RESUMEN POR COLOR")
        sb.appendLine("Color,Total")
        motos.groupBy { it.color }
            .map { (c, l) -> c to l.size }
            .sortedByDescending { it.second }
            .forEach { (color, total) -> sb.appendLine("\"$color\",$total") }

        return sb.toString()
    }

    private fun guardarArchivo(
        context: Context,
        contenido: String,
        nombreArchivo: String
    ): ResultadoExportacion {

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: return ResultadoExportacion(false, mensaje = "No se pudo crear el archivo")

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use { it.write(contenido) }
                }

                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

                ResultadoExportacion(
                    exito = true,
                    rutaArchivo = uri.toString(),
                    nombreArchivo = nombreArchivo,
                    mensaje = "Archivo guardado en Descargas: $nombreArchivo"
                )
            } else {
                // Android 8-9
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = File(dir, nombreArchivo)
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos, Charsets.UTF_8).use { it.write(contenido) }
                }
                ResultadoExportacion(
                    exito = true,
                    rutaArchivo = file.absolutePath,
                    nombreArchivo = nombreArchivo,
                    mensaje = "Archivo guardado en: ${file.absolutePath}"
                )
            }
        } catch (e: Exception) {
            ResultadoExportacion(false, mensaje = "Error guardando: ${e.message}")
        }
    }
}
