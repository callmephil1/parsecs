package com.github.callmephil1.parsecs

import com.github.callmephil1.parsecs.ecs.DependencyInjection
import kotlin.test.*

class ScopedClass

class ScopedClassWithScopedClass(val scoped: ScopedClass)

class ScopedClassWithSingletonClass(val singleton: SingletonClass)

class SingletonClass

class SingletonWithScoped(val scoped: ScopedClass)

class FactoryClass(val value: Int = 0)

class DependencyInjectionTests {

    @Test
    fun `Getting a singleton multiple times returns the same objects`() {
        val builder = DependencyInjection.Builder()
            .singleton(SingletonClass::class.java)
        val app = builder.build()

        val singleton = app.get(SingletonClass::class.java)
        val singleton2 = app.get(SingletonClass::class.java)

        assertNotNull(singleton)
        assertNotNull(singleton2)
        assertSame(singleton, singleton2)
    }

    @Test
    fun `Getting a scoped service multiple times returns different objects`() {
        val builder = DependencyInjection.Builder()
            .scoped(ScopedClass::class.java)
        val app = builder.build()

        val scope = app.get(ScopedClass::class.java)
        val scope2 = app.get(ScopedClass::class.java)

        assertNotNull(scope)
        assertNotNull(scope2)
        assertNotSame(scope, scope2)
    }

    @Test
    fun `Getting a singleton service with scoped dependency returns same objects`() {
        val app = DependencyInjection.Builder()
            .singleton(SingletonWithScoped::class.java)
            .scoped(ScopedClass::class.java)
            .build()

        val singleton = app.get(SingletonWithScoped::class.java)
        val singleton2 = app.get(SingletonWithScoped::class.java)

        assertNotNull(singleton)
        assertNotNull(singleton2)
        assertSame(singleton, singleton2)
        assertSame(singleton.scoped, singleton2.scoped)
    }

    @Test
    fun `Getting a scoped service with scoped dependency returns different objects`() {
        val app = DependencyInjection.Builder()
            .scoped(ScopedClassWithScopedClass::class.java)
            .scoped(ScopedClass::class.java)
            .build()

        val scoped = app.get(ScopedClassWithScopedClass::class.java)
        val scoped2 = app.get(ScopedClassWithScopedClass::class.java)

        assertNotNull(scoped)
        assertNotNull(scoped2)
        assertNotEquals(scoped, scoped2)
        assertNotEquals(scoped.scoped, scoped2.scoped)
    }

    @Test
    fun `Getting a scoped service with singleton dependency returns different scope with equal singleton`() {
        val app = DependencyInjection.Builder()
            .scoped(ScopedClassWithSingletonClass::class.java)
            .singleton(SingletonClass::class.java)
            .build()

        val scoped = app.get(ScopedClassWithSingletonClass::class.java)
        val scoped2 = app.get(ScopedClassWithSingletonClass::class.java)

        assertNotNull(scoped)
        assertNotNull(scoped2)
        assertNotEquals(scoped, scoped2)
        assertEquals(scoped.singleton, scoped2.singleton)
    }

    @Test
    fun `Getting a service with a factory returns object from factory`() {
        val app = DependencyInjection.Builder()
            .scoped(FactoryClass::class.java) {
                FactoryClass(10)
            }
            .build()

        val service = app.get(FactoryClass::class.java)
        val service2 = app.get(FactoryClass::class.java)

        assertNotNull(service)
        assertEquals(10, service.value)
        assertNotSame(service, service2)
    }
}