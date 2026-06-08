package io.nativeplanet.launcher.domain.model

data class BootPackageStatus(
    val exists: Boolean,
    val valid: Boolean,
    val packageVersion: Int?,
    val bootMode: BootMode?,
    val ship: String?,
    val parent: String?,
    val pierPath: String?,
    val pillPath: String?,
    val pierExists: Boolean,
    val pillExists: Boolean,
    val keyFileExists: Boolean,
    val validationErrors: List<ValidationError>
) {
    companion object {
        val NONE = BootPackageStatus(
            exists = false,
            valid = false,
            packageVersion = null,
            bootMode = null,
            ship = null,
            parent = null,
            pierPath = null,
            pillPath = null,
            pierExists = false,
            pillExists = false,
            keyFileExists = false,
            validationErrors = emptyList()
        )
    }
}

data class ValidationError(
    val field: String,
    val code: String,
    val message: String
)
