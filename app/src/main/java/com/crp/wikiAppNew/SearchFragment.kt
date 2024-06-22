package com.crp.wikiAppNew

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Observer

import com.crp.wikiAppNew.databinding.FragmentSearchBinding
import com.crp.wikiAppNew.view.State
import com.crp.wikiAppNew.view.WikiAdapter
import com.crp.wikiAppNew.viewmodel.WikiViewModel


import androidx.appcompat.widget.SearchView
import org.koin.androidx.viewmodel.ext.android.viewModel


class SearchFragment : Fragment() {


    private val viewModel: WikiViewModel by viewModel()// Reference to the ViewModel
    private lateinit var binding: FragmentSearchBinding // ViewBinding for the fragment's layout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup SearchView
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    viewModel.getWikiData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Observe ViewModel's LiveData to update UI
        viewModel.postsLiveData.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is State.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.wikiRv.visibility = View.GONE
                    binding.noDataState.visibility = View.GONE
                }
                is State.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.wikiRv.visibility = View.VISIBLE
                    binding.noDataState.visibility = View.GONE

                    val articles = state.data.query?.pages ?: emptyList()
                    val adapter = WikiAdapter(articles) { url ->
                        val fragmentTransaction = requireFragmentManager().beginTransaction()
                        val wikiDetailFragment = WebViewFragment.newInstance(url.toString())
                        fragmentTransaction.replace(R.id.fragment_container, wikiDetailFragment)
                        fragmentTransaction.addToBackStack(null) // Optional: add to backstack
                        fragmentTransaction.commit()
                    }
                    binding.wikiRv.adapter = adapter
                }
                is State.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.wikiRv.visibility = View.GONE
                    binding.noDataState.visibility = View.VISIBLE
                }
            }
        })
    }

    companion object {
        fun newInstance() = SearchFragment()
    }

}