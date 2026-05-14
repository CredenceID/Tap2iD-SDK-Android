package com.credenceid.sample.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
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

        val webView = view.findViewById<WebView>(R.id.webViewResult)

        webView.settings.apply {
            loadsImagesAutomatically = true
            blockNetworkImage = false
            useWideViewPort = false
            loadWithOverviewMode = true
        }

        webView.setBackgroundColor(Color.TRANSPARENT)

        val htmlString = sharedViewModel.storedVerificationHtml
        Log.i(
            "ResultFragment",
            "onViewCreated  htmlIsNull=${htmlString == null}  htmlLength=${htmlString?.length ?: 0}  " +
                "containsConfidenceLabel=${htmlString?.contains("confidence-label") ?: false}  " +
                "containsConfidenceText=${htmlString?.contains("Confidence:") ?: false}",
        )
        if (!htmlString.isNullOrEmpty()) {
            webView.loadDataWithBaseURL(null, htmlString, "text/html", "utf-8", null)
            Log.i("ResultFragment", "loadDataWithBaseURL() called  htmlLength=${htmlString.length}")
        } else {
            Log.w("ResultFragment", "WebView NOT loaded because storedVerificationHtml is null/empty")
        }
        binding.doneButton.setOnClickListener {
            sharedViewModel.clearVerificationData()
            findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
