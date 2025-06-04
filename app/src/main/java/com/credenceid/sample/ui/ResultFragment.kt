package com.credenceid.sample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.credenceid.sample.R
import com.credenceid.sample.common.SharedViewModel
import com.credenceid.sample.databinding.FragmentResultBinding

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding
        get() = _binding!!

    private val args: ResultFragmentArgs by navArgs()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentResultBinding.inflate(
            layoutInflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Portrait
        sharedViewModel.userPortraitLiveData?.let {
            binding.userPortraitIv.setImageBitmap(it)
        }

        //mDL Attributes
        val identityDataJsonString = args.mDocResultJsonString
        identityDataJsonString?.let {
            binding.resultTv.text = it
        }

        binding.doneButton.setOnClickListener {
            findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
