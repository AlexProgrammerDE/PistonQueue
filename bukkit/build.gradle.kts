plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("de.exlll:configlib-yaml:4.8.0")

    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
}
