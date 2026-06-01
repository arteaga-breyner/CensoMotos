package com.putumayo.censomotos.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
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

    // Datos extraídos por voz
    private var marcaVoz: String? = null
    private var tipoVoz: String? = null
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
        configurarListenersOtro()
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
        // Marca
        val marcas = listOf("-- Seleccione marca --") + ValidationHelper.MARCAS_VALIDAS + listOf("Otro")
        binding.spinnerMarca.adapter = crearAdapter(marcas)

        // Tipo (empieza vacío, se llena según marca)
        actualizarTipos("-- Seleccione marca --")

        // Cilindraje
        val cilindradas = listOf("-- Seleccione cc --") +
                ValidationHelper.CILINDRADAS_VALIDAS.map { "$it cc" } + listOf("Otro")
        binding.spinnerCilindraje.adapter = crearAdapter(cilindradas)

        // Modelo
        val años = listOf("-- Seleccione año --") + (Calendar.getInstance().get(Calendar.YEAR) downTo 1990).map { it.toString() }
        binding.spinnerModelo.adapter = crearAdapter(años)

        // Color
        val colores = listOf("-- Seleccione color --") + ValidationHelper.COLORES_COMUNES + listOf("Otro")
        binding.spinnerColor.adapter = crearAdapter(colores)

        // Municipio
        val municipios = listOf("-- Seleccione municipio --") + ValidationHelper.MUNICIPIOS_VALIDOS + listOf("Otro")
        binding.spinnerMunicipio.adapter = crearAdapter(municipios)
        binding.spinnerMunicipioVoz.adapter = crearAdapter(municipios)
    }

    private fun actualizarTipos(marca: String) {
        val tipos = if (marca.startsWith("--") || marca == "Otro") {
            listOf("-- Seleccione tipo --", "Otro")
        } else {
            listOf("-- Seleccione tipo --") + ValidationHelper.getTiposPorMarca(marca)
        }
        binding.spinnerTipo.adapter = crearAdapter(tipos)
        binding.etTipoOtro.visibility = View.GONE
    }

    private fun configurarListenersOtro() {
        // Marca -> actualiza tipos y muestra campo manual si es "Otro"
        binding.spinnerMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val seleccion = parent.getItemAtPosition(pos).toString()
                actualizarTipos(seleccion)
                toggleCampoOtro(seleccion, binding.etMarcaOtro)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Tipo
        binding.spinnerTipo.onItemSelectedListener = spinnerOtroListener(binding.etTipoOtro)

        // Cilindraje
        binding.spinnerCilindraje.onItemSelectedListener = spinnerOtroListener(binding.etCilindrajeOtro)

        // Color
        binding.spinnerColor.onItemSelectedListener = spinnerOtroListener(binding.etColorOtro)

        // Municipio manual
        binding.spinnerMunicipio.onItemSelectedListener = spinnerOtroListener(binding.etMunicipioOtro)

        // Municipio voz
        binding.spinnerMunicipioVoz.onItemSelectedListener = spinnerOtroListener(binding.etMunicipioVozOtro)
    }

    private fun spinnerOtroListener(campoTexto: EditText) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            toggleCampoOtro(parent.getItemAtPosition(pos).toString(), campoTexto)
        }
        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    private fun toggleCampoOtro(seleccion: String, campoTexto: EditText) {
        if (seleccion == "Otro") {
            campoTexto.visibility = View.VISIBLE
            campoTexto.requestFocus()
        } else {
            campoTexto.visibility = View.GONE
            campoTexto.text.clear()
        }
    }

    // Obtiene el valor real de un spinner (texto manual si eligió "Otro")
    private fun valorSpinner(spinner: Spinner, campoOtro: EditText): String {
        val seleccion = spinner.selectedItem?.toString() ?: ""
        return if (seleccion == "Otro") campoOtro.text.toString().trim() else seleccion
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
        binding.btnGuardar.setOnClickListener { guardarDesdeManual() }
        binding.btnMicrofono.setOnClickListener { pedirPermisoYEscuchar() }
        binding.btnGuardarVoz.setOnClickListener { guardarDesdeVoz() }
        binding.btnReintentar.setOnClickListener {
            binding.cardResultadoVoz.visibility = View.GONE
            binding.tvMensajeVoz.visibility = View.GONE
            pedirPermisoYEscuchar()
        }
    }

    private fun guardarDesdeManual(forzar: Boolean = false) {
        val marca = valorSpinner(binding.spinnerMarca, binding.etMarcaOtro)
        val tipo = valorSpinner(binding.spinnerTipo, binding.etTipoOtro)
        val ccStr = valorSpinner(binding.spinnerCilindraje, binding.etCilindrajeOtro).replace(" cc", "")
        val modelo = binding.spinnerModelo.selectedItem?.toString() ?: ""
        val color = valorSpinner(binding.spinnerColor, binding.etColorOtro)
        val municipio = valorSpinner(binding.spinnerMunicipio, binding.etMunicipioOtro)

        if (marca.isBlank() || marca.startsWith("--") ||
            tipo.isBlank() || tipo.startsWith("--") ||
            ccStr.isBlank() || ccStr.startsWith("--") ||
            modelo.startsWith("--") ||
            color.isBlank() || color.startsWith("--") ||
            municipio.isBlank() || municipio.startsWith("--")) {
            Snackbar.make(binding.root, "Complete todos los campos obligatorios", Snackbar.LENGTH_LONG).show()
            return
        }

        viewModel.guardarMoto(marca, tipo, ccStr, modelo, color, municipio, editId, forzar)
    }

    private fun guardarDesdeVoz(forzar: Boolean = false) {
        val municipio = valorSpinner(binding.spinnerMunicipioVoz, binding.etMunicipioVozOtro)
        if (municipio.isBlank() || municipio.startsWith("--")) {
            binding.tvMensajeVoz.text = "Seleccione el municipio"
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        binding.tvMensajeVoz.visibility = View.GONE

        val marca = marcaVoz ?: run {
            binding.tvMensajeVoz.text = "No se detecto la marca. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val cc = cilindrajeVoz ?: run {
            binding.tvMensajeVoz.text = "No se detecto el cilindraje. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val modelo = modeloVoz ?: run {
            binding.tvMensajeVoz.text = "No se detecto el año. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val color = colorVoz ?: run {
            binding.tvMensajeVoz.text = "No se detecto el color. Use ingreso Manual."
            binding.tvMensajeVoz.visibility = View.VISIBLE
            return
        }
        val tipo = tipoVoz ?: ""

        viewModel.guardarMoto(marca, tipo, cc.toString(), modelo.toString(), color, municipio, editId, forzar)
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
        binding.tvEstadoVoz.text = "Escuchando..."
        binding.btnMicrofono.alpha = 0.6f
        binding.cardResultadoVoz.visibility = View.GONE
        binding.tvMensajeVoz.visibility = View.GONE

        voiceRecognizer.iniciar(object : VoiceRecognizer.VoiceListener {
            override fun onReady() { runOnUiThread { binding.tvEstadoVoz.text = "Habla ahora..." } }
            override fun onEndOfSpeech() { runOnUiThread { binding.tvEstadoVoz.text = "Procesando..." } }
            override fun onResult(texto: String) {
                runOnUiThread {
                    binding.btnMicrofono.alpha = 1f
                    binding.tvEstadoVoz.text = "Listo. Revisa los datos."
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
        tipoVoz = datos.tipo
        cilindrajeVoz = datos.cilindraje
        modeloVoz = datos.modelo
        colorVoz = datos.color

        binding.tvTextoReconocido.text = "\"$texto\""

        val sb = StringBuilder()
        sb.appendLine("Marca: ${datos.marca ?: "No detectado"}")
        sb.appendLine("Tipo: ${datos.tipo ?: "No detectado"}")
        sb.appendLine("Cilindraje: ${datos.cilindraje?.let { "$it cc" } ?: "No detectado"}")
        sb.appendLine("Año: ${datos.modelo ?: "No detectado"}")
        sb.appendLine("Color: ${datos.color ?: "No detectado"}")
        binding.tvDatosExtraidos.text = sb.toString().trim()

        binding.cardResultadoVoz.visibility = View.VISIBLE
    }

    // ── ViewModel observers ──────────────────────────────────────────────────

    private fun observarViewModel() {
        viewModel.guardadoExitoso.observe(this) { ok ->
            if (ok) {
                Snackbar.make(binding.root, "Moto guardada correctamente", Snackbar.LENGTH_SHORT).show()
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
                    .setTitle("Posible duplicado")
                    .setMessage("Ya existe una moto con estos datos.\n\n¿Guardar de todas formas?")
                    .setPositiveButton("Si, guardar") { _, _ ->
                        val desdeVoz = binding.panelVoz.visibility == View.VISIBLE
                        if (desdeVoz) guardarDesdeVoz(forzar = true)
                        else guardarDesdeManual(forzar = true)
                    }
                    .setNegativeButton("Cancelar") { _, _ -> viewModel.limpiarEstado() }
                    .show()
            }
        }
    }

    // ── Edición ──────────────────────────────────────────────────────────────

    private fun cargarDatosEdicion() {
        viewModel.cargarParaEditar(editId) { moto ->
            moto ?: return@cargarParaEditar
            seleccionarSpinner(binding.spinnerMarca, moto.marca, ValidationHelper.MARCAS_VALIDAS + listOf("Otro"))
            actualizarTipos(moto.marca)
            seleccionarSpinner(binding.spinnerTipo, moto.tipo, ValidationHelper.getTiposPorMarca(moto.marca))
            seleccionarSpinner(binding.spinnerCilindraje, "${moto.cilindraje} cc",
                ValidationHelper.CILINDRADAS_VALIDAS.map { "$it cc" } + listOf("Otro"))
            seleccionarSpinner(binding.spinnerModelo, moto.modelo.toString(),
                (2026 downTo 1990).map { it.toString() })
            seleccionarSpinner(binding.spinnerColor, moto.color, ValidationHelper.COLORES_COMUNES + listOf("Otro"))
            seleccionarSpinner(binding.spinnerMunicipio, moto.municipio, ValidationHelper.MUNICIPIOS_VALIDOS + listOf("Otro"))
        }
    }

    private fun seleccionarSpinner(spinner: Spinner, valor: String, lista: List<String>) {
        val pos = lista.indexOf(valor) + 1
        if (pos > 0) spinner.setSelection(pos)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer.liberar()
    }
}
