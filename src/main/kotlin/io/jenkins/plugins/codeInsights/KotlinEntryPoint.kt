package io.jenkins.plugins.codeInsights

import hudson.EnvVars
import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.codeInsights.infrastructure.FileTransferServiceImpl
import io.jenkins.plugins.codeInsights.infrastructure.GitRepo
import io.jenkins.plugins.codeInsights.infrastructure.HttpClient
import io.jenkins.plugins.codeInsights.usecase.CoverageUsecase
import io.jenkins.plugins.codeInsights.usecase.ReportUsecase

@Suppress("unused", "CanBeParameter")
class KotlinEntryPoint(
    private val run: Run<*, *>,
    private val workspace: FilePath,
    private val envVars: EnvVars,
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
    private val pmdFilePath: String,
    private val sonarQubeProjectKey: String,
    private val jacocoFilePath: String,
) {
    init {
        JenkinsLogger.setLogger(listener.logger)
    }

    fun delegate() {
        val httpClient = HttpClient(
            username, password, // credential
            bitbucketUrl, project, repositoryName, commitId, reportKey, // url
        )

        val fileTransferService = FileTransferServiceImpl(workspace, run)
        fileTransferService.copyFromWorkspaceToLocal(".git")

        val changedFiles = GitRepo(run.rootDir.resolve(".git")).use {
            it.detectChangedFiles(commitId, baseBranch)
        }

        if (reportKey.isNotBlank()) {
            ReportUsecase(
                httpClient,
                fileTransferService,
                workspace,
                checkstyleFilePath,
                spotBugsFilePath,
                srcPath,
                pmdFilePath,
                sonarQubeUrl,
                sonarQubeProjectKey,
                sonarQubeToken,
                sonarQubeUserName,
                sonarQubePassword,
                changedFiles
            ).execute()
        }

        if (jacocoFilePath.isNotBlank()) {
            CoverageUsecase(fileTransferService, jacocoFilePath, srcPath, changedFiles, httpClient).execute()
        }
    }
}
