package com.carlosalbertoxw.ejemplos_jwt_android.products

import com.squareup.moshi.JsonClass

/** Producto. En el catálogo público el servidor omite el precio (price == null). */
@JsonClass(generateAdapter = true)
data class Product(
    val id: Int,
    val name: String?,
    val description: String?,
    val price: Double? = null,
)

/** Cuerpo de POST /api/products (requiere rol Admin) */
@JsonClass(generateAdapter = true)
data class NewProduct(
    val name: String,
    val description: String?,
    val price: Double,
)
