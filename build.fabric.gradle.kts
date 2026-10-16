plugins {
    // Applies the correct Loom variant for the Minecraft version
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
val minecraftVersion = findProperty("deps.minecraft") as String? ?: sc.current.version
version = "${property("mod.version")}+$minecraftVersion"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava = JavaVersion.VERSION_25

// Releases to mark compatible on Modrinth / CurseForge
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

dependencies {
    // Only the Fabric API modules we use; they are optional at runtime (`suggests` in fabric.mod.json)
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:$minecraftVersion")
    // No-op on unobfuscated 26.x, kept so older versions could be added later
    loomx.applyMojangMappings()

    // `mod*` configurations are converted by loom-back-compat on 26.1+
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    fapi("fabric-api-base", "fabric-command-api-v2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// `-Phinexoptimize.<name>=<value>` becomes `-Dhinexoptimize.<name>=<value>` in the dev game,
// e.g. `-Phinexoptimize.autobench=spin -Phinexoptimize.vanilla=true`
val gameProperties = providers.gradlePropertiesPrefixedBy("hinexoptimize.").get()

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Names lambdas, useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run") // Shared between versions
        jvmArguments.add("-Dmixin.debug.export=true") // Dumps transformed classes to run/.mixin.out
        gameProperties.forEach { (key, value) -> jvmArguments.add("-D$key=$value") }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, value: String) {
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", sc.properties["mod.id"])
            register("name", sc.properties["mod.name"])
            register("version", sc.properties["mod.version"])
            register("minecraft", sc.properties["mod.mc_compat"])
            register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("META-INF/neoforge.mods.toml")
    }

    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "${it}_$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
