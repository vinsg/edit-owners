package options

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option


class UsernameOption : OptionGroup("Username") {
    val username by option("-u", "--user", help = "Username for user to add.").convert {
        if (it.first().equals('@')) it.takeLast(it.length - 1) else it
    }
}