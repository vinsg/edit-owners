package commands

import Config
import GithubService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import options.UsernameOption

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

    private val usernameOption by UsernameOption()

    private val config by requireObject<Config>()

    private val orgName: String? by option("-o", "--org", help = "Org to narrow repo list")

    override fun run() = runBlocking {
        val t = Terminal()
        val ghService = GithubService()

        val username: String = usernameOption.username ?: config.username
        t.println("Using username: ${TextStyles.bold(username)}")

        val repos: List<String> = ghService.searchRepo(username).getOrThrow().items.map {
            it.repository.full_name
        }
            .toList()
            .also {
                if (orgName != null) {
                    it.filter { it.contains(orgName!!) }
                }
            }

        // check if list is empty
        if (repos.isEmpty()) {
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