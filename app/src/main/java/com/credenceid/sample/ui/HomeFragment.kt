package com.credenceid.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.databinding.FragmentHomeBinding
import com.credenceid.sample.utils.Utils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding
        get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(
            layoutInflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel
            .getTitle(Screen.HOME)
            .plus("\n")
            .plus("Device ID : ${Utils.getAndroidId(requireContext())}")

        binding.qrButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_qrCodeEngagementFragment)
        }

        binding.nfcButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_nfcEngagementFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
