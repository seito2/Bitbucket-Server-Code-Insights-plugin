package io.jenkins.plugins.codeInsights.infrastructure

import groovy.json.JsonSlurper
import io.jenkins.plugins.codeInsights.JenkinsLogger
import io.jenkins.plugins.codeInsights.domain.Annotation
import io.jenkins.plugins.codeInsights.domain.Severity
import io.jenkins.plugins.codeInsights.domain.coverage.CoverageMeasuredFile
import io.jenkins.plugins.codeInsights.domain.coverage.CoverageRequest
import io.jenkins.plugins.codeInsights.infrastructure.HttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

@SuppressWarnings('GroovyAccessibility')
class HttpClientTest extends Specification {

    def server = new MockWebServer()

    def mockLogger = Mock(PrintStream)

    def setup() {
        JenkinsLogger.INSTANCE.setLogger(mockLogger)
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'putReport shows success log when response status is 200'() {
        server.enqueue(new MockResponse(responseCode: 200))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.putReport()

        then:
        1 * mockLogger.println('[Code Insights plugin] Start to put reports')
        1 * mockLogger.println('[Code Insights plugin] Put reports: SUCCESS')
        server.requestCount == 1
        def request = server.takeRequest()
        request.headers['Authorization'] == 'Basic ' + Base64.encoder.encodeToString('username:password'.bytes)
        new JsonSlurper().parseText(request.body.readUtf8()) == [
            title   : 'Jenkins report',
            reporter: 'Jenkins',
            logoUrl : 'https://www.jenkins.io/images/logos/superhero/256.png',
        ]
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'putReport shows error log and throws error when response status is not 200'() {
        given:
        server.enqueue(new MockResponse(responseCode: errorCode))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.putReport()

        then:
        thrown(HttpClient.CallApiFailureException)
        1 * mockLogger.println('[Code Insights plugin] Start to put reports')
        1 * mockLogger.println('[Code Insights plugin] Put reports: FAILURE')
        server.requestCount == 1

        where:
        errorCode << [400, 404, 500, 503]
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'postAnnotations show success log when response status is 200'() {
        server.enqueue(new MockResponse(responseCode: 200))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.postAnnotations('test_name', [new Annotation(1, 'test message', '/test/repo/path', Severity.HIGH, null)])

        then:
        1 * mockLogger.println('[Code Insights plugin] Start to post test_name annotations')
        1 * mockLogger.println('[Code Insights plugin] Post test_name annotations: SUCCESS')
        server.requestCount == 1
        def request = server.takeRequest()
        request.headers['Authorization'] == 'Basic ' + Base64.encoder.encodeToString('username:password'.bytes)
        new JsonSlurper().parseText(request.body.readUtf8()) == [
            annotations: [
                [
                    line    : 1,
                    message : 'test message',
                    path    : '/test/repo/path',
                    severity: 'HIGH',
                    link    : null
                ],
            ],
        ]
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'postAnnotations shows error log and throws error when response status is not 200'() {
        server.enqueue(new MockResponse(responseCode: errorCode))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.postAnnotations('test_name', [new Annotation(1, 'test message', '/test/repo/path', Severity.LOW, null)])

        then:
        thrown(HttpClient.CallApiFailureException)
        1 * mockLogger.println('[Code Insights plugin] Start to post test_name annotations')
        1 * mockLogger.println('[Code Insights plugin] Post test_name annotations: FAILURE')
        server.requestCount == 1

        where:
        errorCode << [400, 404, 500, 503]
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'postCoverage show success log when response status is 200'() {
        server.enqueue(new MockResponse(responseCode: 200))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.postCoverage(new CoverageRequest([new CoverageMeasuredFile('src/main/java/App.java', 'C:1;P:2;U:3')]))

        then:
        1 * mockLogger.println('[Code Insights plugin] Start to post coverage')
        1 * mockLogger.println('[Code Insights plugin] Post coverage: SUCCESS')
        server.requestCount == 1
        def request = server.takeRequest()
        request.headers['Authorization'] == 'Basic ' + Base64.encoder.encodeToString('username:password'.bytes)
        request.headers['X-Atlassian-Token'] == 'no-check'
        request
        new JsonSlurper().parseText(request.body.readUtf8()) == [
            files: [
                [
                    path    : 'src/main/java/App.java',
                    coverage: 'C:1;P:2;U:3'
                ]
            ]
        ]
    }

    @SuppressWarnings('GroovyConstructorNamedArguments')
    def 'postCoverage shows error log and throws error when response status is not 200'() {
        server.enqueue(new MockResponse(responseCode: errorCode))
        def sut = new HttpClient('username', 'password', server.url('').toString(), 'project', 'repo', 'commit', 'key')

        when:
        sut.postCoverage(new CoverageRequest([new CoverageMeasuredFile('src/main/java/App.java', 'C:1;P:2;U:3')]))

        then:
        thrown(HttpClient.CallApiFailureException)
        1 * mockLogger.println('[Code Insights plugin] Start to post coverage')
        1 * mockLogger.println('[Code Insights plugin] Post coverage: FAILURE')
        server.requestCount == 1

        where:
        errorCode << [400, 404, 500, 503]
    }
}
