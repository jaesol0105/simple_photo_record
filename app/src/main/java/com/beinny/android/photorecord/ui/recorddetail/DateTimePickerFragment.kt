package com.beinny.android.photorecord.ui.recorddetail

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.DatePicker
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
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

    // 콜백 인터페이스, RecordDetailFragment에 구현.
    interface CallBacks {
        fun onDateSelected(date: Date)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)

        val dlg = BottomSheetDialog(requireContext(),R.style.transparentDialog)
        binding = FragmentDateTimePickerBinding.inflate(LayoutInflater.from(requireContext()))
        //dlg.setContentView(R.layout.fragment_date_time_picker)
        dlg.setContentView(binding.root)

        setDateAndTime(initialYear, initialMonth, initialDay, initialHour, initialMinute)

        binding.tvDateTimePickerSave.setOnClickListener {
            val resultDate : Date = GregorianCalendar(
                binding.datepicker.year,
                binding.datepicker.month,
                binding.datepicker.dayOfMonth,
                binding.timepicker.currentHour,
                binding.timepicker.currentMinute
            ).time
            targetFragment?.let { fragment -> (fragment as DatePickerFragment.CallBacks).onDateSelected(resultDate) }
            dlg.dismiss()
        }
        binding.tvDateTimePickerCancel.setOnClickListener {
            dlg.dismiss()
        }
        binding.tvDateTimePickerDate.setOnClickListener {
            binding.datepicker.visibility = View.VISIBLE
            binding.timepicker.visibility = View.GONE
        }
        binding.tvDateTimePickerTime.setOnClickListener {
            binding.datepicker.visibility = View.GONE
            binding.timepicker.visibility = View.VISIBLE
        }

        return dlg
    }

    private fun setDateAndTime(year: Int, month: Int, day: Int, hours: Int, minutes: Int) {
        binding.datepicker.updateDate(year,month,day)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timepicker.hour = hours
            binding.timepicker.minute = minutes
        } else {
            binding.timepicker.currentHour = hours
            binding.timepicker.currentMinute = minutes
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