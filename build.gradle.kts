plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "dk.mineclub.plus"
version = providers.environmentVariable("VERSION").getOrElse("1.0.0")

labyMod {
    defaultPackageName = "dk.mineclub.plus"

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "mineclubplus"
        displayName = "MineClub+"
        author = "Synodic Studio"
        description =
            "Overblik over din transporter, økonomi og statistik på MineClub. " +
                "© Synodic Studio - synodicstudio.com. Må ikke kopieres eller videredistribueres."
        // Everything from 1.8.9 upwards. The star on its own would also claim versions no jar
        // is built for, so the range starts at the oldest one that is.
        minecraftVersion = "1.8.9<*"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
