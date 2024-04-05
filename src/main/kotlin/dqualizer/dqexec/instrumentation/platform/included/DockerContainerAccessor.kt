package dqualizer.dqexec.instrumentation.platform.included

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import dqualizer.dqexec.instrumentation.platform.RuntimePlatformAccessor
import io.github.dqualizer.dqlang.types.dam.architecture.RuntimePlatform
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
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

        if (!supports(platformDescription.name))
            throw Exception("Platform ${platformDescription.name} not supported for this accessor.")

        prepareDockerClient(platformDescription)
    }

    private fun prepareDockerClient(platformDescription: RuntimePlatform) {
        val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withProperties(Properties().apply { putAll(platformDescription.settings) })
            .withDockerHost("tcp://localhost:2375")
//            TODO: Uncomment the following lines to enable TLS verification
//            .withDockerTlsVerify(true)
//            .withDockerCertPath("/home/user/.docker")
            .apply {
                if (platformDescription.uri != null)
                    withDockerHost(platformDescription.uri!!.host)
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
        checkIfClientIsInitialized()

        val containers = dockerClient.listContainersCmd().exec()
        if (containers.size > 0 && containers.any { it.names.contains("/$targetContainerName") })
            println("Container $targetContainerName found.")
        else
            throw Exception("Container $targetContainerName not found.")

    }

    /**
     * @param processName the name of the process to search for. e.g. "java"
     */
    override fun getTargetProcessID(processName: String): Int {
        val result = executeInServiceContainer(
            "ps -ef | grep \"$processName\" | grep -v -E \"grep|ps -ef\" | awk '{print \$1}'"
        ).trim()
        return result.split("\n")[0].trim().toInt()
    }

    override fun executeInServiceContainer(cmd: String): String {
        checkIfClientIsInitialized()

        val outputStream = ByteArrayOutputStream()

        dockerClient
            .execStartCmd(
                dockerClient.execCreateCmd(targetContainerName)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
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

    private fun checkIfClientIsInitialized() {
        if (!this::dockerClient.isInitialized) {
            throw Exception("DockerClient not initialized. Call setup() first.")
        }
    }

    override fun supports(delimiter: String): Boolean {
        return delimiter.lowercase(Locale.getDefault()) == "docker"
    }
}
