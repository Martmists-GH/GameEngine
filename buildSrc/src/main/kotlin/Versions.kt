object Versions {
    const val joml = "1.10.9-SNAPSHOT"
    const val lwjgl = "3.3.6"
    const val imgui = "1.89.0"

    val lwjglNatives = Pair(
        System.getProperty("os.name")!!,
        System.getProperty("os.arch")!!
    ).let { (name, arch) ->
        when {
            "FreeBSD" == name -> "freebsd"
            arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> when {
                arrayOf("arm", "aarch64").any { arch.startsWith(it) } -> "linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
                arch.startsWith("ppc") -> "linux-ppc64le"
                arch.startsWith("riscv") -> "linux-riscv64"
                else -> "linux"
            }
            arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> "macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            arrayOf("Windows").any { name.startsWith(it) } -> if (arch.contains("64")) {
                "windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            } else {
                "windows-x86"
            }
            else                                                                            ->
                throw Error("Unrecognized or unsupported platform: ($name, $arch). Please set \"lwjglNatives\" manually")
        }
    }
}
