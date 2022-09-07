import io.ktor.client.*

/**
 * Creating an [httpClient] with a platform specific implementation.
 * @param config Platform specific [HttpClientConfig]
 */
expect fun httpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient