package com.example.fitnessapp.mock
import android.content.SharedPreferences

class SharedPreferencesMock : SharedPreferences {

    private val data = mutableMapOf<String, String>()

    override fun getAll(): MutableMap<String, *> {
        return data
    }

    override fun getString(key: String?, defValue: String?): String? {
        return data[key] ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        throw UnsupportedOperationException("Not implemented in mock")
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return data[key]?.toInt() ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return data[key]?.toLong() ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return data[key]?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return data[key]?.toBoolean() ?: defValue
    }

    override fun contains(key: String?): Boolean {
        return data.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return SharedPreferencesEditorMock(data)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        // Not implemented in mock
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        // Not implemented in mock
    }

    class SharedPreferencesEditorMock(private val data: MutableMap<String, String>) : SharedPreferences.Editor {

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null && value != null) {
                data[key] = value
            }
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) {
                data[key] = value.toString()
            }
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) {
                data[key] = value.toString()
            }
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) {
                data[key] = value.toString()
            }
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) {
                data[key] = value.toString()
            }
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                data.remove(key)
            }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            data.clear()
            return this
        }

        override fun commit(): Boolean {
            return true
        }

        override fun apply() {
            // No-op for mock
        }
    }
}
