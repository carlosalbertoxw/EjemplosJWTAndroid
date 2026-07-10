package com.carlosalbertoxw.ejemplos_jwt_android.products

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Recurso de productos. Este paquete no sabe nada de JWT: va por el cliente
 * autenticado, y es el paquete auth (interceptor + authenticator) quien
 * adjunta y renueva los tokens de forma transparente.
 */
interface ProductsApi {

    /** Catálogo público: mismos productos pero sin el precio. No requiere token. */
    @GET("api/products/public")
    suspend fun publicProducts(): List<Product>

    /** Catálogo completo con precio. Requiere access token válido (cualquier rol). */
    @GET("api/products")
    suspend fun products(): List<Product>

    /** Crea un producto. Requiere que el claim role del token sea Admin; si no, 403. */
    @POST("api/products")
    suspend fun createProduct(@Body body: NewProduct): Product
}
