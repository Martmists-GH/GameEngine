package com.martmists.engine.scene

import com.martmists.engine.component.Component
import com.martmists.engine.ext.buildBuffer
import com.martmists.engine.util.Serializable
import java.nio.ByteBuffer
import kotlin.collections.containsKey
import kotlin.collections.plus
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

class GameObject(
    var name: String = "GameObject"
) : Serializable {
    val transform = Transform(this)
    private val components = mutableMapOf<KClass<out Component>, Pair<Component, Boolean>>()

    inline fun <reified T : Component> hasComponent() = hasComponent(T::class)
    fun <T : Component> hasComponent(klass: KClass<T>) = components.containsKey(klass)

    inline fun <reified T : Component> getComponent() = getComponent(T::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponent(klass: KClass<T>) = components[klass]!!.first as T

    inline fun <reified T : Component> addComponent() = addComponent({ T::class.primaryConstructor!!.call(it) }, T::class)
    inline fun <reified T : Component> addComponent(component: T) = addComponent({ component }, T::class)
    inline fun <reified T : Component> addComponent(noinline constructor: (GameObject) -> T) = addComponent(constructor, T::class)
    fun <T : Component> addComponent(constructor: (GameObject) -> T, klass: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        val allClasses = klass.allSuperclasses.filter { it.isSubclassOf(Component::class) && it != Component::class }
        val existing = allClasses.filter(components::containsKey)
        if (existing.isNotEmpty()) {
            throw IllegalArgumentException("Cannot add ${klass.simpleName} to $this because ${existing.first().simpleName} is already attached.")
        }

        val obj = constructor(this)
        obj.init()

        for (kl in allClasses + klass) {
            @Suppress("UNCHECKED_CAST")
            components[kl as KClass<out Component>] = obj to (kl == klass)
        }

        return obj
    }

    fun addChild(child: GameObject) = transform.addChild(child)
    fun removeChild(child: GameObject) = transform.removeChild(child)

    private fun delegateToComponents(fn: Component.() -> Unit) {
        for (value in components.values.toSet()) {
            value.first.fn()
        }
    }
    private fun <T> delegateToComponents(fn: Component.(T) -> Unit, arg: T) = delegateToComponents { fn(arg) }
    private fun delegateToChildren(fn: GameObject.() -> Unit) {
        for (child in transform.children) {
            child.gameObject.fn()
        }
    }
    private fun <T> delegateToChildren(fn: GameObject.(T) -> Unit, arg: T) = delegateToChildren { fn(arg) }

    fun clone(): GameObject {
        return GameObject("$name (clone)").also { go ->
            go.transform.copyFrom(transform)
            delegateToComponents {
                val klass = components.entries.first { e -> e.value.first === this }.key
                @Suppress("UNCHECKED_CAST")
                go.addComponent({ copyFor(go) }, klass as KClass<Component>)
            }
        }
    }

    internal fun flattenTree(): List<GameObject> {
        return listOf(this) + transform.children.flatMap { it.gameObject.flattenTree() }
    }

    internal fun preUpdate(delta: Float) {
        delegateToComponents(Component::preUpdate, delta)
        delegateToChildren(GameObject::preUpdate, delta)
    }
    internal fun onUpdate(delta: Float) {
        delegateToComponents(Component::onUpdate, delta)
        delegateToChildren(GameObject::onUpdate, delta)
    }
    internal fun postUpdate(delta: Float) {
        delegateToComponents(Component::postUpdate, delta)
        delegateToChildren(GameObject::postUpdate, delta)
    }

    override fun serialize(): ByteArray {
        val name = name.toByteArray()
        val trans = transform.serialize()
        val components = components.filter { it.value.second }.map {
            it.key.qualifiedName!!.toByteArray() to it.value.first.serialize()
        }
        return buildBuffer(4 + name.size + trans.size + 4 + components.sumOf { (fqn, comp) -> 4 + fqn.size + comp.size }) {
            putInt(name.size)
            put(name)
            put(trans)
            putInt(components.size)
            for ((fqn, comp) in components) {
                putInt(fqn.size)
                put(fqn)
                put(comp)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(buffer: ByteBuffer) {
        name = ByteArray(buffer.getInt()).also { buffer.get(it) }.decodeToString()
        transform.deserialize(buffer)
        repeat(buffer.getInt()) {
            val fqn = ByteArray(buffer.getInt()).also { buffer.get(it) }.decodeToString()
            val classRef = Class.forName(fqn)
            val comp = classRef.getConstructor(GameObject::class.java).newInstance(this) as Component
            comp.deserialize(buffer)
            val allClasses = classRef.kotlin.allSuperclasses.filter { it.isSubclassOf(Component::class) && it != Component::class }
            components[classRef.kotlin as KClass<out Component>] = comp to true
            for (klass in allClasses) {
                components[klass as KClass<out Component>] = comp to false
            }
        }
    }
}
