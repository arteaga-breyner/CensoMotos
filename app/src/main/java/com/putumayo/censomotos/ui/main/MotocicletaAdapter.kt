package com.putumayo.censomotos.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.putumayo.censomotos.data.entity.Motocicleta
import com.putumayo.censomotos.databinding.ItemMotocicletaBinding

class MotocicletaAdapter(
    private val onEditar: (Motocicleta) -> Unit,
    private val onEliminar: (Motocicleta) -> Unit
) : ListAdapter<Motocicleta, MotocicletaAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMotocicletaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMotocicletaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(moto: Motocicleta) {
            binding.tvDescripcion.text = moto.getDescripcionCorta()
            binding.tvMunicipio.text = "📍 ${moto.municipio}"
            binding.tvFecha.text = moto.getFechaFormateada()
            binding.btnEditar.setOnClickListener { onEditar(moto) }
            binding.btnEliminar.setOnClickListener { onEliminar(moto) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Motocicleta>() {
        override fun areItemsTheSame(old: Motocicleta, new: Motocicleta) = old.id == new.id
        override fun areContentsTheSame(old: Motocicleta, new: Motocicleta) = old == new
    }
}
