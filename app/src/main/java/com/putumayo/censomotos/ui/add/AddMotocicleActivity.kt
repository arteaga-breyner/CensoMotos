package com.putumayo.censomotos.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.putumayo.censomotos.data.entity.Motocicleta
import com.putumayo.censomotos.databinding.ActivityAddMotocicletaBinding
import com.putumayo.censomotos.utils.ValidationHelper
import com.putumayo.censomotos.utils.VoiceEntityExtractor
import com.putumayo.censomotos.utils.VoiceRecognizer
import com.putumayo.censomotos.viewmodel.AddMotocicleViewModel
import java.util.Calendar

class AddMotocicleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EDIT_ID = "extra_edit_id"
    }

    private lateinit var binding: ActivityAddMotocicletaBinding
    private val viewModel: AddMotocicleViewModel by viewModels()
    private lateinit var voiceRecognizer: VoiceRecognizer

    private var editId: Long = 0
    private var ultimoArchivoExcel: String? = null

    // Datos extraídos por voz
    private var marcaVoz: String? = null
    private var cilindrajeVoz: Int? = null
    private var modeloVoz: Int? = null
    private var colorVoz: String? = null

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarEscucha()
        else Snackbar.make(binding.root, "Se necesita permiso de micrófono", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMotocicletaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editId = intent.getLongExtra(EXTRA_EDIT_ID, 0)
        voiceRecognizer = VoiceRecognizer(this)

        configurarSpinners()
        configurarTabs()
        configurarBotones()
        observarViewModel()

        if (editId > 0) {
            supportActionBar?.title = "Editar Moto"
            cargarDatosEdicion()
        }
    }

    // ── Spinners ─────────────────────────────────────────────────────────────

    private fun configurarSpinners() {
        val marcas = listOf("-- Seleccione marca --") + ValidationHelper.MARCAS_VALIDAS
        binding.spinnerMarca.adapter = crearAdapter(marcas)

        val cilindradas = listOf("-- Seleccione cc --") + ValidationHelper.CILINDRADAS_VALIDAS.map { "$it cc" }
        binding.spinnerCilindraje.adapter = crearAdapter(cilindradas)

        val años = listOf("-- Seleccione año --") + (Calendar.getInstance().get(Calendar.YEAR) downTo 1990).toList()
        binding.spinnerModelo.adapter = crearAdapter(años.map { it.toString() })

        val colores = listOf("-- Seleccione color --") + ValidationHelper.COLORES_COMUNES
        binding.spinnerColor.adapter = crearAdapter(colores)

        val municipios = listOf("-- Seleccione municipio --") + ValidationHelper.MUNICIPIOS_VALIDOS
        binding.spinnerMunicipio.adapter = crearAdapter(municipios)
        binding.spinnerMunicipioVoz.adapter = crearAdapter(municipios)
    }

    private fun crearAdapter(lista: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, lista).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────

    private fun configurarTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { binding.panelManual.visibility = View.VISIBLE; binding.panelVoz.visibility = View.GONE }
                    1 -> { binding.panelManual.visibility = View.GONE; binding.panelVoz.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ── Botones ──────────────────────────────────────────────────────────────

    private fun configurarBotones() {
        // Manual - Guardar
        binding.btnGuardar.setOnClickListener { guardarDesdeManual() }

        // Voz - Micrófono
        binding.btnMicrofono.setOnClickListener { pedirPermisoYEscuchar() }

        // Voz - Guardar resultado
        binding.btnGuardarVoz.setOnClickListener { guardarDesdeVoz() }

        // Voz - Reintentar
        binding.btnReintentar.setOnClickListener {
            binding.cardResultadoVoz.visibility = View.GONE
            binding.tvMensajeVoz.visibility = View.GONE
            pedirPermisoYEscuchar()
        }
    }

    private fun guardarDesdeManual() {
        val marca = binding.spinnerMarca.selectedItem.toString()
        val ccStr = binding.spinnerCilindraje.selectedItem.toString().replace(" cc", "")
        val modelo = binding.spinnerModelo.selectedItem.toString()
        val color = binding.spinnerColor.selectedItem.toString()
        val municipio = binding.spinnerMunicipio.selectedItem.toString()

        if (marca.startsWith("--") || ccStr.startsWith("--") || modelo.startsWith("--") ||
            color.startsWith("--") || municipio.startsWith("--")) {
            Snackbar.make(binding.root, "Complete todos los campos obligatorios", Snackbar.LENGTH_LONG).show()
            return
        }

        viewModel.guardarMoto(marca, ccStr, modelo, color, municipio, editId)
    }

    private fun guardarDesdeVoz(forzar: Boolean = false) {
        val municipio = binding.spinnerMunicipioVoz.selectedItem.toString()
        if (municipio.startsWith("--")) {
            binding.tvMensajeVoz.text = "⚠️ Seleccione el municipio"
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        binding.tvMensajeVoz.visibility = View.GONE

        val marca = marcaVoz ?: run {
            binding.tvMensajeVoz.text = "⚠️ No se detectó la marca. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val cc = cilindrajeVoz ?: run {
            binding.tvMensajeVoz.text = "⚠️ No se detectó el cilindraje. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val modelo = modeloVoz ?: run {
            binding.tvMensajeVoz.text = "⚠️ No se detectó el año. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val color = colorVoz ?: run {
            binding.tvMensajeVoz.text = "⚠️ No se detectó el color. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }

        viewModel.guardarMoto(marca, cc.toString(), modelo.toString(), color, municipio, editId, forzar)
    }

    // ── Reconocimiento de voz ────────────────────────────────────────────────

    private fun pedirPermisoYEscuchar() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> iniciarEscucha()
            else -> requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun iniciarEscucha() {
        binding.tvEstadoVoz.text = "🎤 Escuchando…"
        binding.btnMicrofono.alpha = 0.6f
        binding.cardResultadoVoz.visibility = View.GONE
        binding.tvMensajeVoz.visibility = View.GONE

        voiceRecognizer.iniciar(object : VoiceRecognizer.VoiceListener {
            override fun onReady() {
                runOnUiThread { binding.tvEstadoVoz.text = "🎤 Habla ahora…" }
            }
            override fun onEndOfSpeech() {
                runOnUiThread { binding.tvEstadoVoz.text = "⏳ Procesando…" }
            }
            override fun onResult(texto: String) {
                runOnUiThread {
                    binding.btnMicrofono.alpha = 1f
                    binding.tvEstadoVoz.text = "✅ Listo. Revisa los datos."
                    procesarTextoVoz(texto)
                }
            }
            override fun onError(mensaje: String) {
                runOnUiThread {
                    binding.btnMicrofono.alpha = 1f
                    binding.tvEstadoVoz.text = "Presiona el micrófono"
                    Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun procesarTextoVoz(texto: String) {
        val datos = VoiceEntityExtractor.extraer(texto)

        marcaVoz = datos.marca
        cilindrajeVoz = datos.cilindraje
        modeloVoz = datos.modelo
        colorVoz = datos.color

        binding.tvTextoReconocido.text = "\"$texto\""

        val sb = StringBuilder()
        sb.appendLine("🏷️ Marca: ${datos.marca ?: "❌ No detectado"}")
        sb.appendLine("⚙️ Cilindraje: ${datos.cilindraje?.let { "$it cc" } ?: "❌ No detectado"}")
        sb.appendLine("📅 Año: ${datos.modelo ?: "❌ No detectado"}")
        sb.appendLine("🎨 Color: ${datos.color ?: "❌ No detectado"}")
        binding.tvDatosExtraidos.text = sb.toString().trim()

        binding.cardResultadoVoz.visibility = View.VISIBLE
    }

    // ── ViewModel observers ──────────────────────────────────────────────────

    private fun observarViewModel() {
        viewModel.guardadoExitoso.observe(this) { ok ->
            if (ok) {
                Snackbar.make(binding.root, "✅ Moto guardada correctamente", Snackbar.LENGTH_SHORT).show()
                viewModel.limpiarEstado()
                finish()
            }
        }

        viewModel.errorMensaje.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.limpiarEstado()
            }
        }

        viewModel.advertenciaDuplicado.observe(this) { moto ->
            moto?.let {
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Posible duplicado")
                    .setMessage("Ya existe una moto con estos datos.\n\n¿Guardar de todas formas?")
                    .setPositiveButton("Sí, guardar") { _, _ ->
                        // Determinar si venía de voz o manual
                        val desdeVoz = binding.panelVoz.visibility == View.VISIBLE
                        if (desdeVoz) guardarDesdeVoz(forzar = true)
                        else guardarManualForzado()
                    }
                    .setNegativeButton("Cancelar") { _, _ -> viewModel.limpiarEstado() }
                    .show()
            }
        }
    }

    private fun guardarManualForzado() {
        val marca = binding.spinnerMarca.selectedItem.toString()
        val ccStr = binding.spinnerCilindraje.selectedItem.toString().replace(" cc", "")
        val modelo = binding.spinnerModelo.selectedItem.toString()
        val color = binding.spinnerColor.selectedItem.toString()
        val municipio = binding.spinnerMunicipio.selectedItem.toString()
        viewModel.guardarMoto(marca, ccStr, modelo, color, municipio, editId, forzar = true)
    }

    // ── Edición ──────────────────────────────────────────────────────────────

    private fun cargarDatosEdicion() {
        viewModel.cargarParaEditar(editId) { moto ->
            moto ?: return@cargarParaEditar
            // Seleccionar valores en spinners
            seleccionarSpinner(binding.spinnerMarca, moto.marca, ValidationHelper.MARCAS_VALIDAS)
            seleccionarSpinner(binding.spinnerCilindraje, "${moto.cilindraje} cc",
                ValidationHelper.CILINDRADAS_VALIDAS.map { "$it cc" })
            seleccionarSpinner(binding.spinnerModelo, moto.modelo.toString(),
                (2026 downTo 1990).map { it.toString() })
            seleccionarSpinner(binding.spinnerColor, moto.color, ValidationHelper.COLORES_COMUNES)
            seleccionarSpinner(binding.spinnerMunicipio, moto.municipio, ValidationHelper.MUNICIPIOS_VALIDOS)
        }
    }

    private fun seleccionarSpinner(spinner: android.widget.Spinner, valor: String, lista: List<String>) {
        val pos = lista.indexOf(valor) + 1  // +1 por el "-- Seleccione --"
        if (pos > 0) spinner.setSelection(pos)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer.liberar()
    }
}
