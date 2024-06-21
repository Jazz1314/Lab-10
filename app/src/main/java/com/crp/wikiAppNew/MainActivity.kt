package com.crp.wikiAppNew

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.crp.wikiAppNew.databinding.ActivityMainBinding

import com.crp.wikiAppNew.utils.Helper
import com.crp.wikiAppNew.view.State
import com.crp.wikiAppNew.view.WikiAdapter
import com.crp.wikiAppNew.viewmodel.WikiViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel



class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private val viewModel: WikiViewModel by viewModel()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationService()
        }

        binding.searchNow.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { callSearchApi(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })


        viewModel.postsLiveData.observe(this, Observer { state ->
            when (state) {
                is State.Loading -> {
                    loadingState(true)
                }
                is State.Success -> {
                    loadingState(false)
                    binding.wikiRv.adapter =
                        state.data.query?.pages?.let { WikiAdapter(it) { it1 -> openBrowser(it1 as String) } }
                }
                is State.Error -> {
                    loadingState(false)
                    binding.noDataState.visibility = VISIBLE
                }
            }
        })
    }

    private fun openBrowser(string: String) = startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/$string"))
    )


    private fun loadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingState.visibility = VISIBLE
            binding.defaultState.visibility = GONE
            binding.noDataState.visibility = GONE
        } else {
            Helper.hideKeyboard(this)
            binding.loadingState.visibility = GONE
        }
    }

    fun callSearchApi(searchString: String) {
        if (Helper.isNetworkAvailable(this))
            viewModel.getWikiData(searchString)
        else
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

}
