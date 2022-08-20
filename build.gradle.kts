import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.support.zipTo
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.nativeplatform.platform.internal.DefaultOperatingSystem
import org.jetbrains.kotlin.daemon.common.toHexString
import java.security.MessageDigest

plugins {
    kotlin("multiplatform") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
}

group = "ca.vinsg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "2.0.3"
val koinVersion = "3.2.0"

val hostOs: DefaultOperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

kotlin {
    val nativeTarget = when {
        hostOs.isMacOsX -> listOf(macosArm64(), macosX64())
        hostOs.isLinux -> listOf(linuxX64())
        hostOs.isWindows -> listOf(mingwX64("windowsX64"))
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.map {
        it.binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-auth:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.insert-koin:koin-core:$koinVersion")
                implementation("com.github.ajalt.clikt:clikt:3.5.0")
                implementation("com.github.ajalt.mordant:mordant:2.0.0-beta7")
                implementation("com.squareup.okio:okio:3.2.0")
            }
        }

        if (hostOs.isLinux) {
            val linuxX64Main by getting {
                dependsOn(commonMain)
                dependencies {
                    implementation("io.ktor:ktor-client-curl:$ktorVersion")
                }
            }
            val linuxX64Test by getting
        }

        if (hostOs.isMacOsX) {
            val macosArm64Main by getting {
                dependsOn(commonMain)
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:$ktorVersion")
                }
            }
            val macosX64Main by getting {
                dependsOn(macosArm64Main)
            }
        }

        if (hostOs.isWindows) {
            val windowsX64Main by getting {
                dependencies {
                    implementation("io.ktor:ktor-client-curl:$ktorVersion")
                }
            }
        }
    }
}

/*!
 * Original boilerplate code by Andrew Carlson
 * MIT Licensed,Copyright (c) 2022 Andrew Carlson, see LICENSE.ktpack.md for details
 *
 * Credits to Andrew:
 * https://github.com/DrewCarlson/ktpack/blob/main/ktpack/build.gradle.kts
 */

fun createPackageReleaseTask(target: String) {
    val extension = if (hostOs.isWindows) ".exe" else ".kexe"
    tasks.create("packageRelease${target.capitalized()}") {
        dependsOn("linkReleaseExecutable${target.capitalized()}X64")
        if (hostOs.isMacOsX) {
            dependsOn("linkReleaseExecutable${target.capitalized()}Arm64")
        }
        doFirst {
            var executable = buildDir.resolve("bin/${target}X64/releaseExecutable/edit-owners$extension")
            if (hostOs.isMacOsX) {
                val executableArm = buildDir.resolve("bin/${target}Arm64/releaseExecutable/edit-owners$extension")
                val executableUniversal = buildDir.resolve("bin/${target}/releaseExecutable/edit-owners$extension")
                executableUniversal.parentFile.mkdirs()
                exec {
                    commandLine("lipo")
                    args(
                        "-create",
                        "-output",
                        executableUniversal.absolutePath,
                        executable.absolutePath,
                        executableArm.absolutePath
                    )
                }.assertNormalExitValue()
                executable = executableUniversal
            }

            val releaseName = "edit-owners-$target.zip"
            val releaseBinDir = buildDir.resolve("release/bin")
            val releaseZip = buildDir.resolve("release/$releaseName")
            val releaseZipChecksum = buildDir.resolve("release/$releaseName.sha256")
            copy {
                from(executable)
                into(releaseBinDir)
                rename { if (hostOs.isWindows) it else it.removeSuffix(extension) }
            }
            zipTo(releaseZip, releaseBinDir)
            val sha256 = MessageDigest.getInstance("SHA-256")
            releaseZip.forEachBlock { buffer, _ -> sha256.update(buffer) }
            releaseZipChecksum.writeText(sha256.digest().toHexString())
        }
    }
}

when {
    hostOs.isLinux -> createPackageReleaseTask("linux")
    hostOs.isWindows -> createPackageReleaseTask("windows")
    hostOs.isMacOsX -> createPackageReleaseTask("macos")
}

// Temporary for Mordant color bug, waiting for Colormath hierarchical structure support (KMM v1.7.0)
tasks.matching { it.name == "compileCommonMainKotlinMetadata" }.all {
    enabled = false
}