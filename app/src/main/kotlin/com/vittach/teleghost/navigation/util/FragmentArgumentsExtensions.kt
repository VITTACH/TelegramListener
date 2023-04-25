package com.vittach.teleghost.navigation.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun Fragment.booleanArg() = BooleanArgDelegate()

fun Fragment.longArg() = LongArgDelegate()

fun Fragment.intArg() = IntArgDelegate()

fun Fragment.stringArg() = StringArgDelegate()

fun Fragment.stringArgOrNull() = StringArgOrNullDelegate()

fun <T : Serializable> Fragment.serializableArg() = SerializableArgDelegate<T>()

fun <T : Serializable> Fragment.serializableOrNullArg() = SerializableOrNullArgDelegate<T>()

class BooleanArgDelegate : ReadWriteProperty<Fragment, Boolean> {

    override fun getValue(thisRef: Fragment, property: KProperty<*>): Boolean {
        return thisRef.requireArguments().getBoolean(property.name)
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Boolean) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putBoolean(property.name, value)
        }
    }
}

class LongArgDelegate : ReadWriteProperty<Fragment, Long> {

    override fun getValue(thisRef: Fragment, property: KProperty<*>): Long {
        return thisRef.requireArguments().getLong(property.name)
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Long) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putLong(property.name, value)
        }
    }
}

class IntArgDelegate : ReadWriteProperty<Fragment, Int> {

    override fun getValue(thisRef: Fragment, property: KProperty<*>): Int {
        return thisRef.requireArguments().getInt(property.name)
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Int) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putInt(property.name, value)
        }
    }
}

class StringArgDelegate : ReadWriteProperty<Fragment, String> {

    override fun getValue(thisRef: Fragment, property: KProperty<*>): String {
        return thisRef.arguments?.getString(property.name) ?: ""
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: String) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putString(property.name, value)
        }
    }
}

class StringArgOrNullDelegate : ReadWriteProperty<Fragment, String?> {

    override fun getValue(thisRef: Fragment, property: KProperty<*>): String? {
        return thisRef.arguments?.getString(property.name)
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: String?) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putString(property.name, value)
        }
    }
}

class SerializableArgDelegate<T : Serializable> : ReadWriteProperty<Fragment, T> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return thisRef.requireArguments().getSerializable(property.name) as T
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            this.putSerializable(property.name, value)
        }
    }
}

class SerializableOrNullArgDelegate<T : Serializable> : ReadWriteProperty<Fragment, T?> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? {
        val args = thisRef.requireArguments()
        return if (args.containsKey(property.name)) {
            args.getSerializable(property.name) as T
        } else {
            null
        }
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        thisRef.arguments = (thisRef.arguments ?: Bundle()).apply {
            if (value != null) {
                putSerializable(property.name, value)
            }
        }
    }
}
