package com.crp.wikiAppNew

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast


class WebViewFragment : Fragment() {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String): WebViewFragment {
            val fragment = WebViewFragment()
            val args = Bundle()
            args.putString(ARG_URL, url)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_web_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url ?: "")
                return true
            }
        }
        webView.settings.javaScriptEnabled = true

        val url = arguments?.getString(ARG_URL)
        Log.d("WebViewFragment", "URL recibida: $url")

        if (!url.isNullOrBlank()) {
            webView.loadUrl(url)
        } else {
            Log.e("WebViewFragment", "URL es null o está vacía")
            Toast.makeText(requireContext(), "URL inválida. No se puede cargar la página.", Toast.LENGTH_SHORT).show()
        }
    }
}