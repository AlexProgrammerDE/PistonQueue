plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
}
