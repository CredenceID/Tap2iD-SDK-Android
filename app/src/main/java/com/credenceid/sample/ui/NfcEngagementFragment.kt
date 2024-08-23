package com.credenceid.sample.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.credenceid.sample.R
import com.credenceid.sample.common.Screen
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.databinding.FragmentNfcEngagementBinding
import com.credenceid.tap2idSdk.api.MdocVerificationListener
import com.credenceid.tap2idSdk.api.models.VerificationStage
import com.credenceid.tap2idSdk.core.model.MdocAttributes
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
        binding.titleTv.text = sharedViewModel.getTitle(Screen.NFC)
        binding.cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_nfcEngagementFragment_to_homeFragment)
        }
    }

    private fun verifyWithNfc() {
        sharedViewModel.verifyWitNfc(requireActivity(), mdocVerificationListener = object : MdocVerificationListener {
            override fun onVerificationCompleted(result: MdocAttributes) {
                Log.d("Sample", result.toString())
                setStatusOnUi("Verification Success")
                lifecycleScope.launch {
                    delay(5000)
                    val identityResult: String = sharedViewModel.prettyPrintJson(result)
                    val directions = NfcEngagementFragmentDirections.actionNfcEngagementFragmentToResultFragment(identityResult)
                    findNavController().navigate(directions)
                }
            }

            override fun onVerificationStageCompleted(stage: VerificationStage) {
                Log.d("Sample", stage.name)
                setStatusOnUi("Completed :".plus(stage.toString()))
            }

            override fun onVerificationStageError(stage: VerificationStage, error: Throwable) {
                Log.e("Sample", "${stage.name}: ${error.message}")
                setStatusOnUi("Error :".plus(stage.toString()))
                setStatusOnUi("Message :".plus(error.message))
                setStatusOnUi("Verification Failed")
            }

            override fun onVerificationStageStarted(stage: VerificationStage) {
                Log.d("Sample", stage.name)
                setStatusOnUi("Started:".plus(stage.toString()))
            }
        })
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
