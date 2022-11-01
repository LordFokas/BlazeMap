plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "5.1.+"
    id("org.spongepowered.mixin") version "0.7.+"
}

// Toolchain versions
val minecraftVersion: String = "1.18.2"
val forgeVersion: String = "40.1.80"
val mixinVersion: String = "0.8.5"

// What it says on the tin
val modId: String = "blazemap"
val modVersion : String = "0.5.1-alpha"

// Dependency versions
val rubidiumVersion: String = "0.5.6"

// Dev playtest dependency versions
val tfcVersion: String = "Forge-${minecraftVersion}-2.1.11-beta"
val patchouliVersion: String = "${minecraftVersion}-71.1"
val cartographyVersion: String = "${minecraftVersion}-0.4.0-alpha3"


// Optional dev-env properties
val mappingsChannel: String = project.findProperty("mappings_channel") as String? ?: "official"
val mappingsVersion: String = project.findProperty("mappings_version") as String? ?: minecraftVersion
val minifyResources: Boolean = project.findProperty("minify_resources") as Boolean? ?: false
val useAdvancedClassRedef: Boolean = project.findProperty("use_advanced_class_redefinition") as Boolean? ?: false

println("Using mappings $mappingsChannel / $mappingsVersion")

base {
    archivesName.set("BlazeMap-$minecraftVersion")
    group = "net.dries007.tfc"
    version = modVersion
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
}

dependencies {
    minecraft("net.minecraftforge", "forge", version = "$minecraftVersion-$forgeVersion")

    // Rubidium
    compileOnly(fg.deobf("_:rubidium:${rubidiumVersion}"))
    // runtimeOnly(fg.deobf("_:rubidium:${rubidiumVersion}"))



    // ======== DEV PLAYTEST ==================================================
    // // TFC
    // compileOnly(fg.deobf("_:TerraFirmaCraft:${tfcVersion}"))
    // runtimeOnly(fg.deobf("_:TerraFirmaCraft:${tfcVersion}"))

    // // Patchouli
    // compileOnly(fg.deobf("_:Patchouli:$patchouliVersion"))
    // runtimeOnly(fg.deobf("_:Patchouli:$patchouliVersion"))

    // // Cartography
    // compileOnly(fg.deobf("_:Cartography:${cartographyVersion}"))
    // runtimeOnly(fg.deobf("_:Cartography:${cartographyVersion}"))
    // ========================================================================


    //if (System.getProperty("idea.sync.active") != "true") {
        annotationProcessor("org.spongepowered:mixin:${mixinVersion}:processor")
    //}
}

minecraft {
    mappings(mappingsChannel, mappingsVersion)
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        all {
            args("-mixin.config=$modId.mixins.json")

            property("forge.logging.console.level", "debug")
            property("forge.enabledGameTestNamespaces", modId)

            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "$projectDir/build/createSrgToMcp/output.srg")

            jvmArgs("-ea", "-Xmx4G", "-Xms4G")

            if (useAdvancedClassRedef) {
                jvmArg("-XX:+AllowEnhancedClassRedefinition")
            }

            mods.create(modId) {
                source(sourceSets.main.get())
            }
        }

        register("client") {
            workingDirectory("run/client")
        }

        register("server") {
            workingDirectory("run/server")

            arg("--nogui")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

tasks {
    processResources { }
    test { }

    jar {
        manifest {
            attributes["Implementation-Version"] = project.version
            attributes["MixinConfigs"] = "$modId.mixins.json"
        }
    }
}