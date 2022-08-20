package commands

import GithubService
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import model.Status
import model.getRepoName
import model.printAll
import model.printRepo
import okio.FileSystem
import okio.Path.Companion.toPath

/*

 */
class Add : CliktCommand(
    help = """
        todo 
        
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

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
        val username = ghService.getUsername()
        t.println("The username associated with this account is ${TextStyles.bold(username)}")

        val repos = inputReposList.map {
            ghService.getRepo(username, it)
        }.toList()

        t.printAll(repos)

        val response = t.prompt(
            "You are about to create a pull request for every ${Status.READY} repositories listed above.\n" +
                    "do you wish to continue?", choices = listOf("y", "n")
        )
        if (response == "n") {
            throw CliktError("Exiting")
        }

        repos.filter { it.status == Status.READY }.forEach {
            ghService.createBranch(username, it)
            ghService.createCommit(username, it)
            it.prURL = ghService.openPR(username, it.getRepoName())
            it.status = Status.DONE
            t.printRepo(it)
        }

        t.printAll(repos)

        echo("All done!")
    }
}