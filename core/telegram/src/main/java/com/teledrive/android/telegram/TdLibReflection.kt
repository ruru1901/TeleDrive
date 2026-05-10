package com.teledrive.android.telegram

import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class TdLibReflection(
    private val packageName: String = "org.drinkless.tdlib",
) {
    val clientClass: Class<*> = loadClass("$packageName.Client")
    private val tdApiPrefix = "$packageName.TdApi"
    private val resultHandlerClass: Class<*> = loadClass("$packageName.Client\$ResultHandler")
    private val exceptionHandlerClass: Class<*> = loadClass("$packageName.Client\$ExceptionHandler")
    private val functionClass: Class<*> = loadClass("$packageName.TdApi\$Function")

    fun createClient(
        onUpdate: (Any) -> Unit,
        onDefaultException: (Throwable) -> Unit,
    ): Any {
        val updateHandler = resultHandlerProxy(onUpdate)
        val exceptionHandler = exceptionHandlerProxy(onDefaultException)
        val createMethod = clientClass.methods.firstOrNull { method ->
            method.name == "create" && method.parameterTypes.size == 3
        } ?: error("TDLib Client.create(...) method was not found.")
        val args = createMethod.parameterTypes.mapIndexed { index, type ->
            when {
                type == resultHandlerClass -> if (index == 0) updateHandler else null
                type == exceptionHandlerClass -> exceptionHandler
                else -> null
            }
        }.toTypedArray()
        return requireNotNull(createMethod.invoke(null, *args)) {
            "TDLib Client.create(...) returned null."
        }
    }

    fun send(client: Any, function: Any, handler: (Any) -> Unit) {
        client.javaClass
            .getMethod("send", functionClass, resultHandlerClass)
            .invoke(client, function, resultHandlerProxy(handler))
    }

    fun execute(function: Any): Any? =
        clientClass.getMethod("execute", functionClass).invoke(null, function)

    fun newObject(name: String): Any =
        clazz(name).getDeclaredConstructor().newInstance()

    fun newObjectOrNull(name: String): Any? =
        runCatching { newObject(name) }.getOrNull()

    fun newFunction(name: String): Any = newObject(name)

    fun clazz(name: String): Class<*> = loadClass("$tdApiPrefix\$$name")

    fun constructorId(name: String): Int = clazz(name).getField("CONSTRUCTOR").getInt(null)

    fun constructorOf(value: Any): Int =
        value.javaClass.getMethod("getConstructor").invoke(value) as Int

    fun simpleName(value: Any): String = value.javaClass.simpleName

    fun field(value: Any, name: String): Any? =
        fieldOrNull(value.javaClass, name)?.get(value)

    fun longField(value: Any, name: String): Long? =
        (field(value, name) as? Number)?.toLong()

    fun intField(value: Any, name: String): Int? =
        (field(value, name) as? Number)?.toInt()

    fun stringField(value: Any, name: String): String? =
        field(value, name) as? String

    fun booleanField(value: Any, name: String): Boolean? =
        field(value, name) as? Boolean

    fun set(value: Any, name: String, fieldValue: Any?) {
        val field = fieldOrNull(value.javaClass, name) ?: return
        field.set(value, coerce(field.type, fieldValue))
    }

    fun setIfPresent(value: Any, name: String, fieldValue: Any?) {
        runCatching { set(value, name, fieldValue) }
    }

    fun hasField(value: Any, name: String): Boolean =
        fieldOrNull(value.javaClass, name) != null

    fun resultHandlerProxy(handler: (Any) -> Unit): Any =
        Proxy.newProxyInstance(
            resultHandlerClass.classLoader,
            arrayOf(resultHandlerClass),
            InvocationHandler { _, method, args ->
                if (method.name == "onResult" && args?.isNotEmpty() == true && args[0] != null) {
                    handler(args[0])
                }
                null
            },
        )

    private fun exceptionHandlerProxy(handler: (Throwable) -> Unit): Any =
        Proxy.newProxyInstance(
            exceptionHandlerClass.classLoader,
            arrayOf(exceptionHandlerClass),
            InvocationHandler { _, method, args ->
                if (method.name == "onException" && args?.firstOrNull() is Throwable) {
                    handler(args[0] as Throwable)
                }
                null
            },
        )

    private fun fieldOrNull(type: Class<*>, name: String): Field? =
        runCatching { type.getField(name) }.getOrNull()

    private fun coerce(type: Class<*>, value: Any?): Any? {
        if (value == null) return null
        return when {
            type == java.lang.Boolean.TYPE && value is Boolean -> value
            type == java.lang.Integer.TYPE && value is Number -> value.toInt()
            type == java.lang.Long.TYPE && value is Number -> value.toLong()
            type == java.lang.Double.TYPE && value is Number -> value.toDouble()
            type == ByteArray::class.java && value is String -> value.toByteArray()
            type == String::class.java && value is ByteArray -> String(value)
            else -> value
        }
    }

    private fun loadClass(name: String): Class<*> =
        Class.forName(name, false, TdLibReflection::class.java.classLoader)

    companion object {
        private val packageCandidates = listOf(
            "org.drinkless.tdlib",
            "org.drinkless.td.libcore.telegram",
        )

        fun available(): TdLibReflection =
            availableOrNull() ?: error(
                "TDLib Java classes are not packaged. Expected one of: ${packageCandidates.joinToString()}",
            )

        fun availableOrNull(): TdLibReflection? =
            packageCandidates.firstNotNullOfOrNull { packageName ->
                runCatching { TdLibReflection(packageName) }.getOrNull()
            }
    }
}
