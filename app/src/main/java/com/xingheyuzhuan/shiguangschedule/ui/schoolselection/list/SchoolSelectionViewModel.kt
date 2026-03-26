package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolHistoryRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import school_index.AdapterCategory
import school_index.School
import javax.inject.Inject

/**
 * 负责一级学校选择页面的数据管理、状态维护和过滤逻辑。
 * 使用 Hilt 注入 Context 和 Repository。
 */
@HiltViewModel
class SchoolSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: SchoolHistoryRepository
) : ViewModel() {

    private val _allSchools = MutableStateFlow<List<School>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow(AdapterCategory.BACHELOR_AND_ASSOCIATE)
    val selectedCategory: StateFlow<AdapterCategory> = _selectedCategory

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 观察历史记录
    val schoolHistory: StateFlow<SchoolHistoryModel> = historyRepository.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchoolHistoryModel()
        )

    init {
        loadSchools()
    }

    val displayCategories: List<AdapterCategory> = listOf(
        AdapterCategory.BACHELOR_AND_ASSOCIATE,
        AdapterCategory.POSTGRADUATE,
        AdapterCategory.GENERAL_TOOL
    )

    // 过滤逻辑
    val filteredSchools: StateFlow<List<School>> = combine(
        _allSchools,
        _searchQuery,
        _selectedCategory
    ) { allSchools, query, category ->
        val categoryFiltered = allSchools.filter { school ->
            school.adaptersList.any { adapter -> adapter.category == category }
        }

        if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { school ->
                school.name.contains(query, ignoreCase = true) ||
                        school.initial.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 索引字母
    val initials: StateFlow<List<String>> = filteredSchools.map { schools ->
        schools.map { it.initial.uppercase() }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun loadSchools() {
        viewModelScope.launch {
            _isLoading.value = true
            val schools = SchoolRepository.getSchools(context)
            _allSchools.value = schools
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: AdapterCategory) {
        _selectedCategory.value = category
    }

    fun saveLastSchool(school: School) {
        viewModelScope.launch {
            historyRepository.saveLastSchool(_selectedCategory.value, school)
        }
    }

    fun clearHistory(category: AdapterCategory) {
        viewModelScope.launch {
            historyRepository.clearHistory(category)
        }
    }

    /**
     * 获取当前选中的适配器列表
     */
    suspend fun getAdaptersForSchoolAndCategory(schoolId: String): List<school_index.Adapter> {
        val allAdapters = SchoolRepository.getAdaptersForSchool(context, schoolId)
        val currentCategory = _selectedCategory.value
        return allAdapters.filter { adapter ->
            adapter.category == currentCategory
        }
    }
}