package options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Get list of repositories in format org/name.
 */
class RepoInputOption : OptionGroup("Input Repository List") {
    val input = mutuallyExclusiveOptions(
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
        })
        .single()
        .required()
}