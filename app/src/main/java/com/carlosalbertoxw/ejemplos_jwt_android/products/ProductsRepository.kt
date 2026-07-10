package com.carlosalbertoxw.ejemplos_jwt_android.products

/**
 * Fachada del recurso de productos: lo único de este paquete que la UI
 * necesita conocer. Hoy solo delega en la API; si mañana hubiera caché o
 * base de datos local, viviría aquí sin tocar las pantallas.
 */
class ProductsRepository(private val api: ProductsApi) {

    suspend fun publicCatalog(): List<Product> = api.publicProducts()

    suspend fun fullCatalog(): List<Product> = api.products()

    suspend fun create(product: NewProduct): Product = api.createProduct(product)
}
