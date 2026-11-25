plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://maven.bitwig.com")
}

val bitwigApi = "com.bitwig:extension-api:25"

dependencies {
    compileOnly(bitwigApi)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    // Bitwig 6.1 に同梱の Java で動くように、21 向けにビルドする。
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

val bwextension = tasks.register<Jar>("bwextension") {
    archiveFileName.set("BitwigOSC.bwextension")
    destinationDirectory.set(layout.buildDirectory.dir("extension"))
    from(sourceSets.main.get().output)
}

val bitwigExtensionsDir = "${System.getProperty("user.home")}/Documents/Bitwig Studio/Extensions"

tasks.register<Copy>("installExtension") {
    description = "Builds the .bwextension and copies it into Bitwig's Extensions folder."
    from(bwextension)
    into(bitwigExtensionsDir)
}

tasks.named("build") { dependsOn(bwextension) }

// TouchOSC のレイアウトを作る道具。拡張と同じアドレスの決まりを使うが、.bwextension には入れない。
val touchosc: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    // BitwigControls.TRACK_COUNT を読むために要る(定数なので、動かすときには要らない)。
    "touchoscCompileOnly"(bitwigApi)
}

sourceSets.test {
    compileClasspath += touchosc.output
    runtimeClasspath += touchosc.output
}

tasks.register<JavaExec>("generateLayout") {
    description = "Writes the TouchOSC layout to touchosc/BitwigOSC.tosc."
    classpath = touchosc.runtimeClasspath
    mainClass.set("dev.bitwigosc.GenerateLayout")
    args(layout.projectDirectory.file("../touchosc/BitwigOSC.tosc").asFile.path)
}
