package com.example.mrd.data.remote

import android.content.Context
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

/**
 * In-app mock REST backend. Retrofit/OkHttp issue real requests; this interceptor answers them
 * from bundled assets after an artificial delay, so the whole stack above it (Retrofit,
 * serialization, error mapping, repository) is exercised exactly as it would be against a real
 * server:
 *
 *   GET /restaurants            -> assets/restaurants.json
 *   GET /restaurants/{id}/menu  -> the matching entry of assets/menus.json (404 if there is none)
 *
 * To point the app at a real service: change the base URL in `DataModule` and delete this
 * interceptor. Nothing else changes.
 */
class MockRestaurantInterceptor(
    private val context: Context,
    private val simulator: NetworkSimulator,
    private val latencyMs: Long = 700,
) : Interceptor {

    /** Parsed once, on first menu request (about 0.5 MB of JSON). */
    private val menus: JSONObject by lazy {
        JSONObject(readAsset(MENUS_ASSET)).getJSONObject("menus")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val segments = request.url.pathSegments
        val isMenu = segments.size == 3 && segments[0] == "restaurants" && segments[2] == "menu"
        Thread.sleep(if (isMenu) latencyMs / 2 else latencyMs)

        when (simulator.mode.value) {
            SimulatedNetwork.OFFLINE -> throw IOException("Simulated: no network connection")
            SimulatedNetwork.SERVER_ERROR ->
                return respond(request, 503, "Service Unavailable", """{"error":"service_unavailable"}""")
            SimulatedNetwork.ONLINE -> Unit
        }

        if (request.method != "GET") return respond(request, 405, "Method Not Allowed", """{"error":"method_not_allowed"}""")

        return when {
            segments == listOf("restaurants") ->
                respond(request, 200, "OK", readAsset(RESTAURANTS_ASSET))

            isMenu -> {
                val menu = menus.optJSONObject(segments[1])
                if (menu != null) respond(request, 200, "OK", menu.toString())
                else respond(request, 404, "Not Found", """{"error":"menu_not_found"}""")
            }

            else -> respond(request, 404, "Not Found", """{"error":"not_found"}""")
        }
    }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    private fun respond(request: Request, code: Int, message: String, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

    private companion object {
        const val RESTAURANTS_ASSET = "restaurants.json"
        const val MENUS_ASSET = "menus.json"
    }
}
