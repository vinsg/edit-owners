import com.github.ajalt.clikt.core.CliktError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import model.Repository
import model.Status
import model.getRepoName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Contains main GitHub API functionalities.
 */
class GithubService : KoinComponent {

    // DI injecting httpClient
    private val ghClient: HttpClient by inject()

    @Serializable
    data class Repo(
        @SerialName("object")
        val objectSha: ObjectSha
    )

    @Serializable
    data class ObjectSha(val sha: String)

    /**
     * Serves as auth validation and gets the GitHub username associated with the token.
     */
    suspend fun getUsername(): String {
        @Serializable
        data class User(val url: String)

        // get user
        val response: User = try {
            ghClient.get("user").body()
        } catch (e: ClientRequestException) {
            // todo write better error msg
            throw CliktError("Fatal: Failed to authenticate with provided Token.", cause = e)
        }

        return response.url.substringAfterLast("/")
    }

    sealed class CodeOwnersResult {
        data class Success(val codeOwnersFile: CodeownerFile) : CodeOwnersResult()
        data class FileMissing(val message: String, val cause: Exception? = null) : CodeOwnersResult()
        data class Error(val message: String?, val cause: Exception? = null) : CodeOwnersResult()
    }

    /**
     * Get a repository object using username.
     * @param username
     * @param repoName in the <org>/<name> format
     */
    suspend fun getRepo(username: String, repoName: String): Repository =
        when (val result = getCodeOwnersFile(repoName)) {
            is CodeOwnersResult.Success -> {
                Repository(
                    org = repoName.substringBefore("/"),
                    name = repoName.substringAfter("/"),
                    sha = getShaRepo(repoName),
                    codeOwnersFile = result.codeOwnersFile,
                    status = if (result.codeOwnersFile.content.contains(username)) {
                        Status.DONE
                    } else {
                        Status.READY
                    },
                    prURL = "---"
                )
            }

            is CodeOwnersResult.FileMissing -> {
                Repository(
                    org = repoName.substringBefore("/"),
                    name = repoName.substringAfter("/"),
                    sha = "---",
                    codeOwnersFile = CodeownerFile("----", "---"),
                    status = Status.MISSING,
                    prURL = "---"
                )
            }

            is CodeOwnersResult.Error -> {
                Repository(
                    org = repoName.substringBefore("/"),
                    name = repoName.substringAfter("/"),
                    sha = "---",
                    codeOwnersFile = CodeownerFile("---", "---"),
                    status = Status.ERROR,
                    prURL = "---"
                )
            }
        }

    /**
     * Decode Base64 file content.
     */
    object Base64ContentSerializer : KSerializer<String> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Content", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) =
            decoder.decodeString().trim().decodeBase64String().trim()

        override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value.encodeBase64())
    }

    @Serializable
    data class CodeownerFile(
        @Serializable(with = Base64ContentSerializer::class)
        val content: String,
        val sha: String
    )

    /**
     * Retrieves CODEOWNERS file from repo.
     * @param repoName in the <org>/<name> format
     */
    private suspend fun getCodeOwnersFile(repoName: String): CodeOwnersResult {
        val codeOwnersPath = "contents/.github/CODEOWNERS"

        val response: CodeownerFile = try {
            ghClient.get("repos") {
                url { appendEncodedPathSegments(repoName, codeOwnersPath) }
            }.body()
        } catch (e: ClientRequestException) {
            return CodeOwnersResult.FileMissing(e.message, cause = e)
        } catch (e: Exception) {
            return CodeOwnersResult.Error(e.message, cause = e)
        }
        return CodeOwnersResult.Success(response)
    }

    /**
     * Get the unique Sha reference for the repo, used for creating a branch.
     * @param repoName in the <org>/<name> format
     */
    private suspend fun getShaRepo(repoName: String): String {
        val refPath = "git/ref/heads/main"
        val response: Repo = ghClient.get("repos") {
            url { appendEncodedPathSegments(repoName, refPath) }
        }.body() // todo proper exception handling
//        println("The sha for the repo is ${response.objectSha.sha}")
        return response.objectSha.sha
    }

    /**
     * Create a branch with the name "add-[username]" on the remote.
     */
    suspend fun createBranch(username: String, repo: Repository) {
        @Serializable
        data class RequestBody(val ref: String, val sha: String)

        val sha = repo.sha
        val gitRefPath = "git/refs"
        val branchName = "refs/heads/add-$username"
        val response = ghClient.post("repos") {
            url { appendEncodedPathSegments(repo.getRepoName(), gitRefPath) }
            contentType(ContentType.Application.Json)
            setBody(RequestBody(branchName, sha))
        }
    }

    /**
     * Create commit and push commit with "@[username]" string appended to CODEOWNERS file.
     */
    suspend fun createCommit(username: String, repo: Repository) {
        @Serializable
        data class Commit(
            val message: String,
            @Serializable(with = Base64ContentSerializer::class)
            val content: String,
            val sha: String,
            val branch: String
        )
        val (content, sha) = repo.codeOwnersFile
        val newContent = "$content @$username"
        val filePath = "contents/.github/CODEOWNERS"
        val response = ghClient.put("repos") {
            url { appendEncodedPathSegments(repo.getRepoName(), filePath) }
            contentType(ContentType.Application.Json)
            setBody(
                Commit(
                    message = "add $username to CODEOWNERS",
                    content = newContent,
                    sha = sha, // sha of the CODEOWNERS file
                    branch = "refs/heads/add-$username"
                )
            )
        }
    }

    /**
     * Open Pull-Request on remote.
     * @param repoName in the <org>/<name> format
     */
    suspend fun openPR(username: String, repoName: String): String {
        @Serializable
        data class Pr(val title: String, val head: String, val base: String)

        @Serializable
        data class Response(val html_url: String)

        val response: Response = ghClient.post("repos") {
            url { appendEncodedPathSegments(repoName, "pulls") }
            contentType(ContentType.Application.Json)
            setBody(
                Pr(
                    title = "Add $username to the CODEOWNERS file",
                    head = "refs/heads/add-$username",
                    base = "main"
                )
            )
        }.body()
        return response.html_url
    }
}