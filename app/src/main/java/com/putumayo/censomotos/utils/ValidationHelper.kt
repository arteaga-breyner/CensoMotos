package com.putumayo.censomotos.utils

/**
 * Validaciones de negocio para los campos de la motocicleta.
 */
object ValidationHelper {

    val MARCAS_VALIDAS = listOf(
        "Yamaha", "Honda", "Suzuki", "Bajaj", "Kawasaki",
        "AKT", "TVS", "Auteco", "Royal Enfield", "KTM",
        "Pulsar", "Hero", "Bera", "UM", "Otro"
    )

    val CILINDRADAS_VALIDAS = listOf(50, 80, 100, 110, 125, 150, 175, 200, 250, 300, 400, 500)

    val MUNICIPIOS_VALIDOS = listOf(
        "Mocoa",
        "Puerto Asís",
        "Puerto Caicedo",
        "Villagarzón"
    )

    val COLORES_COMUNES = listOf(
        "Roja", "Azul", "Negra", "Blanca", "Gris",
        "Verde", "Amarilla", "Naranja", "Plateada", "Café",
        "Morada", "Rosa", "Turquesa", "Otro"
    )

    val AÑO_MINIMO = 1990
    val AÑO_MAXIMO = 2026
    val CC_MINIMO = 50
    val CC_MAXIMO = 500

    data class ResultadoValidacion(
        val esValido: Boolean,
        val mensaje: String = ""
    )

    fun validarMarca(marca: String): ResultadoValidacion {
        return if (marca.isBlank()) {
            ResultadoValidacion(false, "La marca no puede estar vacía")
        } else {
            ResultadoValidacion(true)
        }
    }

    fun validarCilindraje(cilindrajeStr: String): ResultadoValidacion {
        val cc = cilindrajeStr.toIntOrNull()
            ?: return ResultadoValidacion(false, "El cilindraje debe ser un número")
        return if (cc < CC_MINIMO || cc > CC_MAXIMO) {
            ResultadoValidacion(false, "El cilindraje debe estar entre $CC_MINIMO y $CC_MAXIMO cc")
        } else {
            ResultadoValidacion(true)
        }
    }

    fun validarModelo(modeloStr: String): ResultadoValidacion {
        val año = modeloStr.toIntOrNull()
            ?: return ResultadoValidacion(false, "El modelo debe ser un año numérico")
        return if (año < AÑO_MINIMO || año > AÑO_MAXIMO) {
            ResultadoValidacion(false, "El año debe estar entre $AÑO_MINIMO y $AÑO_MAXIMO")
        } else {
            ResultadoValidacion(true)
        }
    }

    fun validarColor(color: String): ResultadoValidacion {
        return if (color.isBlank()) {
            ResultadoValidacion(false, "El color no puede estar vacío")
        } else {
            ResultadoValidacion(true)
        }
    }

    fun validarMunicipio(municipio: String): ResultadoValidacion {
        return if (municipio !in MUNICIPIOS_VALIDOS) {
            ResultadoValidacion(false, "Seleccione un municipio válido: ${MUNICIPIOS_VALIDOS.joinToString(", ")}")
        } else {
            ResultadoValidacion(true)
        }
    }

    /**
     * Valida todos los campos y retorna lista de errores.
     * Lista vacía = sin errores.
     */
    fun validarTodo(
        marca: String,
        cilindrajeStr: String,
        modeloStr: String,
        color: String,
        municipio: String
    ): List<String> {
        val errores = mutableListOf<String>()
        validarMarca(marca).takeIf { !it.esValido }?.let { errores.add(it.mensaje) }
        validarCilindraje(cilindrajeStr).takeIf { !it.esValido }?.let { errores.add(it.mensaje) }
        validarModelo(modeloStr).takeIf { !it.esValido }?.let { errores.add(it.mensaje) }
        validarColor(color).takeIf { !it.esValido }?.let { errores.add(it.mensaje) }
        validarMunicipio(municipio).takeIf { !it.esValido }?.let { errores.add(it.mensaje) }
        return errores
    }
}
