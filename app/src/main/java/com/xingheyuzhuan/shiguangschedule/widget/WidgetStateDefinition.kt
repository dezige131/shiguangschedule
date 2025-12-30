package com.xingheyuzhuan.shiguangschedule.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object WidgetStateDefinition : GlanceStateDefinition<WidgetSnapshot> {

    // 1. 定义序列化器：直接使用 Protobuf 生成的 parseFrom 和 writeTo
    object WidgetSnapshotSerializer : Serializer<WidgetSnapshot> {
        override val defaultValue: WidgetSnapshot = WidgetSnapshot.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): WidgetSnapshot {
            return try {
                WidgetSnapshot.parseFrom(input)
            } catch (e: Exception) {
                defaultValue
            }
        }

        override suspend fun writeTo(t: WidgetSnapshot, output: OutputStream) {
            t.writeTo(output)
        }
    }

    // 2. 这里的 getLocation 必须指向 DataStore 实际存储的位置
    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/widget_snapshot_$fileKey.pb")
    }

    // 3. 直接实现 getDataStore，手动创建实例
    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetSnapshot> {
        // 使用 DataStoreFactory 或者直接通过委托访问
        return context.widgetSnapshotStore
    }
}

// 在外部定义 DataStore 委托
private val Context.widgetSnapshotStore by dataStore(
    fileName = "widget_snapshot.pb",
    serializer = WidgetStateDefinition.WidgetSnapshotSerializer
)