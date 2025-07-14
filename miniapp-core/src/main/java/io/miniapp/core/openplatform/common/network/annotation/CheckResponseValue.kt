package io.miniapp.core.openplatform.common.network.annotation

enum class When {
    /** S is a subset of T  */
    ALWAYS,

    /** nothing definitive is known about the relation between S and T  */
    UNKNOWN,

    /** S intersection T is non empty and S - T is nonempty  */
    MAYBE,

    /** S intersection T is empty  */
    NEVER
}

@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE
)
@Retention(
    AnnotationRetention.RUNTIME
)
annotation class CheckResponseValue(val `when`: When = When.ALWAYS)

