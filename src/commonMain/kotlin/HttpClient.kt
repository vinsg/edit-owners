import io.ktor.client.*

/*
Creating an HttpClient with a platform specific implementation
 */
expect fun httpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient