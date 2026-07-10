package com.carlosalbertoxw.ejemplos_jwt_android.products

/** Producto. En el catálogo público el servidor omite el precio (price == null). */
data class Product(
    val id: Int,
    val name: String?,
    val description: String?,
    val price: Double? = null,
)

/** Cuerpo de POST /api/products (requiere rol Admin) */
data class NewProduct(
    val name: String,
    val description: String?,
    val price: Double,
)
