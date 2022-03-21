package io.jenkins.plugins.codeInsights

import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.codeInsights.framework.FileTransferServiceImpl
import io.jenkins.plugins.codeInsights.usecase.ExecutableAnnotationProvidersBuilder
import io.jenkins.plugins.codeInsights.usecase.GitRepo

@Suppress("unused", "CanBeParameter")
class KotlinEntryPoint(
    private val run: Run<*, *>,
    private val workspace: FilePath,
    private val launcher: Launcher,
    private val listener: TaskListener,
    private val bitbucketUrl: String,
    private val project: String,
    private val reportKey: String,
    private val username: String,
    private val password: String,
    private val sonarQubeUrl: String,
    private val sonarQubeToken: String,
    private val sonarQubeUserName: String,
    private val sonarQubePassword: String,
    private val repositoryName: String,
    private val commitId: String,
    private val srcPath: String,
    private val baseBranch: String,
    private val checkstyleFilePath: String,
    private val spotBugsFilePath: String,
    private val sonarQubeProjectKey: String,
) {
    init {
        JenkinsLogger.setLogger(listener.logger)
    }

    fun delegate() {
        val httpClient = HttpClient(
            username, password, // credential
            bitbucketUrl, project, repositoryName, commitId, reportKey, // url
        )

        httpClient.putReport()

        val fileTransferService = FileTransferServiceImpl(workspace, run)
        fileTransferService.copyFromWorkspaceToLocal(".git")
        val changedFiles = GitRepo(run.rootDir.resolve(".git").absolutePath)
            .detectChangedFiles(commitId, baseBranch)
        val executables = ExecutableAnnotationProvidersBuilder(fileTransferService)
            .setCheckstyle(checkstyleFilePath, workspace.remote)
            .setSpotBugs(spotBugsFilePath, srcPath)
            .setSonarQube(sonarQubeUrl, sonarQubeProjectKey, sonarQubeToken, sonarQubeUserName, sonarQubePassword)
            .build()
        for (executable in executables) {
            JenkinsLogger.info("Start ${executable.name}")
            val annotations = executable.convert().filter { changedFiles.contains(it.path) }
            if (annotations.isNotEmpty()) {
                httpClient.postAnnotations(executable.name, annotations)
            }
            JenkinsLogger.info("Finish ${executable.name}")
        }
    }
}
