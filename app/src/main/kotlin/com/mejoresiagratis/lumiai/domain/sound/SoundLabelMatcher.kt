package com.mejoresiagratis.lumiai.domain.sound

/**
 * Resuelve a que [SoundCategory] pertenece una etiqueta de clase del clasificador (nombres
 * AudioSet/YAMNet). Construye un indice inverso a partir de [SoundCategory.labels].
 *
 * Las categorias de v1 no comparten etiquetas; si en el futuro lo hicieran, gana la ultima
 * categoria declarada (y conviene revisarlo: ver test de no solapamiento).
 */
class SoundLabelMatcher(categories: List<SoundCategory> = SoundCategory.entries) {

    private val index: Map<String, SoundCategory> = buildMap {
        categories.forEach { category -> category.labels.forEach { label -> put(label, category) } }
    }

    /** Categoria asociada a la etiqueta, o null si no se vigila. */
    fun categoryFor(label: String): SoundCategory? = index[label]

    /** Conjunto de etiquetas conocidas (union de todas las categorias dadas). */
    val knownLabels: Set<String> get() = index.keys
}
