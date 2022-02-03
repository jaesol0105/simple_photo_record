package com.beinny.android.dailylook.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.beinny.android.dailylook.databinding.FragmentBackUpBinding

class BackUpFragment : Fragment() {
    private lateinit var backUpViewModel: BackUpViewModel
    private var _binding: FragmentBackUpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        backUpViewModel =
            ViewModelProvider(this).get(BackUpViewModel::class.java)

        _binding = FragmentBackUpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textBackUp
        backUpViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}