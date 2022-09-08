package model

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal

data class Repository(
    val repoName: String,
    val sha: String,
    val codeOwnersFile: CodeOwnersFile,
    var status: Status,
    var prURL: String,
)

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
fun Terminal.printRepos(repositoryList: List<Repository>) {
    this.println(
        table {
            header { row("repo name", "status", "Pull Request URL") }
            body {
                repositoryList.forEach { i ->
                    row(i.repoName, i.status, i.prURL)
                }
            }
        }
    )
}
