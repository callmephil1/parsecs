package com.github.callmephil1.parsecs.ecs

import java.lang.reflect.InvocationTargetException

class MissingDependencyError(
    clazz: Class<*>,
    paramClass: Class<*>,
    reason: Throwable
) : Error("Missing dependency '${paramClass.name}' for '${clazz.name}' | Reason: '$reason'")

class DependencyInjection internal constructor(
    private val getters: MutableMap<Class<*>, DependencyInjection.() -> Any>
) {
    fun <T> get(clazz: Class<T>) = getters[clazz]!!.invoke(this) as T
    inline fun <reified T : Any> get() = this.get(T::class.java)

    class Builder {
        private val getters = mutableMapOf<Class<*>, (DependencyInjection) -> Any>()

        fun build(): DependencyInjection = DependencyInjection(getters)

        fun <T : Any> scoped(clazz: Class<T>): Builder {
            val ctor = clazz.constructors[0]
            ctor.isAccessible = true
            val params = ctor.parameterTypes

            val clazzGetter: DependencyInjection.() -> Any = getter@{
                val paramInstances = params.map {
                    val result = runCatching { this.get(it) }
                    when {
                        result.isSuccess -> { return@map result.getOrThrow() }
                        else -> {
                            var exception = result.exceptionOrNull()

                            if (exception is InvocationTargetException)
                                exception = exception.targetException
                            throw MissingDependencyError(clazz, it, exception!!)
                        }
                    }
                }.toTypedArray()
                return@getter ctor.newInstance(*paramInstances)!!
            }

            getters[clazz] = clazzGetter
            return this
        }

        fun <T : Any> scoped(clazz: Class<T>, factory: DependencyInjection.() -> T): Builder {
            getters[clazz] = factory
            return this
        }

        inline fun <reified T : Any> scoped() = scoped(T::class.java)
        inline fun <reified T : Any> scoped(noinline factory: DependencyInjection.() -> T) = scoped(T::class.java, factory)

        fun <T : Any> singleton(clazz: Class<T>): Builder {
            val ctor = clazz.constructors[0]
            ctor.isAccessible = true
            val params = ctor.parameterTypes

            var singleton: T? = null

            val clazzGetter: DependencyInjection.() -> T = getter@{
                if (singleton != null)
                    return@getter singleton as T

                val paramInstances = params.map {
                    val result = runCatching { this.get(it) }
                    when {
                        result.isSuccess -> { return@map result.getOrThrow() }
                        else -> {
                            var exception = result.exceptionOrNull()

                            if (exception is InvocationTargetException)
                                exception = exception.targetException

                            throw MissingDependencyError(clazz, it, exception!!)
                        }
                    }
                }.toTypedArray()

                singleton = ctor.newInstance(*paramInstances)!! as T
                return@getter singleton as T
            }

            getters[clazz] = clazzGetter
            return this
        }

        fun <T : Any> singleton(clazz: Class<T>, factory: DependencyInjection.() -> T): Builder {
            var singleton: T? = null

            val singletonGetter: DependencyInjection.() -> T = getter@{
                if (singleton != null)
                    return@getter singleton as T

                singleton = this.factory()
                return@getter singleton as T
            }

            getters[clazz] = singletonGetter
            return this
        }

        inline fun <reified T : Any> singleton() = singleton(T::class.java)
        inline fun <reified T : Any> singleton(noinline factory: DependencyInjection.() -> T) = singleton(T::class.java, factory)
    }
}

fun dependencyInjection(configure: DependencyInjection.Builder.() -> Unit): DependencyInjection {
    val builder = DependencyInjection.Builder()
    builder.configure()
    return builder.build()
}