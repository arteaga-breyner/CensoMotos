package com.putumayo.censomotos.ui.summary

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.putumayo.censomotos.databinding.ActivitySummaryBinding
import com.putumayo.censomotos.utils.ExcelExporter
import com.putumayo.censomotos.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding
    private val viewModel: MainViewModel by viewModels()
    private var ultimaRutaExcel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        configurarBotones()
        cargarResumen()
    }

    private fun configurarBotones() {
        binding.btnExportarExcel.setOnClickListener { exportarExcel() }
        binding.btnCompartir.setOnClickListener { compartirExcel() }
    }

    private fun cargarResumen() {
        lifecycleScope.launch {
            val motos = viewModel.obtenerTodasSync()
            val total = motos.size

            runOnUiThread {
                binding.tvTotalGeneral.text = total.toString()
            }

            // Por municipio
            val porMunicipio = motos.groupBy { it.municipio }
                .map { (mun, lista) -> mun to lista.size }
                .sortedByDescending { it.second }

            val coloresMunicipio = mapOf(
                "Mocoa" to "#1565C0",
                "Puerto Asís" to "#2E7D32",
                "Puerto Caicedo" to "#6A1B9A",
                "Villagarzón" to "#E65100"
            )

            runOnUiThread {
                binding.llTablaMunicipios.removeAllViews()
                // Encabezado
                agregarFilaTabla(binding.llTablaMunicipios, "Municipio", "Motos", esEncabezado = true)
                porMunicipio.forEach { (mun, cnt) ->
                    agregarFilaTabla(binding.llTablaMunicipios, mun, cnt.toString(),
                        color = coloresMunicipio[mun])
                }
                agregarFilaTabla(binding.llTablaMunicipios, "TOTAL", total.toString(), esTotal = true)
            }

            // Por marca
            val porMarca = viewModel.contarPorMarca()
            runOnUiThread {
                binding.llTablaMarcas.removeAllViews()
                agregarFilaTabla(binding.llTablaMarcas, "Marca", "Total", esEncabezado = true)
                porMarca.forEach { agregarFilaTabla(binding.llTablaMarcas, it.marca, it.total.toString()) }
            }

            // Por color
            val porColor = viewModel.contarPorColor()
            runOnUiThread {
                binding.llTablaColores.removeAllViews()
                agregarFilaTabla(binding.llTablaColores, "Color", "Total", esEncabezado = true)
                porColor.forEach { agregarFilaTabla(binding.llTablaColores, it.marca, it.total.toString()) }
            }
        }
    }

    private fun agregarFilaTabla(
        container: LinearLayout,
        col1: String,
        col2: String,
        esEncabezado: Boolean = false,
        esTotal: Boolean = false,
        color: String? = null
    ) {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 12, 16, 12)
            if (esEncabezado) setBackgroundColor(Color.parseColor("#1565C0"))
            else if (esTotal) setBackgroundColor(Color.parseColor("#FFF9C4"))
        }

        val params1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        val params2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val tv1 = TextView(this).apply {
            text = col1
            textSize = if (esEncabezado) 15f else 16f
            setTextColor(if (esEncabezado) Color.WHITE else Color.parseColor(color ?: "#212121"))
            if (esEncabezado || esTotal) setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = params1
        }

        val tv2 = TextView(this).apply {
            text = col2
            textSize = if (esEncabezado) 15f else 16f
            setTextColor(if (esEncabezado) Color.WHITE else Color.parseColor("#212121"))
            if (esEncabezado || esTotal) setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.END
            layoutParams = params2
        }

        fila.addView(tv1)
        fila.addView(tv2)
        container.addView(fila)

        // Línea separadora
        if (!esEncabezado) {
            val divider = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(Color.parseColor("#E0E0E0"))
            }
            container.addView(divider)
        }
    }

    private fun exportarExcel() {
        binding.btnExportarExcel.isEnabled = false
        binding.btnExportarExcel.text = "⏳ Generando..."

        lifecycleScope.launch {
            val motos = viewModel.obtenerTodasSync()
            val resultado = ExcelExporter.exportar(this@SummaryActivity, motos)

            runOnUiThread {
                binding.btnExportarExcel.isEnabled = true
                binding.btnExportarExcel.text = "📥  EXPORTAR A EXCEL"

                if (resultado.exito) {
                    ultimaRutaExcel = resultado.rutaArchivo
                    binding.btnCompartir.visibility = android.view.View.VISIBLE
                    Snackbar.make(
                        binding.root,
                        "✅ Excel guardado: ${resultado.nombreArchivo}",
                        Snackbar.LENGTH_LONG
                    ).setAction("Compartir") { compartirExcel() }.show()
                } else {
                    Snackbar.make(binding.root, "❌ ${resultado.mensaje}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun compartirExcel() {
        val ruta = ultimaRutaExcel ?: run {
            Snackbar.make(binding.root, "Primero exporte el archivo", Snackbar.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = if (ruta.startsWith("content://")) {
                Uri.parse(ruta)
            } else {
                FileProvider.getUriForFile(
                    this, "${packageName}.fileprovider", File(ruta)
                )
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Censo de Motocicletas Putumayo")
                putExtra(Intent.EXTRA_TEXT, "Adjunto el archivo de censo de motocicletas.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Compartir Excel via..."))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error al compartir: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
