import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.versionOption
import commands.Add
import commands.CreateFile
import commands.Remove
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Entry point of the application. Subcommands are added to using the [subcommands] function.
 */
fun main(args: Array<String>) = EditOwners().subcommands(Add(), CreateFile(), Remove()).main(args)

data class Config(
    var username: String
)

class EditOwners : CliktCommand(
    help = """Edit-owners is a command line tool to help manage the CODEOWNERS file
        of many GitHub repositories.

        This tool is a proof of concept for Kotlin Multiplatform and 
        its source code is heavily annotated. Please leave feedback or report any bugs
        by opening a issue on GitHub.
        
        Made by @vinsg
        """,
    printHelpOnEmptyArgs = true
) {

    init {
        versionOption("0.0.5")
    }

    /**
     * Interacting with the GitHub API requires the use of a Personal Access Token.
     */
    private val token by option("-t", "--token", help = "Personal Access Token")
        .required()
        .check(
            """must provide a Personal Access Token, 
            |see guide: https://github.com/vinsg/edit-owners/blob/main/token-guide.md""".trimMargin()
        ) {
            it.isNotBlank()
        }

    private val config by findOrSetObject { Config("") }

    override fun run() = runBlocking {
        initKoin(token)


        // get authenticated user and validate auth
        val result = GithubService().getUsername().onSuccess {
            config.username = it
        }.onFailure {
            throw CliktError("Cannot retrieve user", cause = Exception(it.cause))
        }
    }
}

// todo -v verbose flag

/**
 * Implementation of the dependency injection pattern using Koin. We declare a single [httpClient] that
 * is used throughout the application.
 * @param token GitHub Personal Access Token
 */
private fun initKoin(token: String) = run {
    startKoin {
        modules(
            module {
                single {
                    httpClient {
                        expectSuccess = true
                        defaultRequest {
                            header(HttpHeaders.Authorization, "token $token")
                        }
                        install(ContentNegotiation) {
                            json(Json {
                                prettyPrint = true
                                isLenient = true
                                ignoreUnknownKeys = true
                            })
                        }
                        defaultRequest {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                            url("https://api.github.com/")
                        }
                    }
                }
            }
        )
    }
}


