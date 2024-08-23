package com.credenceid.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.databinding.FragmentLicenseVerificationBinding

class LicenseVerificationFragment : Fragment() {

    private var _binding: FragmentLicenseVerificationBinding? = null
    private val binding
        get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLicenseVerificationBinding.inflate(
            layoutInflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel.getTitle(Screen.LICENSE_KEY_VERIFICATION)

        binding.licenseKeyTL.setEndIconOnClickListener {
            binding.licenseKeyEt.text?.clear()
        }

        binding.licenseKeyTL.editText?.doOnTextChanged { text, _, _, _ ->
            binding.verifyButton.isEnabled = text?.length == 34
            if (text?.length != 34) {
                binding.licenseKeyTL.error = ""
            }
        }
        binding.verifyButton.setOnClickListener {
            binding.progressCircular.visibility = View.VISIBLE
            binding.nextButton.isEnabled = false
            sharedViewModel.initializeSdk(
                licenseKey = binding.licenseKeyTL.editText?.text.toString(),
                applicationContext = requireContext().applicationContext
            ) { result ->
                binding.progressCircular.visibility = View.INVISIBLE
                binding.nextButton.isEnabled = result.isSuccess
                binding.verifyButton.isEnabled = !result.isSuccess
                if (result.isSuccess) {
                    binding.resultTv.text = result.getOrDefault("")
                    binding.licenseKeyTL.error = ""
                } else {
                    binding.licenseKeyTL.error = result.exceptionOrNull()?.message.toString()
                    binding.resultTv.text = ""
                }
            }
        }

        binding.nextButton.setOnClickListener {
            findNavController().navigate(R.id.action_licenseVerificationFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
