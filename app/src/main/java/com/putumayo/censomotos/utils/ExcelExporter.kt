package com.putumayo.censomotos.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.putumayo.censomotos.data.entity.Motocicleta
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

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
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale("es", "CO")).format(Date())
        val municipioNombre = municipioFiltro.replace(" ", "_")
        val nombreArchivo = "Censo_Motos_${municipioNombre}_${fecha}.xlsx"
        return try {
            val workbook = crearWorkbook(motos)
            guardarArchivo(context, workbook, nombreArchivo)
        } catch (e: Exception) {
            ResultadoExportacion(false, mensaje = "Error al generar Excel: ${e.message}")
        }
    }

    private fun crearWorkbook(motos: List<Motocicleta>): XSSFWorkbook {
        val wb = XSSFWorkbook()
        val estiloEncabezado = crearEstiloEncabezado(wb)
        val estiloTotal = crearEstiloTotal(wb)
        val estiloAlternado = crearEstiloAlternado(wb)
        crearHojaDatosCompletos(wb, motos, estiloEncabezado, estiloAlternado)
        crearHojaResumenMunicipio(wb, motos, estiloEncabezado, estiloTotal)
        crearHojaResumenGeneral(wb, motos, estiloEncabezado, estiloTotal)
        return wb
    }

    private fun crearHojaDatosCompletos(
        wb: XSSFWorkbook,
        motos: List<Motocicleta>,
        estiloEncabezado: XSSFCellStyle,
        estiloAlternado: XSSFCellStyle
    ) {
        val hoja = wb.createSheet("Datos Completos")
        hoja.defaultColumnWidth = 16

        val filaTitulo = hoja.createRow(0)
        val celdaTitulo = filaTitulo.createCell(0)
        celdaTitulo.setCellValue("CENSO DE MOTOCICLETAS - PUTUMAYO")
        val estiloTitulo = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        font.fontHeightInPoints = 14
        estiloTitulo.setFont(font)
        estiloTitulo.alignment = HorizontalAlignment.CENTER
        celdaTitulo.cellStyle = estiloTitulo
        hoja.addMergedRegion(CellRangeAddress(0, 0, 0, 6))

        val filaEnc = hoja.createRow(2)
        listOf("ID", "Marca", "Cilindraje (cc)", "Modelo (Año)", "Color", "Municipio", "Fecha/Hora")
            .forEachIndexed { i, titulo ->
                filaEnc.createCell(i).apply {
                    setCellValue(titulo)
                    cellStyle = estiloEncabezado
                }
            }

        motos.forEachIndexed { idx, moto ->
            val fila = hoja.createRow(idx + 3)
            fila.createCell(0).setCellValue(moto.id.toDouble())
            fila.createCell(1).setCellValue(moto.marca)
            fila.createCell(2).setCellValue(moto.cilindraje.toDouble())
            fila.createCell(3).setCellValue(moto.modelo.toDouble())
            fila.createCell(4).setCellValue(moto.color)
            fila.createCell(5).setCellValue(moto.municipio)
            fila.createCell(6).setCellValue(moto.getFechaFormateada())
            if (idx % 2 != 0) {
                (0..6).forEach { fila.getCell(it)?.cellStyle = estiloAlternado }
            }
        }

        val filaTotal = hoja.createRow(motos.size + 4)
        filaTotal.createCell(0).apply { setCellValue("TOTAL:"); cellStyle = estiloEncabezado }
        filaTotal.createCell(1).apply { setCellValue(motos.size.toDouble()); cellStyle = estiloEncabezado }
        (0..6).forEach { hoja.autoSizeColumn(it) }
    }

    private fun crearHojaResumenMunicipio(
        wb: XSSFWorkbook,
        motos: List<Motocicleta>,
        estiloEncabezado: XSSFCellStyle,
        estiloTotal: XSSFCellStyle
    ) {
        val hoja = wb.createSheet("Resumen por Municipio")
        hoja.defaultColumnWidth = 20
        hoja.createRow(0).createCell(0).setCellValue("RESUMEN POR MUNICIPIO")

        val encabezados = hoja.createRow(2)
        listOf("Municipio", "Total Motos", "% del Total").forEachIndexed { i, s ->
            encabezados.createCell(i).apply { setCellValue(s); cellStyle = estiloEncabezado }
        }

        val agrupado = motos.groupBy { it.municipio }
            .map { (mun, lista) -> mun to lista.size }
            .sortedByDescending { it.second }

        agrupado.forEachIndexed { idx, (municipio, total) ->
            val fila = hoja.createRow(idx + 3)
            fila.createCell(0).setCellValue(municipio)
            fila.createCell(1).setCellValue(total.toDouble())
            val pct = if (motos.isNotEmpty()) (total * 100.0 / motos.size) else 0.0
            fila.createCell(2).setCellValue("${"%.1f".format(pct)}%")
        }

        val filaTotal = hoja.createRow(agrupado.size + 4)
        filaTotal.createCell(0).apply { setCellValue("TOTAL"); cellStyle = estiloTotal }
        filaTotal.createCell(1).apply { setCellValue(motos.size.toDouble()); cellStyle = estiloTotal }
        filaTotal.createCell(2).apply { setCellValue("100%"); cellStyle = estiloTotal }
        (0..2).forEach { hoja.autoSizeColumn(it) }
    }

    private fun crearHojaResumenGeneral(
        wb: XSSFWorkbook,
        motos: List<Motocicleta>,
        estiloEncabezado: XSSFCellStyle,
        estiloTotal: XSSFCellStyle
    ) {
        val hoja = wb.createSheet("Resumen General")
        var filaActual = 0
        filaActual = escribirTablaResumen(hoja, motos.groupBy { it.marca }, "POR MARCA", filaActual, estiloEncabezado, estiloTotal)
        filaActual += 2
        filaActual = escribirTablaResumen(hoja, motos.groupBy { "${it.cilindraje} cc" }, "POR CILINDRAJE", filaActual, estiloEncabezado, estiloTotal)
        filaActual += 2
        escribirTablaResumen(hoja, motos.groupBy { it.color }, "POR COLOR", filaActual, estiloEncabezado, estiloTotal)
        (0..2).forEach { hoja.autoSizeColumn(it) }
    }

    private fun escribirTablaResumen(
        hoja: Sheet,
        agrupado: Map<String, List<Motocicleta>>,
        titulo: String,
        filaInicio: Int,
        estiloEncabezado: XSSFCellStyle,
        estiloTotal: XSSFCellStyle
    ): Int {
        hoja.createRow(filaInicio).createCell(0).setCellValue(titulo)
        hoja.createRow(filaInicio + 1).apply {
            createCell(0).apply { setCellValue("Categoria"); cellStyle = estiloEncabezado }
            createCell(1).apply { setCellValue("Total"); cellStyle = estiloEncabezado }
        }
        var row = filaInicio + 2
        var total = 0
        agrupado.entries.sortedByDescending { it.value.size }.forEach { (cat, lista) ->
            hoja.createRow(row).apply {
                createCell(0).setCellValue(cat)
                createCell(1).setCellValue(lista.size.toDouble())
            }
            total += lista.size
            row++
        }
        hoja.createRow(row).apply {
            createCell(0).apply { setCellValue("TOTAL"); cellStyle = estiloTotal }
            createCell(1).apply { setCellValue(total.toDouble()); cellStyle = estiloTotal }
        }
        return row + 1
    }

    private fun crearEstiloEncabezado(wb: XSSFWorkbook): XSSFCellStyle {
        val estilo = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.index
        estilo.setFont(font)
        estilo.fillForegroundColor = IndexedColors.DARK_BLUE.index
        estilo.fillPattern = FillPatternType.SOLID_FOREGROUND
        estilo.alignment = HorizontalAlignment.CENTER
        estilo.borderBottom = BorderStyle.THIN
        return estilo
    }

    private fun crearEstiloTotal(wb: XSSFWorkbook): XSSFCellStyle {
        val estilo = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        estilo.setFont(font)
        estilo.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
        estilo.fillPattern = FillPatternType.SOLID_FOREGROUND
        return estilo
    }

    private fun crearEstiloAlternado(wb: XSSFWorkbook): XSSFCellStyle {
        val estilo = wb.createCellStyle() as XSSFCellStyle
        estilo.fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
        estilo.fillPattern = FillPatternType.SOLID_FOREGROUND
        return estilo
    }

    private fun guardarArchivo(
        context: Context,
        workbook: XSSFWorkbook,
        nombreArchivo: String
    ): ResultadoExportacion {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                ) ?: return ResultadoExportacion(false, mensaje = "No se pudo crear el archivo")
                context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                ResultadoExportacion(true, uri.toString(), nombreArchivo, "Guardado en Downloads: $nombreArchivo")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val file = File(downloadsDir, nombreArchivo)
                FileOutputStream(file).use { workbook.write(it) }
                ResultadoExportacion(true, file.absolutePath, nombreArchivo, "Guardado en: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            ResultadoExportacion(false, mensaje = "Error guardando archivo: ${e.message}")
        } finally {
            workbook.close()
        }
    }
}
