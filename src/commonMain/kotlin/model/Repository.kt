package model

import GithubService
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

data class Repository(
    val org: String,
    val name: String,
    val sha: String,
    val codeOwnersFile: GithubService.CodeownerFile,
    var status: Status,
    var prURL: String?,
)

fun Repository.getRepoName() = "$org/$name"

enum class Status {
    READY {
        override fun toString(): String {
            return TextColors.blue(super.toString())
        }
    },
    MISSING {
        override fun toString(): String {
            return TextColors.yellow(super.toString())
        }
    },
    ERROR {
        override fun toString(): String {
            return TextColors.red(super.toString())
        }
    },
    DONE {
        override fun toString(): String {
            return TextColors.green(super.toString())
        }
    }
}

/**
 * Print [repositoryList] with pretty colors and easy to read columns.
 */
fun Terminal.printAll(repositoryList: List<Repository>) {
    this.println(
        table {
            header { row("org", "repo name", "status", "Pull Request URL") }
            body {
                repositoryList.forEach { i ->
                    row(i.org, i.name, i.status, i.prURL)
                }
            }
        }
    )
}

/**
 * Pretty print a single [repository].
 */
fun Terminal.printRepo(repository: Repository) {
    this.println(
        table { body { row(repository.name, repository.status) } }
    )
}
