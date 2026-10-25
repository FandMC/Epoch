plugins { application }

application {
    mainClass = "cn.fandmc.epoch.Epoch"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(project(":epoch-api"))
    implementation(platform("org.lwjgl:lwjgl-bom:3.4.3"))
    for (module in listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")) {
        implementation("org.lwjgl:$module")
        runtimeOnly("org.lwjgl:$module::natives-windows")
    }
    implementation("io.github.humbleui:skija-windows-x64:0.143.17")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.joml:joml:1.10.8")
}

tasks.withType<JavaExec>().configureEach { workingDir = rootProject.projectDir }
