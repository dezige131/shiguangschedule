package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.lifecycle.ViewModel
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WebViewModel @Inject constructor(
    val courseConversionRepository: CourseConversionRepository,
    val timeSlotRepository: TimeSlotRepository
) : ViewModel()