package dqualizer.dqexec.instrumentation.platform

import io.github.dqualizer.dqlang.types.dam.architecture.RuntimePlatform
import io.github.dqualizer.dqlang.types.dam.architecture.ServiceDescription
import org.springframework.plugin.core.Plugin

interface RuntimePlatformAccessor : Plugin<String> {

    /**
     * Prepares the accessor for a connection.
     * Should only be called once per accessor.
     */
    fun setup(targetService: ServiceDescription, platformDescription: RuntimePlatform)

    /**
     * Connects to the target service.
     * This method should at least check whether the connection works but does <b>not</b>
     * necessarily result in a permanently open connection.
     * Calls should be idempotent.
     */
    fun connect() {}

    /**
     * Disconnects from the target service.
     * Executes cleanup tasks.
     */
    fun disconnect() {}

    /**
     * Trys to find the process ID of the target service.
     * @return the process ID or -1 if the process could not be found.
     */
    fun getTargetProcessID(processName: String): Int

    /**
     * Executes a command on the container (or machine).
     * @return the output of the command.
     */
    fun executeInServiceContainer(cmd: String): String

}
