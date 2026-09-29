import com.modrinth.minotaur.ModrinthExtension
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.Properties

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.1" apply false
    id("com.modrinth.minotaur") version "2.9.0" apply false
}

fun Properties.required(name: String): String =
    getProperty(name)?.takeIf(String::isNotBlank)
        ?: error("gradle/targets.properties is missing $name")

val targetProperties = Properties().apply {
    rootProject.file("gradle/targets.properties").inputStream().use(::load)
}
val targetNames = targetProperties.required("targets")
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
val targets = targetNames.associateWith { name ->
    Target(
        minecraft = targetProperties.required("$name.minecraft"),
    )
}
val modVersion = providers.gradleProperty("mod_version").get()

allprojects {
    group = "org.hypixelskyblockmods"
    version = modVersion

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

subprojects {
    val target = targets[name] ?: return@subprojects
    val artifactVersion = "${rootProject.version}+mc${target.minecraft}"

    apply(plugin = "net.fabricmc.fabric-loom")
    apply(plugin = "com.modrinth.minotaur")

    version = artifactVersion

    extensions.configure<ModrinthExtension> {
        projectId.set(providers.environmentVariable("MODRINTH_PROJECT_ID"))
        versionNumber.set(artifactVersion)
        versionName.set("PackBridge $artifactVersion")
        versionType.set("release")
        uploadFile.set(tasks.named("jar"))
        gameVersions.set(listOf(target.minecraft))
        loaders.set(listOf("fabric"))
        changelog.set(
            providers.environmentVariable("MODRINTH_CHANGELOG")
                .orElse("See the corresponding GitHub release for changes."),
        )
    }

    dependencies {
        add("minecraft", "com.mojang:minecraft:${target.minecraft}")
        add("implementation", "net.fabricmc:fabric-loader:0.19.3")
        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.13.4")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.13.4")
    }

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
    }

    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        clientOnlyMinecraftJar()
        runs {
            named("client") {
                ideConfigGenerated(true)
                runDir("run/${target.minecraft}")
            }
        }
    }

    extensions.configure<org.gradle.api.tasks.SourceSetContainer> {
        named("main") {
            java.setSrcDirs(
                listOf(
                    rootProject.file("src/main/java"),
                    rootProject.file("src/${target.minecraft}/java"),
                ),
            )
            resources.setSrcDirs(listOf(rootProject.file("src/main/resources")))
        }
        named("test") {
            java.setSrcDirs(listOf(rootProject.file("src/test/java")))
        }
    }

    val sourceSets = extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
    val smoke = sourceSets.create("smoke") {
        java.setSrcDirs(listOf(rootProject.file("src/smoke/java")))
        resources.setSrcDirs(listOf(rootProject.file("src/smoke/resources")))
        compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
        runtimeClasspath += sourceSets["main"].runtimeClasspath
    }
    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        mods {
            register("packbridge") { sourceSet(sourceSets["main"]) }
            register("packbridge_smoke") { sourceSet(smoke) }
        }
        runs {
            register("smoke") {
                client()
                source(smoke)
                vmArg("-Dpackbridge.smoke=true")
                runDir("run/smoke/${target.minecraft}")
                ideConfigGenerated(false)
            }
        }
    }

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    tasks.named<ProcessResources>("processResources") {
        inputs.property("version", artifactVersion)
        inputs.property("minecraft_version", target.minecraft)
        filesMatching("fabric.mod.json") {
            expand(
                "version" to artifactVersion,
                "minecraft_version" to target.minecraft,
            )
        }
    }

    tasks.named<Jar>("jar") {
        archiveBaseName.set("PackBridge")
        archiveVersion.set(project.version.toString())
        from(rootProject.file("LICENSE")) {
            rename { "LICENSE_PackBridge" }
        }
    }
}

val releaseTargets = targets.map { (name, target) ->
    check(rootProject.file("versions/$name/build.gradle.kts").isFile) {
        "Missing versions/$name/build.gradle.kts for configured target $name"
    }

    val artifactVersion = "$modVersion+mc${target.minecraft}"
    linkedMapOf(
        "project" to name,
        "minecraft" to target.minecraft,
        "artifactVersion" to artifactVersion,
        "modrinthTask" to ":versions:$name:modrinth",
        "jar" to "versions/$name/build/libs/PackBridge-$artifactVersion.jar",
    )
}
val releaseManifestJson = JsonOutput.prettyPrint(
    JsonOutput.toJson(
        linkedMapOf(
            "modVersion" to modVersion,
            "tag" to "v$modVersion",
            "targets" to releaseTargets,
        ),
    ),
) + "\n"

tasks.register<ReleaseManifestTask>("releaseManifest") {
    group = "publishing"
    description = "Writes metadata for every supported release target."
    manifestJson.set(releaseManifestJson)
    outputFile.set(layout.buildDirectory.file("release/manifest.json"))
}

abstract class ReleaseManifestTask : DefaultTask() {
    @get:Input
    abstract val manifestJson: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeManifest() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(manifestJson.get())
    }
}

data class Target(
    val minecraft: String,
)
