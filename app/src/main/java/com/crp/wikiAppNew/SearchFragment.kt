package com.crp.wikiAppNew

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.Toast

import androidx.appcompat.widget.SearchView

import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.crp.wikiAppNew.databinding.FragmentSearchBinding


import com.crp.wikiAppNew.utils.Helper
import com.crp.wikiAppNew.view.State
import com.crp.wikiAppNew.view.WikiAdapter
import com.crp.wikiAppNew.viewmodel.WikiViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: WikiViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchNow.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { callSearchApi(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        viewModel.postsLiveData.observe(viewLifecycleOwner, { state ->
            when (state) {
                is State.Loading -> {
                    loadingState(true)
                }
                is State.Success -> {
                    loadingState(false)
                    binding.wikiRv.adapter = state.data.query?.pages?.let {
                        WikiAdapter(it) { articleTitle -> openArticle(articleTitle.toString()) }
                    }
                }
                is State.Error -> {
                    loadingState(false)
                    binding.noDataState.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun openArticle(articleTitle: String) {
        val articleFragment = WebViewFragment.newInstance(articleTitle)
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, articleFragment)
        transaction.addToBackStack(null)  // Para poder navegar hacia atrás
        transaction.commit()
    }

    private fun loadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingState.visibility = View.VISIBLE
            binding.noDataState.visibility = View.GONE
        } else {
            Helper.hideKeyboard(requireActivity())
            binding.loadingState.visibility = View.GONE
        }
    }

    private fun callSearchApi(searchString: String) {
        if (Helper.isNetworkAvailable(requireContext()))
            viewModel.getWikiData(searchString)
        else
            Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_SHORT).show()
    }
}