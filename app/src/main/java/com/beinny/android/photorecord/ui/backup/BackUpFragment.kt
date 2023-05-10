package com.beinny.android.photorecord.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.beinny.android.photorecord.databinding.FragmentBackUpBinding

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

        val loginBtn : Button = binding.loginBtn
        val backupBtn : Button = binding.backupBtn
        val restoreBtn : Button = binding.restoreBtn
        //val textView: TextView = binding.textBackUp
        //backUpViewModel.text.observe(viewLifecycleOwner, Observer {
        //    textView.text = it
        //})

        loginBtn.setOnClickListener { view ->
            Toast.makeText(context,"로그인버튼", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}