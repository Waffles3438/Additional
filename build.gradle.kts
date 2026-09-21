plugins {
    java
    id("net.fabricmc.fabric-loom-remap") version "1.17.21"
    id("ploceus") version "1.17.7"
}

group = "me.waffles"
version = "3.1.2+1.8.9-ornithe"
base { archivesName.set("additional") }

repositories {
    mavenCentral()
    google()
    maven("https://repo.polyfrost.org/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

ploceus { setIntermediaryGeneration(2) }

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings(ploceus.featherMappings("2"))
    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    modImplementation("org.polyfrost.oneconfig:1.8.9-ornithe:1.2.3")
    ploceus.dependOsl("0.21.0")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

configurations.configureEach { exclude(group = "org.lwjgl.lwjgl") }
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
tasks.test { useJUnitPlatform() }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
