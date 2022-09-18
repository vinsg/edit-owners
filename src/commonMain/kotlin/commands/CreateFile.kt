package commands

import GithubService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Create a csv file with all the repositories associated with a user.
 */
class CreateFile : CliktCommand(
    help = """
        Create a csv file with all the repositories associated with a user.
        Adds the authenticated user by default, use the -u flag to specify a user.
        Use the -o flag to narrow the list of repositories to a specific org.
        
        Recommended use:
        ./edit-owners \
        -t <token> \
        create-file -o MyOrg
    """.trimIndent(),
) {

    private val externalUsername: String? by option("-u", "--user", help = "Username for user to add.").convert {
        if (it.first().equals('@')) it.takeLast(it.length - 1) else it
    }

    private val orgName: String? by option("-o", "--org", help = "Org to narrow repo list")

    override fun run() = runBlocking {
        val t = Terminal()
        val ghService = GithubService()

        // get username and validate token
        // todo Use contract here ?
        val mainUsername: String = ghService.getUsername().getOrThrow()
        val username = if (!externalUsername.isNullOrBlank()) {
            t.println("Using username provided: ${TextStyles.bold(externalUsername!!)}")
            externalUsername!!
        } else {
            t.println("No username provided, using the authenticated username: ${TextStyles.bold(mainUsername)}")
            mainUsername
        }

        val repos: List<String> = ghService.searchRepo(username).getOrThrow().items.map {
            it.repository.full_name
        }
        val filtered = if (orgName != null) {
            t.println("Narrowing the list to org: ${TextStyles.bold(orgName!!)}")
            repos.filter { it.contains(orgName!!) }
        } else repos

        // check if list is empty
        if (filtered.isEmpty()) {
            t.println("No repositories with this org/name.")
            throw ProgramResult(0)
        }

        val path = "repos.csv".toPath()

        val result = runCatching {
            FileSystem.SYSTEM.write(path, false) {
                writeUtf8(repos.joinToString(","))
            }
        }.onSuccess {
            t.println("${path.name} created.")
        }.onFailure {
            t.println(it.message)
        }
    }
}