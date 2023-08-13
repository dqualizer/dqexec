package dqualizer.dqexec.instrumentation.platform.included

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.architecture.RuntimePlatform
import io.github.dqualizer.dqlang.types.architecture.ServiceDescription
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.*


/**
 * Uses <a href="https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md">docker-java</a> to connect to a docker container.
 */
@Service
class DockerContainerAccessor : RuntimePlatformAccessor {

    private lateinit var targetContainerName: String
    private lateinit var dockerClient: DockerClient

    override fun setup(targetService: ServiceDescription, platformDescription: RuntimePlatform) {

        this.targetContainerName = targetService.getDeploymentName()

        if (!supports(platformDescription.platformName))
            throw Exception("Platform ${platformDescription.platformName} not supported for this accessor.")

        prepareDockerClient(platformDescription)
    }

    private fun prepareDockerClient(platformDescription: RuntimePlatform) {
        val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withProperties(Properties().apply { putAll(platformDescription.platformSettings) })
            .apply {
                if (platformDescription.platformUri != null)
                    withDockerHost(platformDescription.platformUri)
            }
            .build()
        val httpDockerClientConfig = ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .sslConfig(dockerClientConfig.sslConfig)
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build()

        dockerClient = DockerClientImpl.getInstance(dockerClientConfig, httpDockerClientConfig)
    }

    override fun connect() {
        val containers = dockerClient.listContainersCmd().exec()
        if (containers.size > 0 && containers.any { it.names.contains("/$targetContainerName") })
            println("Container $targetContainerName found.")
        else
            throw Exception("Container $targetContainerName not found.")

    }

    override fun getTargetProcessID(processCmd: String): Int {
        val result = executeInServiceContainer(
            "ps -ef | grep \"$processCmd\" | grep -v -E \"grep|ps -ef\" | awk '{print \$1}'"
        ).trim()
        return result.split("\n")[0].trim().toInt()
    }

    override fun executeInServiceContainer(cmd: String): String {
        val outputStream = ByteArrayOutputStream()

        dockerClient
            .execStartCmd(
                dockerClient.execCreateCmd(targetContainerName)
                    .withAttachStdout(true)
                    .withCmd("sh", "-c", cmd)
                    .exec().id
            )
            .exec(object : ResultCallback.Adapter<Frame>() {
                override fun onNext(item: Frame) {
                    outputStream.write(item.payload)
                }
            })
            .awaitCompletion()
        return outputStream.toString()
    }

    override fun supports(delimiter: String): Boolean {
        return delimiter.lowercase(Locale.getDefault()) == "docker"
    }
}
