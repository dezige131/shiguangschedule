package com.xingheyuzhuan.shiguangschedulemiuix.ui.schoolselection.web

import androidx.lifecycle.ViewModel
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedulemiuix.data.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewModel @Inject constructor(
    val courseConversionRepository: CourseConversionRepository,
    val timeSlotRepository: TimeSlotRepository
) : ViewModel()