package com.coinz.michaelabrezinova.coinz

import android.location.Location
import android.util.Log
import android.support.v4.app.NavUtils
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
import org.json.JSONObject
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_maps.openWallet
import kotlinx.android.synthetic.main.content_maps.view.*
import org.xml.sax.Parser


class MapsActivity : AppCompatActivity(), PermissionsListener, OnMapReadyCallback,LocationEngineListener, MapboxMap.OnMapClickListener  {

    private val tag= "MapsActivity"

    private var mapView: MapView? = null
    private var map: MapboxMap? = null

    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location

    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        openWallet?.setOnClickListener {
            val intent = Intent(applicationContext, WalletActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap ==null){
            Log.d(tag,"[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            displayCoins()
        }
    }
    fun enableLocation() {
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

    fun displayCoins(){

        var vectorDrawable: Drawable = ResourcesCompat.getDrawable(resources,R.drawable.ic_marker,null)!!
        var bitmap: Bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())

        for(feature in MainActivity.features){
            var p =feature.geometry() as Point
            var props = feature.properties()
            var c = props?.get("marker-color").toString()
            if (c.contains("ffdf00")){
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#292e1e"))
            } else if (c.contains("#008000")) {
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#e8d17f"))
            } else if (c.contains("0000ff")) {
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#66cc81"))
            } else {
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#ff7575"))
            }
            vectorDrawable.draw(canvas)
            var icon: Icon  = IconFactory.getInstance(this).fromBitmap(bitmap)
            var latlng = LatLng(p.coordinates()[1],p.coordinates()[0])
            MainActivity.markers.add(map?.addMarker(MarkerOptions()
                    .title(feature.id())
                    .position(latlng)
                    .snippet(props?.get("currency").toString())
                    .icon(icon)
            )!!)
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
            // Open a dialogue with the user
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
        // destinationMarker = map?.addMarker(MarkerOptions().position(LatLng(55.94327575639263, -3.18686952803462)))
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag,"[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non´null) and update UI
        //updateUI(mAuth?.currentUser)

        //if (PermissionsManager.areLocationPermissionsGranted(this)) {
        //    locationEngine.requestLocationUpdates()
        //    locationLayerPlugin.onStart()
        //}

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
}
