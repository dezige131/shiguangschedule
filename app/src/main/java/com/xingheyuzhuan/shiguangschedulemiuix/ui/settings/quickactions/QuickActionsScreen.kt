package com.xingheyuzhuan.shiguangschedulemiuix.ui.settings.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedulemiuix.AppRoute
import com.xingheyuzhuan.shiguangschedulemiuix.LocalAppNavigator
import com.xingheyuzhuan.shiguangschedulemiuix.R
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsScreen(
) {
    val navigator = LocalAppNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val titleText = stringResource(R.string.item_quick_actions)

    Scaffold(
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = titleText,
                largeTitle = titleText,
                navigationIcon = {
                    IconButton(onClick = {
                        if (navigator.isNotEmpty()) {
                            navigator.removeAt(navigator.size - 1)
                        }
                    }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.a11y_back),
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_quick_action_category_schedule),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, top = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.item_schedule_tweak),
                            summary = stringResource(R.string.desc_schedule_tweak),
                            onClick = { navigator.add(AppRoute.TweakSchedule) } // <- 修改
                        )

                        ArrowPreference(
                            title = stringResource(R.string.item_quick_delete),
                            summary = stringResource(R.string.quick_delete_subtitle),
                            onClick = { navigator.add(AppRoute.QuickDelete) } // <- 修改
                        )
                    }
                }
            }
        }
    }
}