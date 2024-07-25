package dqualizer.dqexec.util

import kotlin.io.path.Path
import kotlin.io.path.exists

class EnvironmentChecker {

    companion object {
        val isRunningInDocker: Boolean = Path("/proc/1/cgroup").exists()
    }
}