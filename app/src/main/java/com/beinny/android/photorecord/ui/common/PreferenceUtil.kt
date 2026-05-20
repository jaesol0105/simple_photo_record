package com.beinny.android.photorecord.ui.common

import android.content.Context
import android.content.SharedPreferences
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.common.MAX_RECENT_SEARCHES
import com.beinny.android.photorecord.common.RECENT_SEARCHES_KEY
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceUtil(context: Context) {
    private val pref: SharedPreferences = context.getSharedPreferences(context.getString(R.string.preferences_file_key),Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getInt(key: String, defValue: Int):Int {
        return pref.getInt(key, defValue)
    }

    fun setInt(key: String, int: Int) {
        pref.edit().putInt(key,int).apply()
    }

    // 저장된 최근 검색어 목록 반환 (최신 순)
    fun getRecentSearches(): List<String> {
        val json = pref.getString(RECENT_SEARCHES_KEY, null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    // 최근 검색어 추가 — 중복 제거 후 맨 앞 삽입, MAX_RECENT_SEARCHES 초과 시 마지막 제거
    fun addRecentSearch(query: String) {
        val list = getRecentSearches().toMutableList()
        list.remove(query)
        list.add(0, query)
        if (list.size > MAX_RECENT_SEARCHES) list.removeAt(list.lastIndex)
        pref.edit().putString(RECENT_SEARCHES_KEY, gson.toJson(list)).apply()
    }

    // 특정 검색어를 최근 검색어 목록에서 제거
    fun removeRecentSearch(query: String) {
        val list = getRecentSearches().toMutableList()
        list.remove(query)
        pref.edit().putString(RECENT_SEARCHES_KEY, gson.toJson(list)).apply()
    }
}