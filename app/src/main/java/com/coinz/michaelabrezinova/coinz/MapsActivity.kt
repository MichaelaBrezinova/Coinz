package com.coinz.michaelabrezinova.coinz

import android.location.Location
import android.util.Log
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.graphics.Canvas
import android.support.v7.app.AppCompatActivity
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.content.res.ResourcesCompat
import com.mapbox.android.core.location.LocationEngine
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import java.text.SimpleDateFormat
import android.content.Context
import com.google.gson.JsonObject
import android.view.View
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import java.util.Date
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_maps.openWallet
import kotlinx.android.synthetic.main.activity_maps.changeTheme
import kotlinx.android.synthetic.main.activity_maps.CountCollected
import kotlinx.android.synthetic.main.content_maps.view.*
import org.xml.sax.Parser


class MapsActivity : AppCompatActivity(), PermissionsListener, View.OnClickListener,
        OnMapReadyCallback,LocationEngineListener, MapboxMap.OnMapClickListener  {

    private val tag= "MapsActivity"

    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var userReference: DocumentReference? = null
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var layer: Layer? = null

    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location

    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    private lateinit var currentDate: String

    companion object {
        var features = ArrayList<Feature>()
        var features_testing: FeatureCollection? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = MainActivity.currentUser

        //Initialize fireStore
        firestore = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_maps)
        openWallet?.setOnClickListener(this)
        changeTheme?.setOnClickListener(this)

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap ==null){
            Log.d(tag,"[onMapReady] mapboxMap is null")
        } else {
            Log.d(tag,"[onMapReady] map has been set")
            map = mapboxMap
            layer = map?.getLayer("background")
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            displayCoins()
        }

    }

    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    private fun displayCoins(){
        Log.d(tag, "Displaying of coins has started")
        //setting up DrawableCompat to change colors of the markers
        var vectorDrawable: Drawable = ResourcesCompat.getDrawable(resources,R.drawable.ic_marker,
                null)!!
        var bitmap: Bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)


        //looping through features and displaying the coins on the map, setting proper color,
        //location, title and snippet
        //TODO: fix snippet
        if(!features.isEmpty()) {
            Log.d(tag, "[features] are non-empty and started to be displayed")
            for (feature in features) {

                val p = feature.geometry() as Point
                val props = feature.properties()
                val c = props?.get("marker-color").toString()

                //sets color depending on the color stated in the JSON
                if (c.contains("ffdf00")) {
                    DrawableCompat.setTint(vectorDrawable, Color.parseColor("#292e1e"))
                } else if (c.contains("#008000")) {
                    DrawableCompat.setTint(vectorDrawable, Color.parseColor("#e8d17f"))
                } else if (c.contains("0000ff")) {
                    DrawableCompat.setTint(vectorDrawable, Color.parseColor("#66cc81"))
                } else {
                    DrawableCompat.setTint(vectorDrawable, Color.parseColor("#ff7575"))
                }

                vectorDrawable.draw(canvas)
                var icon: Icon = IconFactory.getInstance(this).fromBitmap(bitmap)

                var latlng = LatLng(p.coordinates()[1], p.coordinates()[0])

                //adds marker to map
                map?.addMarker(MarkerOptions()
                        .title(feature.id())
                        .position(latlng)
                        .snippet(props?.get("currency").toString())
                        .icon(icon))
            }
        }

    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        var lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)

        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
        locationLayerPlugin.setLocationLayerEnabled(true)
        locationLayerPlugin.cameraMode = CameraMode.TRACKING
        locationLayerPlugin.renderMode = RenderMode.NORMAL
    }

    private fun setCameraPosition(location: Location) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude,location.longitude),13.0))
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //Dialog why access needed
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialog
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            //TODO:add dialog to onPermissionResult
            // Open a dialogue with the user
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onLocationChanged(location: Location?) {
        if(location ==null) {
            Log.d(tag,"[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(originLocation)
        }
    }

    override fun onMapClick(point: LatLng){
        println("Works")
        // destinationMarker = map?.addMarker(MarkerOptions()
        // .position(LatLng(55.94327575639263, -3.18686952803462)))
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag,"[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        //if (PermissionsManager.areLocationPermissionsGranted(this)) {
        //    locationEngine.requestLocationUpdates()
        //    locationLayerPlugin.onStart()
        //}

        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
        currentDate = sdf.format(Date()).substring(0,10)

        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settingsFireBase

        userReference = firestore?.collection("users")?.document(user!!.email!!)
        userReference?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(tag, "DocumentSnapshot data: " + document.data)
                        val lastDateSignedIn = document.data!!["lastDateSignedIn"].toString()
                        val overallScore = document.data!!["overallScore"] as Int
                        if(lastDateSignedIn!=currentDate) {
                            resetUser(overallScore)
                        } else {
                            //MainActivity.user?.collectedIds =""
                            MainActivity.user?.collectedBankable =
                                    document.data!!["collectedBankable"] as Int
                            MainActivity.user?.collectedSpareChange =
                                    document.data!!["collectedSpareChange"] as Int
                            MainActivity.user?.dailyCollected=
                                    document.data!!["dailyCollected"] as Int
                            MainActivity.user?.dailyDistanceWalked =
                                    document.data!!["dailyDistanceWalked"] as Int
                            MainActivity.user?.dailyScore =
                                    document.data!!["dailyScore"] as Int
                            MainActivity.user?.overallScore = overallScore
                        }
                    } else {
                        Log.d(tag, "No such document")
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "get failed with ", exception)
                }

        downloadCoins()
        //realTimeUpdateListener()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()

        userReference?.update(
                "lastDateSignedIn", currentDate,
                "dailyCollected",MainActivity.user?.dailyCollected,
                "dailyDistanceWalked",MainActivity.user?.dailyDistanceWalked,
                "dailyScore",MainActivity.user?.dailyScore,
                "collectedBankable",MainActivity.user?.collectedBankable,
                "collectedSpareChange",15,
                "collectedIds", MainActivity.user?.collectedIds)

        locationEngine.removeLocationUpdates()
        locationLayerPlugin.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        locationEngine.deactivate()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    //TODO: Change naming of the buttons
    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.openWallet -> startActivity(Intent(applicationContext, WalletActivity::class.java))
            //R.id.changeTheme ->  mapView?.setStyleUrl("@string/mapbox_style_dark")
        }
    }

    private fun downloadCoins() {
        MainActivity.downloadDate = currentDate
        val listener = DownloadCompleteRunner
        val downloader = DownloadFileTask(listener)
        val path = "http://homepages.inf.ed.ac.uk/stg/coinz/" + currentDate + "/coinzmap.geojson"
        downloader.execute(path)
        Log.d(tag,"Download of the coins has been executed")
    }

    private fun resetUser( overallScore: Int) {
        MainActivity.user?.collectedIds =""
        MainActivity.user?.collectedBankable =0
        MainActivity.user?.collectedSpareChange = 0
        MainActivity.user?.dailyCollected= 0
        MainActivity.user?.dailyDistanceWalked = 0
        MainActivity.user?.dailyScore = 0
        MainActivity.user?.overallScore = overallScore
    }

    /*private fun realtimeUpdateListener() {
        firestore?.collection("users")
                ?.document(user!!.email!!)?.addSnapshotListener{ documentSnapshot, e ->
            when {
                e != null -> Log.e(tag, e.message)
                documentSnapshot != null && documentSnapshot.exists() -> {
                with(documentSnapshot) {
                    val collected = "${data?.get("dailyCollected")}"
                    incoming message text.text = incoming
                }
            }
            }
        }
    }*/
}