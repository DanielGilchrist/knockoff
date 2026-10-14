package dev.danielgilchrist.knockoff.domain.notification

@JvmInline
value class PackageName(
    val value: String,
) {
    companion object {
        val GMAIL = PackageName("com.google.android.gm")
    }
}
