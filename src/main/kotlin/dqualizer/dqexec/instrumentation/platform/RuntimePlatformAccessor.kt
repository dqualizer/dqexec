package dqualizer.dqexec.instrumentation.platform

import dqualizer.dqlang.types.architecture.RuntimePlatform
import dqualizer.dqlang.types.architecture.ServiceDescription
import org.springframework.plugin.core.Plugin

interface RuntimePlatformAccessor : Plugin<String> {
    fun connect() {}

    fun disconnect() {}

    fun getTargetProcessID(processName: String): Int

    fun executeInContainer(cmd: String): String

    fun initalize(targetService: ServiceDescription, platformDescription: RuntimePlatform)
}
