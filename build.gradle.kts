import org.spongepowered.asm.gradle.plugins.struct.DynamicProperties

plugins {
    idea
    java
    id("maven-publish")
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.+"
}

file("./dev.gradle.kts").createNewFile()
apply(from = "dev.gradle.kts")

val modID = "esc"
val modName = "Extended-Slot-Capacity"
val minecraftVersion = "1.20.1"
val forgeVersion = "47.1.3"
val lombokVersion = "1.18.32"
val mouseTweaksFileId = "5338457"

val mappingsChannel: String = project.findProperty("mappings_channel") as String? ?: "official"
val mappingsVersion: String = project.findProperty("mappings_version") as String? ?: minecraftVersion
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"

base {
    archivesName.set("$modName-$minecraftVersion")
    group = "mod.traister101" // http://maven.apache.org/guides/mini/guide-naming-conventions.html
    version = modVersion
}

java {
    // Mojang ships Java 17 to end users in 1.18+, so your mod should target Java 17.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withJavadocJar()
    withSourcesJar()
}

println(
    "Java: ${System.getProperty("java.version")}, JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")}), Arch: ${
        System.getProperty(
            "os.arch"
        )
    }"
)
println("Using mappings $mappingsChannel / $mappingsVersion with version $modVersion")

sourceSets.create("testmod")

configurations {
    get("testmodCompileClasspath").extendsFrom(compileClasspath.get())
    get("testmodRuntimeClasspath").extendsFrom(runtimeClasspath.get())
}

mixin {
    add(sourceSets.main.get(), "extended-slot-capacity.refmap.json")
    config("extended-slot-capacity.mixins.json")

    (debug as DynamicProperties).propertyMissing("verbose", true)
    (debug as DynamicProperties).propertyMissing("export", true)
    overwriteErrorLevel = "error"
    hotSwap = true
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)

    runs {
        all {
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            // Mixin is stupid
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "$projectDir/build/createSrgToMcp/output.srg")

            mods.create(modID) {
                source(sourceSets.main.get())
            }
        }

        register("client") {
            workingDirectory(file("run/client"))

            jvmArgs("-ea", "-Xmx4G", "-Xms4G")

            property("forge.enabledGameTestNamespaces", modID)
        }

        register("testmodClient") {
            parent(runs.getByName("client"))

            ideaModule("${project.name.replace(' ', '_')}.testmod")

            mods.create("testmod") {
                source(sourceSets["testmod"])
            }
        }

        register("server") {
            workingDirectory(file("run/server"))

            args("-nogui")
        }
    }
}

repositories {
    maven {
        name = "Curse maven"
        url = uri("https://www.cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge", "forge", "$minecraftVersion-$forgeVersion")

    "testmodImplementation"(sourceSets["main"].output)

    // Lombok cause why not
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Mixin annotation processor
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // Mouse Tweaks to make sure we are compatible
    compileOnly(fg.deobf("curse.maven:mouse-tweaks-60089:$mouseTweaksFileId"))
    runtimeOnly(fg.deobf("curse.maven:mouse-tweaks-60089:$mouseTweaksFileId"))
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true

        // Add directory exlusions
        arrayOf(
            "run", ".gradle", "build", ".idea", "gradle", "resources/venv", "resources/.idea"
        ).forEach { excludeDirs.add(file(it)) }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.base.archivesName.get()
            version = project.version.toString()

            // This makes maven produce a pom with deobf dependencies :| it works without it so we shall bodge
//            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("file://${project.projectDir}/mcmodsrepo")
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/traister101/ExtendedSlotCapacity")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks {

    jar {
        manifest {
            attributes["Implementation-Version"] = project.version
        }

        finalizedBy("reobfJar")
    }

    javadoc {
        source = sourceSets.main.get().java
        options.optionFiles(file("javadoc-options.txt"))
        options.encoding = "UTF-8"
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(arrayOf("-Xlint:all,-processing", "-Werror"))
    }
}