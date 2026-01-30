plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        trimTrailingWhitespace()
        leadingTabsToSpaces(2)
        endWithNewline()
    }
}
