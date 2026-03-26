package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import school_index.School

/**
 * 单个学校的选择记录模型
 * 包含跳转到适配器选择页面所需的核心字段
 */
data class CategoryLastSchool(
    val id: String = "",
    val name: String = "",
    val resourceFolder: String = ""
) {
    /** * 判断该记录是否为空（即该分类下从未选择过学校）
     * 只有当 id 不为空时，才认为有有效的历史记录
     */
    val isEmpty: Boolean get() = id.isBlank()

    /**
     * 辅助方法：将保存的记录转换回 Protobuf 的 School 对象
     * 方便 UI 直接复用现有的 SchoolItem 组件
     */
    fun toSchool(): School {
        return School.newBuilder()
            .setId(id)
            .setName(name)
            .setResourceFolder(resourceFolder)
            .build()
    }
}

/**
 * 学校选择记录模型 (SchoolHistoryModel)
 * 分类存储“本科/专科”、“研究生”和“通用工具”三个标签页对应的上次打开学校。
 */
data class SchoolHistoryModel(
    val bachelor: CategoryLastSchool = CategoryLastSchool(),
    val postgraduate: CategoryLastSchool = CategoryLastSchool(),
    val general: CategoryLastSchool = CategoryLastSchool()
) {
    companion object {
        // --- DataStore 存储键定义 ---

        // 本科/专科分类
        val KEY_BACHELOR_ID = stringPreferencesKey("last_school_bachelor_id")
        val KEY_BACHELOR_NAME = stringPreferencesKey("last_school_bachelor_name")
        val KEY_BACHELOR_FOLDER = stringPreferencesKey("last_school_bachelor_folder")

        // 研究生分类
        val KEY_POSTGRAD_ID = stringPreferencesKey("last_school_postgrad_id")
        val KEY_POSTGRAD_NAME = stringPreferencesKey("last_school_postgrad_name")
        val KEY_POSTGRAD_FOLDER = stringPreferencesKey("last_school_postgrad_folder")

        // 通用工具分类
        val KEY_GENERAL_ID = stringPreferencesKey("last_school_general_id")
        val KEY_GENERAL_NAME = stringPreferencesKey("last_school_general_name")
        val KEY_GENERAL_FOLDER = stringPreferencesKey("last_school_general_folder")

        /**
         * 从 DataStore 的 Preferences 对象中解析出完整的 SchoolHistoryModel
         */
        fun fromPreferences(prefs: Preferences): SchoolHistoryModel {
            return SchoolHistoryModel(
                bachelor = CategoryLastSchool(
                    id = prefs[KEY_BACHELOR_ID] ?: "",
                    name = prefs[KEY_BACHELOR_NAME] ?: "",
                    resourceFolder = prefs[KEY_BACHELOR_FOLDER] ?: ""
                ),
                postgraduate = CategoryLastSchool(
                    id = prefs[KEY_POSTGRAD_ID] ?: "",
                    name = prefs[KEY_POSTGRAD_NAME] ?: "",
                    resourceFolder = prefs[KEY_POSTGRAD_FOLDER] ?: ""
                ),
                general = CategoryLastSchool(
                    id = prefs[KEY_GENERAL_ID] ?: "",
                    name = prefs[KEY_GENERAL_NAME] ?: "",
                    resourceFolder = prefs[KEY_GENERAL_FOLDER] ?: ""
                )
            )
        }

        /**
         * 辅助方法：根据类别返回对应的一组 DataStore Keys
         */
        fun getKeysForCategory(category: school_index.AdapterCategory): Triple<Preferences.Key<String>, Preferences.Key<String>, Preferences.Key<String>> {
            return when (category) {
                school_index.AdapterCategory.BACHELOR_AND_ASSOCIATE ->
                    Triple(KEY_BACHELOR_ID, KEY_BACHELOR_NAME, KEY_BACHELOR_FOLDER)

                school_index.AdapterCategory.POSTGRADUATE ->
                    Triple(KEY_POSTGRAD_ID, KEY_POSTGRAD_NAME, KEY_POSTGRAD_FOLDER)

                school_index.AdapterCategory.GENERAL_TOOL ->
                    Triple(KEY_GENERAL_ID, KEY_GENERAL_NAME, KEY_GENERAL_FOLDER)

                else -> Triple(KEY_BACHELOR_ID, KEY_BACHELOR_NAME, KEY_BACHELOR_FOLDER)
            }
        }
    }
}