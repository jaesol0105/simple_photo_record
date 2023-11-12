package com.beinny.android.photorecord.ui.recorddetail

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.databinding.FragmentDateTimePickerBinding
import com.beinny.android.photorecord.getScaledBitmap
import com.beinny.android.photorecord.model.Record
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.util.*

private const val ARG_DATE = "date"

class DateTimePickerFragment() : DialogFragment() {
    private lateinit var binding: FragmentDateTimePickerBinding

    /** [콜백 인터페이스, RecordDetailFragment에 구현] */
    interface CallBacks {
        fun onDateSelected(date: Date)
    }

    /** [BottomSheetDialog를 생성하여 반환] */
    @SuppressLint("ResourceAsColor")
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        /** arg로 넘겨받은 date로 calendar 초기화 */
        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date

        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        /** [BottomSheetDialog 배경 설정, 레이아웃 인플레이트(데이터 바인딩)]*/
        val dlg = BottomSheetDialog(requireContext(),R.style.transparentDialog)
        binding = FragmentDateTimePickerBinding.inflate(LayoutInflater.from(requireContext()))
        dlg.setContentView(binding.root)

        /** [DatePicker TimePicker에 날짜 설정] */
        setDateAndTime(initialYear, initialMonth, initialDay, initialHour, initialMinute)

        /** [완료 버튼] */
        binding.tvDtpComplete.setOnClickListener {
            val resultDate : Date = GregorianCalendar(
                binding.datepickerDtp.year,
                binding.datepickerDtp.month,
                binding.datepickerDtp.dayOfMonth,
                binding.timepickerDtp.currentHour,
                binding.timepickerDtp.currentMinute
            ).time
            /** 최종 선택 날짜를 콜백함수를 통해 Fragment로 반환 */
            targetFragment?.let { fragment -> (fragment as DateTimePickerFragment.CallBacks).onDateSelected(resultDate) }
            dlg.dismiss()
        }

        /** [취소 버튼] */
        binding.tvDtpCancel.setOnClickListener {
            dlg.dismiss()
        }

        /** [날짜 버튼] */
        binding.layoutDtpDate.setOnClickListener {
            binding.datepickerDtp.visibility = View.VISIBLE
            binding.timepickerDtp.visibility = View.GONE
            binding.viewDtpDateBg.visibility = View.VISIBLE
            binding.viewDtpTimeBg.visibility = View.GONE
            binding.tvDtpNowDate.setTextColor(ContextCompat.getColor(PhotoRecordApplication.applicationContext(),R.color.blue_700))
            binding.tvDtpNowTime.setTextColor(ContextCompat.getColor(PhotoRecordApplication.applicationContext(),R.color.gray))
        }

        /** [시간 버튼] */
        binding.layoutDtpTime.setOnClickListener {
            binding.datepickerDtp.visibility = View.GONE
            binding.timepickerDtp.visibility = View.VISIBLE
            binding.viewDtpDateBg.visibility = View.GONE
            binding.viewDtpTimeBg.visibility = View.VISIBLE
            binding.tvDtpNowDate.setTextColor(ContextCompat.getColor(PhotoRecordApplication.applicationContext(),R.color.gray))
            binding.tvDtpNowTime.setTextColor(ContextCompat.getColor(PhotoRecordApplication.applicationContext(),R.color.blue_700))
        }

        /** [날짜/시간 변경시 textView 갱신 (API 26 이상)] */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.datepickerDtp.setOnDateChangedListener(DatePicker.OnDateChangedListener { _,_,month,day ->
                binding.tvDtpNowDate.text = convertDateToString(month,day)
            })
            binding.timepickerDtp.setOnTimeChangedListener(TimePicker.OnTimeChangedListener { _,hours,minutes ->
                binding.tvDtpNowTime.text = convertTimeToString(hours,minutes)
            })
        } else {
            binding.tvDtpNowDate.visibility = View.GONE
            binding.tvDtpNowTime.visibility = View.GONE
        }

        return dlg
    }

    /** [DatePicker TimePicker에 날짜 설정] */
    private fun setDateAndTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int) {
        binding.datepickerDtp.updateDate(year,month,day)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timepickerDtp.hour = hours
            binding.timepickerDtp.minute = minutes
        } else {
            binding.timepickerDtp.currentHour = hours
            binding.timepickerDtp.currentMinute = minutes
        }

        binding.tvDtpNowDate.text = convertDateToString(month,day)
        binding.tvDtpNowTime.text = convertTimeToString(hours,minutes)
    }

    /** [날짜를 문자열로 변환 (0월 0일)] */
    private fun convertDateToString(month: Int, day: Int) : String{
        return (month+1).toString() + "월 " + day.toString() + "일"
    }

    /** [시간을 문자열로 변환 (오전 0:00)] */
    private fun convertTimeToString(hours: Int, minutes: Int) : String{
        var min = if(minutes<10) {
            "0$minutes"
        } else {minutes.toString()}
        return if (hours<12) {
            "오전 $hours:$min"
        } else {
            "오후 " + (hours-12).toString() + ":" + min
        }
    }

    companion object {
        fun newInstance(date: Date): DateTimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE,date)
            }
            return DateTimePickerFragment().apply {
                arguments = args
            }
        }
    }
}