package com.putumayo.censomotos.ui.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.putumayo.censomotos.data.entity.Motocicleta
import com.putumayo.censomotos.databinding.ActivityMainBinding
import com.putumayo.censomotos.ui.add.AddMotocicleActivity
import com.putumayo.censomotos.ui.summary.SummaryActivity
import com.putumayo.censomotos.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: MotocicletaAdapter

    // Colores por municipio para los chips
    private val coloresMunicipio = mapOf(
        "Mocoa" to "#1565C0",
        "Puerto Asís" to "#2E7D32",
        "Puerto Caicedo" to "#6A1B9A",
        "Villagarzón" to "#E65100"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        configurarRecyclerView()
        configurarBotones()
        observarViewModel()
    }

    private fun configurarRecyclerView() {
        adapter = MotocicletaAdapter(
            onEditar = { moto -> abrirFormulario(moto.id) },
            onEliminar = { moto -> confirmarEliminar(moto) }
        )
        binding.rvMotos.layoutManager = LinearLayoutManager(this)
        binding.rvMotos.adapter = adapter
    }

    private fun configurarBotones() {
        binding.btnNuevaMoto.setOnClickListener {
            abrirFormulario()
        }
        binding.btnVerResumen.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
    }

    private fun observarViewModel() {
        viewModel.todasLasMotos.observe(this) { motos ->
            adapter.submitList(motos)
            binding.tvVacio.visibility = if (motos.isEmpty()) View.VISIBLE else View.GONE
            binding.rvMotos.visibility = if (motos.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.totalMotos.observe(this) { total ->
            binding.tvTotal.text = total.toString()
        }

        viewModel.resumenMunicipios.observe(this) { resumen ->
            actualizarChipsMunicipio(resumen)
        }

        viewModel.mensajeUI.observe(this) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }
    }

    private fun actualizarChipsMunicipio(resumen: Map<String, Int>) {
        binding.llMunicipios.removeAllViews()
        coloresMunicipio.forEach { (municipio, colorHex) ->
            val total = resumen[municipio] ?: 0
            val chip = Chip(this).apply {
                text = "$municipio\n$total"
                setChipBackgroundColorResource(android.R.color.white)
                chipStrokeWidth = 2f
                setChipStrokeColorResource(android.R.color.transparent)
                setTextColor(Color.parseColor(colorHex))
                textSize = 13f
                isClickable = false
            }
            // Peso uniforme
            val params = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            params.marginEnd = 4
            chip.layoutParams = params
            binding.llMunicipios.addView(chip)
        }
    }

    private fun abrirFormulario(editId: Long = 0) {
        val intent = Intent(this, AddMotocicleActivity::class.java)
        if (editId > 0) intent.putExtra(AddMotocicleActivity.EXTRA_EDIT_ID, editId)
        startActivity(intent)
    }

    private fun confirmarEliminar(moto: Motocicleta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Eliminar ${moto.getDescripcionCorta()}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarMoto(moto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarResumenMunicipios()
    }
}
