package commands

import GithubService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import model.CodeOwnersFile
import model.Repository
import model.Status
import model.printRepos
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Add the @username to all the repositories provided with the --repos or --file flags.
 */
class Add : CliktCommand(
    help = """
        Append @username to all repositories provided.
        
        Recommended use:
        ./edit-owners \
        -t <token> \
        -f <path-to-file>
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    /**
     * Get list of repositories in format org/name.
     */
    private val inputReposList by mutuallyExclusiveOptions(
        option(
            "-r",
            "--repos",
            help = "list of repositories comma separated ie. \"org/repo1,org/repo2,org/repo3\""
        ).convert { it.split(",") },
        option(
            "-f",
            "--file",
            help = "comma-separated values (csv) file with the name of the repositories ie. org/repo1,org/repo2"
        ).convert {
            FileSystem.SYSTEM.read(it.toPath()) { readUtf8() }.split(",")
        }
    )
        .single()
        .required()

    override fun run() = runBlocking {
        val t = Terminal()
        val ghService = GithubService()

        // get username and validate token
        val username: String = ghService.getUsername().getOrThrow()
        t.println("The username associated with this account is ${TextStyles.bold(username)}")

        val validRepos: List<Repository> = inputReposList.map { i ->
            ghService.getRepo(username, i).getOrElse {
                t.print(it.message)
                // dummy repos for printing
                Repository(
                    i, "", CodeOwnersFile("", ""),
                    if (it is ClientRequestException) Status.MISSING else Status.ERROR,
                    ""
                )
            }
        }.toList().also {
            t.printRepos(it)
        }.filter { it.status == Status.READY }

        val response = t.prompt(
            "You are about to create a pull request for every ${Status.READY} repositories listed above.\n" +
                    "do you wish to continue?", choices = listOf("y", "n")
        )
        if (response == "n") {
            t.println("Exiting")
            throw ProgramResult(0)
        }

        // create a PR for every READY repositories on the list.
        validRepos.forEach { repo ->
            t.println("${repo.repoName}: adding username")
            ghService.createBranch(username, repo).onFailure {
                if (it.message?.contains("Reference already exists") == true) {
                    t.println("${repo.repoName}: branch with name add-$username already exists")
                } else {
                    t.println(it.message)
                }
                repo.status = Status.ERROR
                return@forEach
            }
            ghService.createCommit(username, repo).onFailure {
                t.println(it.message)
                repo.status = Status.ERROR
                return@forEach
            }
            repo.prURL = ghService.openPR(username, repo.repoName).getOrElse {
                t.println(it.message)
                repo.status = Status.ERROR
                return@forEach
            }
            repo.status = Status.DONE
            t.println("${repo.repoName}: username successfully added")
        }

        t.printRepos(validRepos)

        echo("All done!")
    }
}