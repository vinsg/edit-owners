import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import model.Base64ContentSerializer
import model.CodeOwnersFile
import model.Repository
import model.Status
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Main GitHub API functionalities.
 */
class GithubService : KoinComponent {

    // DI injecting httpClient
    private val ghClient: HttpClient by inject()

    /**
     * Serves as auth validation and gets the GitHub username associated with the token.
     */
    suspend fun getUsername(): Result<String> = runCatching {
        @Serializable
        data class User(val url: String)

        val response: User = ghClient.get("user").body()
        response.url.substringAfterLast("/")
    }

    /**
     * Get a repository object using username.
     * @param username
     * @param repoName in the <org>/<name> format
     */
    suspend fun getRepo(username: String, repoName: String): Result<Repository> = runCatching {
        val codeownerFile = getCodeOwnersFile(repoName).getOrThrow()
        val shaRepo = getShaRepo(repoName).getOrThrow()
        Repository(
            repoName,
            shaRepo,
            codeownerFile,
            status = if (codeownerFile.content.contains(username)) Status.DONE else Status.READY,
            prURL = ""
        )
    }

    /**
     * Retrieves CODEOWNERS file from repo.
     * @param repoName in the <org>/<name> format
     */
    private suspend fun getCodeOwnersFile(repoName: String): Result<CodeOwnersFile> = runCatching {
        ghClient.get("repos") {
            url { appendEncodedPathSegments(repoName, "contents/.github/CODEOWNERS") }
        }.body()
    }

    /**
     * Get the unique Sha reference for the repo, used for creating a branch.
     * @param repoName in the <org>/<name> format
     */
    private suspend fun getShaRepo(repoName: String): Result<String> = runCatching {
        @Serializable
        data class ObjectSha(val sha: String)

        @Serializable
        data class Repo(
            @SerialName("object")
            val objectSha: ObjectSha
        )

        val refPath = "git/ref/heads/main"
        val response: Repo = ghClient.get("repos") {
            url { appendEncodedPathSegments(repoName, refPath) }
        }.body()
        response.objectSha.sha
    }

    /**
     * Create a branch with the name "add-[username]" on the remote.
     */
    suspend fun createBranch(username: String, repo: Repository): Result<HttpStatusCode> = runCatching {
        @Serializable
        data class RequestBody(val ref: String, val sha: String)

        val gitRefPath = "git/refs"
        val branchName = "refs/heads/add-$username"
        ghClient.post("repos") {
            url { appendEncodedPathSegments(repo.repoName, gitRefPath) }
            contentType(ContentType.Application.Json)
            setBody(RequestBody(branchName, repo.sha))
        }.status
    }

    /**
     * Create commit and push commit with "@[username]" string appended to CODEOWNERS file.
     */
    suspend fun createCommit(username: String, repo: Repository): Result<HttpStatusCode> = runCatching {
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
        ghClient.put("repos") {
            url { appendEncodedPathSegments(repo.repoName, filePath) }
            contentType(ContentType.Application.Json)
            setBody(
                Commit(
                    message = "add $username to CODEOWNERS",
                    content = newContent,
                    sha = sha, // sha of the CODEOWNERS file
                    branch = "refs/heads/add-$username"
                )
            )
        }.status
    }

    /**
     * Open Pull-Request on remote.
     * @param repoName in the <org>/<name> format
     */
    suspend fun openPR(username: String, repoName: String): Result<String> = runCatching {
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
        response.html_url
    }
}