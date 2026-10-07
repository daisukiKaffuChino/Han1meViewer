import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.Properties

/**
 * Build configuration and verification tasks for the Rust H3/ECH transport.
 */
object EchH3 {

    const val NDK_VERSION = "28.2.13676358"
    const val CMAKE_VERSION = "3.22.1"
    const val TARGET_ABI = "arm64-v8a"

    private const val JNI_SYMBOL =
        "Java_io_github_daisukikaffuchino_han1meviewer_logic_network_ech_HyEchH3_h3Fetch"

    fun cmakeArguments(project: Project): List<String> {
        val root = project.rootProject.layout.projectDirectory
        return listOf(
            "-DANDROID_STL=c++_shared",
            "-DHN1_H3_STATIC_LIB=${
                root.file("native-h3/target/aarch64-linux-android/release/libhn1_h3.a")
                    .asFile.absolutePath
            }",
            "-DHN1_H3_VERSION_SCRIPT=${
                project.layout.projectDirectory.file("src/main/cpp/chino.exports")
                    .asFile.absolutePath
            }",
        )
    }

    fun configureBuild(project: Project) = with(project) {
        val nativeH3Directory = rootProject.layout.projectDirectory.dir("native-h3")
        val staticArchive = nativeH3Directory
            .file("target/aarch64-linux-android/release/libhn1_h3.a")
            .asFile
        val preparedMarker = nativeH3Directory.file("vendor/.patched").asFile
        val ndkDirectory = ndkDirectory()
        val python = if (isWindows()) "python" else "python3"
        val cargo = cargoExecutable()
        val verificationScript = rootProject.file("tools/verify_ech.py")

        val prepareEchH3 = tasks.register<Exec>("prepareEchH3") {
            group = "ech"
            description = "Downloads, verifies, extracts, and patches quiche 0.22."
            workingDir(rootProject.projectDir)
            commandLine(python, rootProject.file("tools/prepare_quiche.py").absolutePath)
            inputs.file(rootProject.file("tools/prepare_quiche.py"))
            outputs.file(preparedMarker)
        }

        val buildEchH3Arm64 = tasks.register<Exec>("buildEchH3Arm64") {
            group = "ech"
            description = "Builds the H3 ECH static archive for $TARGET_ABI."
            dependsOn(prepareEchH3)
            workingDir(nativeH3Directory.asFile)
            environment("ANDROID_NDK_HOME", ndkDirectory.absolutePath)
            environment("ANDROID_NDK_ROOT", ndkDirectory.absolutePath)
            configureWindowsToolchain(this, project, ndkDirectory)
            commandLine(
                cargo,
                "ndk",
                "-t",
                TARGET_ABI,
                "build",
                "--release",
                "--lib",
                "--locked",
            )
            inputs.dir(nativeH3Directory.dir("src"))
            inputs.file(nativeH3Directory.file("Cargo.toml"))
            inputs.dir(nativeH3Directory.dir("vendor/quiche"))
            outputs.file(staticArchive)
        }

        val verifyEchH3Archive = tasks.register("verifyEchH3Archive") {
            group = "ech"
            description = "Verifies that the H3 archive exports the expected JNI entry point."
            dependsOn(buildEchH3Arm64)
            doLast {
                check(staticArchive.isFile) {
                    "H3 archive was not produced: ${staticArchive.absolutePath}"
                }
                val llvmNm = ndkDirectory.resolve(
                    "toolchains/llvm/prebuilt/${llvmHostTag()}/bin/llvm-nm${exeSuffix()}",
                )
                check(llvmNm.isFile) { "llvm-nm not found: ${llvmNm.absolutePath}" }
                val symbols = providers.exec {
                    commandLine(llvmNm.absolutePath, "-g", staticArchive.absolutePath)
                }.standardOutput.asText.get()
                check(symbols.contains(JNI_SYMBOL)) {
                    "libhn1_h3.a does not export the HyEchH3 JNI entry point"
                }
            }
        }

        val verifyEchBridgeJs = tasks.register<Exec>("verifyEchBridgeJs") {
            group = "ech"
            description = "Checks the WebView ECH bridge JavaScript syntax and routing."
            workingDir(rootProject.projectDir)
            commandLine(python, verificationScript.absolutePath, "bridge")
            inputs.file(verificationScript)
            inputs.file(
                layout.projectDirectory.file(
                    "src/main/java/io/github/daisukikaffuchino/han1meviewer"
                        + "/logic/network/ech/EchWebBridgeJs.kt",
                ),
            )
        }

        val verifyEchApk = tasks.register<Exec>("verifyEchApk") {
            group = "ech"
            description = "Builds the debug APK and verifies the merged native libraries."
            dependsOn("assembleDebug")
            workingDir(rootProject.projectDir)
            commandLine(python, verificationScript.absolutePath, "apk")
            inputs.file(verificationScript)
        }

        tasks.matching {
            it.name.startsWith("configureCMake") ||
                it.name.startsWith("buildCMake") ||
                it.name == "preBuild"
        }.configureEach {
            dependsOn(verifyEchH3Archive)
        }
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(verifyEchBridgeJs)
        }
    }

    private fun configureWindowsToolchain(
        task: Exec,
        project: Project,
        ndkDirectory: File,
    ) {
        if (!isWindows()) return

        val rustupHome = rustupHome()
        val cargoHome = cargoHome()
        val hostToolchain = File(
            rustupHome,
            "toolchains/stable-x86_64-pc-windows-gnu",
        )
        check(hostToolchain.isDirectory) {
            "Rust GNU host toolchain is required on Windows: " +
                "rustup toolchain install stable-x86_64-pc-windows-gnu"
        }

        val hostTools = project.layout.buildDirectory.dir("ech-h3-host-tools").get().asFile
        val cmakeBin = ndkDirectory.parentFile.parentFile.resolve("cmake/$CMAKE_VERSION/bin")
        val llvmBin = ndkDirectory.resolve("toolchains/llvm/prebuilt/windows-x86_64/bin")
        task.doFirst {
            hostTools.mkdirs()
            File(llvmBin, "llvm-dlltool.exe").copyTo(
                File(hostTools, "dlltool.exe"),
                overwrite = true,
            )
            File(llvmBin, "llvm-ar.exe").copyTo(
                File(hostTools, "ar.exe"),
                overwrite = true,
            )
        }
        task.environment("RUSTUP_TOOLCHAIN", "stable-x86_64-pc-windows-gnu")
        task.environment("RUSTUP_HOME", rustupHome.absolutePath)
        task.environment("CARGO_HOME", cargoHome.absolutePath)
        task.environment(
            "PATH",
            listOf(
                hostTools.absolutePath,
                cmakeBin.absolutePath,
                File(cargoHome, "bin").absolutePath,
                System.getenv("PATH").orEmpty(),
            ).joinToString(File.pathSeparator),
        )
        task.environment("CMAKE", cmakeBin.resolve("cmake.exe").absolutePath)
        task.environment("CMAKE_GENERATOR", "Ninja")
        task.environment("CMAKE_MAKE_PROGRAM", cmakeBin.resolve("ninja.exe").absolutePath)
    }

    private fun Project.ndkDirectory(): File {
        val localProperties = rootProject.file("local.properties")
        val configuredSdk = if (localProperties.isFile) {
            Properties().apply {
                localProperties.inputStream().use(::load)
            }.getProperty("sdk.dir")
        } else {
            null
        }
        val sdkPath = configuredSdk
            ?: System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: error("Android SDK path is unavailable")
        return File(sdkPath, "ndk/$NDK_VERSION")
    }

    private fun Project.cargoExecutable(): String {
        return File(cargoHome(), "bin/cargo${exeSuffix()}")
            .takeIf(File::isFile)
            ?.absolutePath
            ?: "cargo"
    }

    private fun rustupHome(): File {
        val configured = System.getenv("RUSTUP_HOME")?.takeIf { it.isNotBlank() }
        return File(configured ?: File(System.getProperty("user.home"), ".rustup").absolutePath)
    }

    private fun cargoHome(): File {
        val configured = System.getenv("CARGO_HOME")?.takeIf { it.isNotBlank() }
        return File(configured ?: File(System.getProperty("user.home"), ".cargo").absolutePath)
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private fun llvmHostTag(): String =
        if (isWindows()) "windows-x86_64" else "linux-x86_64"

    private fun exeSuffix(): String = if (isWindows()) ".exe" else ""
}
