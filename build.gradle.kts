plugins { base }

allprojects {
    group = "cn.fandmc"
    version = "0.1.0"
}

subprojects {
    repositories { mavenCentral() }
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(25)
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release = 25
        }
    }
}

tasks.named("build") { dependsOn(":epoch-api:build", ":epoch-client:build") }
tasks.named("clean") { dependsOn(":epoch-api:clean", ":epoch-client:clean") }
