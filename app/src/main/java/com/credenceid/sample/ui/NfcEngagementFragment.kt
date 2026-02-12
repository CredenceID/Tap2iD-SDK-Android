package com.credenceid.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.common.VerificationResultCallback
import com.credenceid.sample.databinding.FragmentNfcEngagementBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NfcEngagementFragment : Fragment() {

    private var _binding: FragmentNfcEngagementBinding? = null
    private val binding
        get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val statusQueue = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNfcEngagementBinding.inflate(
            layoutInflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        verifyWithNfc()
    }

    private fun setupView() {
        binding.titleTv.text = sharedViewModel.getTitle(Screen.NFC, requireContext())
        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_nfcEngagementFragment_to_homeFragment)
        }
    }

    private fun verifyWithNfc() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.verifyWitNfc(requireActivity()).collect { result ->
                    when (result) {
                        is VerificationResultCallback.StageCompleted -> {
                            if (isAdded) {
                                setStatusOnUi(result.message)
                            }
                        }

                        is VerificationResultCallback.StageError -> {
                            if (isAdded) {
                                setStatusOnUi(result.message)
                            }
                        }

                        is VerificationResultCallback.StageStarted -> {
                            if (isAdded) {
                                setStatusOnUi(result.message)
                            }
                        }

                        is VerificationResultCallback.VerificationCompleted -> {
                            if (isAdded) {
                                if (result.hasValidationErrors) {
                                    setStatusOnUi("Verification Successful with some validation failures")
                                    delay(5000)
                                } else {
                                    setStatusOnUi("Verification Successful")
                                }
                                val directions = NfcEngagementFragmentDirections.actionNfcEngagementFragmentToResultFragment()
                                findNavController().navigate(directions)
                            }
                        }

                        VerificationResultCallback.VerificationProcessStarted -> {
                            if (isAdded) {
                                setStatusOnUi("Verifying mDL...")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setStatusOnUi(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            statusQueue.append(message.plus("\n"))
            binding.statusTv.text = statusQueue.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
