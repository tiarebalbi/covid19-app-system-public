package smoke.env

import org.http4k.format.Moshi
import uk.nhs.nhsx.core.aws.xray.Tracing
import java.io.File
import java.io.FileNotFoundException

object SmokeTests {

    init {
        Tracing.disableXRayComplaintsForMainClasses()
    }

    fun loadConfig(): EnvConfig = deserializeOrThrow(loadConfigAsString())

    private fun loadConfigAsString() = loadConfigFileFor(loadConfigFileName())

    private fun loadConfigFileName() =
        System.getenv("SMOKE_TEST_CONFIG") ?: "../../../../../out/gen/config/test_config_branch.json"

    private fun loadConfigFileFor(envConfigFileName: String) = try {
        loadFile(envConfigFileName).readText()
    } catch (e: FileNotFoundException) {
        throw IllegalStateException(
            "Config file not found: ${envConfigFileName}, " +
                "run following commands to generate it: rake gen:config:<tgt_env>"
        )
    }

    fun loadStaticContent(fileName: String): String = loadFile("../../../../../src/static/$fileName").readText()

    private fun loadFile(filePath: String): File = File(filePath)

    private inline fun <reified T : Any> deserializeOrThrow(value: String): T = try {
        Moshi.asA(value)
    } catch (e: Exception) {
        throw IllegalStateException("Unable to deserialize configuration file, check generated config file against ${EnvConfig::class.simpleName} class")
    }
}

