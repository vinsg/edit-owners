package commands

import Config
import GithubService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import model.CodeOwnersFile
import model.GithubRepo
import model.Status
import model.printRepos
import options.RepoInputOption
import options.UsernameOption

/**
 * Add the @username to all the repositories provided with the --repos or --file flags.
 */
class Add : CliktCommand(
    help = """
        Append @username to all repositories provided.
        Adds the authenticated user by default, use the -u flag to specify a user.
        
        Recommended use:
        ./edit-owners \
        -t <token> \
        -f <path-to-file>
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val inputReposList by RepoInputOption().input

    private val usernameOption by UsernameOption()

    private val config by requireObject<Config>()

    override fun run() = runBlocking {
        val t = Terminal()
        val ghService = GithubService()

        val username: String = usernameOption.username ?: config.username

        t.println("Using username: ${TextStyles.bold(username)}")

        val repos: List<GithubRepo> = inputReposList.map { i ->
            ghService.getRepo(username, i).fold(
                onSuccess = {
                    if (it.codeOwnersFile.content.contains(username)) {
                        it.status = Status.DONE
                    }
                    it
                },
                onFailure = {
                    GithubRepo(
                        i, "", CodeOwnersFile("", ""),
                        if (it is ClientRequestException) Status.MISSING else Status.ERROR,
                        ""
                    )
                }
            )
        }.toList()

        t.printRepos(repos)

        val validRepos = repos.filter { it.status == Status.READY }

        confirm(
            "You are about to create a pull request for every ${Status.READY} repositories listed above.\n" +
                    "do you wish to continue?", abort = true
        )

        // create a PR for every READY repositories on the list.
        validRepos.forEach { repo ->
            t.println("${repo.repoName}: adding username")
            ghService.createBranch(username, repo, "add").onFailure {
                if (it.message?.contains("Reference already exists") == true) {
                    t.println("${repo.repoName}: branch with name add-$username already exists")
                } else {
                    t.println(it.message)
                }
                repo.status = Status.ERROR
                return@forEach
            }
            val newContent = "${repo.codeOwnersFile.content} @$username"
            ghService.createCommit(username, repo, newContent).onFailure {
                t.println(it.message)
                repo.status = Status.ERROR
                return@forEach
            }
            val title = "Add $username to the CODEOWNERS file"
            repo.prURL = ghService.openPR(title, action = "add", username, repo.repoName).getOrElse {
                t.println(it.message)
                repo.status = Status.ERROR
                return@forEach
            }
            repo.status = Status.DONE
            t.println("${repo.repoName}: Pull request successfully created")
        }

        t.printRepos(validRepos)

        echo("All done!")
    }
}