package com.crp.wikiAppNew

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.crp.wikiAppNew.network.WikiAPI
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import com.crp.wikiAppNew.BuildConfig


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private var contNotificacion =2
    private val wikiAPI: WikiAPI by inject()
    private var lastKnownPlace: String? = null
    private var lastNotifiedPlace: String? = null

    override fun onCreate() {

        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Places.initialize(applicationContext, BuildConfig.GOOGLE_API_KEY)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        this.startForeground(1, createNotification("Service running"))

        requestLocationUpdates()
    }



//    private fun createNotificationChannel() {
//
//        val serviceChannel = NotificationChannel(
//            "locationServiceChannel",
//            "Location Service Channel",
//            NotificationManager.IMPORTANCE_HIGH
//        )
//        notificationManager.createNotificationChannel(serviceChannel)
//
//    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "locationServiceChannel",
            "Location Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Location Service"
        }

        notificationManager.createNotificationChannel(serviceChannel)
    }


    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Location Service")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun requestLocationUpdates() {


        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30000
        ).apply {
            setMinUpdateIntervalMillis(10000)
        }.build()


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                getPlaceName(location.latitude, location.longitude)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getPlaceName(latitude: Double, longitude: Double) {
        // val placeFields: List<Place.Field> = listOf(Place.Field.NAME)
        val placeFields: List<Place.Field> = listOf(
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES
        )

        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        val placesClient: PlacesClient = Places.createClient(this)


        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                val topPlace = response.placeLikelihoods
                    .maxByOrNull { it.likelihood } // Get the most likely place

                topPlace?.let { placeLikelihood ->
                    val currentPlace = placeLikelihood.place.name
                    if (currentPlace != lastKnownPlace) {

                        lastKnownPlace = currentPlace


                        // Llamar a la API de Wikipedia (usando tu implementación)
                        CoroutineScope(Dispatchers.IO).launch {
                            val wikiResponse = wikiAPI.getSearchRespone(
                                currentPlace, // searchStr
                                "query",
                                "2",
                                "extracts|pageimages|pageterms",
                                "2",
                                "true",
                                "thumbnail",
                                "json",
                                "prefixsearch",
                                "300",
                                "true"


                            )

                            withContext(Dispatchers.Main) {
                                if (wikiResponse.query?.pages?.isNotEmpty() == true) {
                                    // Hay resultados, enviar notificación
                                    val pageList = wikiResponse.query.pages.toList()
                                    val firstPage = if (pageList.isNotEmpty()) pageList[0] else null
                                    val articleTitle = firstPage?.title
                                    val coordinates = placeLikelihood.place.latLng
                                    // val notificationMessage = "Nuevo artículo disponible en Wikipedia sobre ${firstPage?.title}: ${firstPage?.extract}"
                                    if (articleTitle != null) {
                                        sendNotification(currentPlace, articleTitle, coordinates)
                                    }
                                } else {
                                    // No hay resultados, no enviar notificación
                                    Log.d(TAG, "No se encontraron resultados para $currentPlace")
                                }
                            }
                        }
                    }
                }
            } else {
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e(TAG, "Lugar no encontrado: ${exception.statusCode}")
                }
            }
        }
    }

//    private fun sendNotification(message: String) {
//        contNotificacion++
//        val notification = NotificationCompat.Builder(this, "locationServiceChannel")
//            .setContentTitle("Lugar encontrado")
//            .setContentText(message).setSmallIcon(R.mipmap.ic_launcher)
//            .build()
//        notificationManager.notify(contNotificacion, notification)
//    }


    private fun sendNotification(placeName: String, articleTitle: String, coordinates: LatLng) {
        contNotificacion++

        val formattedCoordinates = String.format("%.6f, %.6f", coordinates.latitude, coordinates.longitude)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://en.wikipedia.org/wiki/$articleTitle")
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Artículo encontrado")
            .setContentText("Articulo: $articleTitle " +
                    "\nLugar: $placeName " +
                    "\nCoordenadas: $coordinates")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_view,
                    "Mostrar",
                    pendingIntent
                )
            )
            .build()

        // Only send notification if the place has not been notified before
        if (placeName != lastNotifiedPlace) {
            notificationManager.notify(contNotificacion, notification)
            lastNotifiedPlace = placeName
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}
