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
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import com.mapbox.geojson.Point
import java.util.Date
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.mapboxsdk.style.layers.BackgroundLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import kotlinx.android.synthetic.main.activity_maps.openWalletButton
import kotlinx.android.synthetic.main.activity_maps.changeThemeButton
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), PermissionsListener, View.OnClickListener,
        OnMapReadyCallback,LocationEngineListener, MapboxMap.OnMarkerClickListener  {

    private val tag= "MapsActivity"

    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var userReference: DocumentReference? = null

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var layer: Layer? = null
    private var markers: ArrayList<Marker>? = null

    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    private val preferencesFile = "MyPrefsFile"
    private lateinit var currentDate: String
    private var darkTheme: Boolean = false

    companion object {
        var features = ArrayList<Feature>()
        var rates  = JSONObject()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = MainActivity.currentUser

        //Initialize fireStore
        firestore = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_maps)
        openWalletButton?.setOnClickListener(this)
        changeThemeButton?.setOnClickListener(this)

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

            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            map?.setOnMarkerClickListener(this)
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
            Toast.makeText(
                    baseContext, "Please, grant the permission to use the location, " +
                    "otherwise, you will not be able to enjoy the game fully!",
                    Toast.LENGTH_SHORT).show()
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

    override fun onMarkerClick( marker: Marker): Boolean {
        Log.d(tag,"[onMarkerClick] marker $marker has been clicked")
        var markerLocation: Location? = Location("")
        markerLocation?.longitude = marker.position.longitude
        markerLocation?.latitude = marker.position.latitude

        if(originLocation.distanceTo(markerLocation)<=25) {

            map?.removeMarker(marker)
            markers?.remove(marker)
            MainActivity.user?.collectedIds?.add(marker.title)
            MainActivity.user?.dailyCollected =
                    MainActivity.user?.dailyCollected?.plus(1)!!

            var value = marker.snippet.toDouble().roundToInt()
            var countCollected = findViewById<TextView>(R.id.CountCollected)
            countCollected.text = Integer.toString(MainActivity.user?.dailyCollected!!)

            if(MainActivity.user?.dailyCollected!!<=25){
                MainActivity.user!!.collectedBankable =
                        MainActivity.user!!.collectedBankable.plus(value)
            } else {
                MainActivity.user!!.collectedSpareChange =
                        MainActivity.user!!.collectedSpareChange.plus(value)
            }
            updateUser()
        }
        return true
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
        userReference = firestore?.collection("users")
                ?.document(MainActivity.currentUser!!.email!!)

        if(MainActivity.downloadDate!=currentDate){
            downloadCoins()
        } else {
            val obj = JSONObject(MainActivity.downloadedGJson)
            rates = obj.getJSONObject("rates")
            val fc = FeatureCollection.fromJson(MainActivity.downloadedGJson)
            if(fc!=null) {
                features = fc.features()!!.toCollection(ArrayList())
            }
        }

        realTimeUpdateListener()
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

        //Save last download date and downloaded geoJson
        val editor =
                getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).edit()
        editor.putString("lastDownloadDate", MainActivity.downloadDate)
        editor.putString("lastDownloadedGJson", MainActivity.downloadedGJson)
        editor.apply()

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

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.openWalletButton -> startActivity(Intent(applicationContext, WalletActivity::class.java))
            R.id.changeThemeButton ->  changeTheme()
        }
    }

    private fun changeTheme(){
        if(!darkTheme) {
            map?.setStyleUrl("mapbox://styles/michaelabrezinova/cjpu3m5ku5xgz2rpbg5pyq322")
            darkTheme = true
        } else {
            map?.setStyleUrl("mapbox://styles/michaelabrezinova/cjpu4wj8w013b2spc4zcugc4z")
            darkTheme = false
        }
    }
    private fun displayCoins(){
        Log.d(tag, "Displaying of coins has started")

        //looping through features and displaying the coins on the map, setting proper color,
        //location, title and snippet
        if(!features.isEmpty()) {
            Log.d(tag, "[features] are non-empty and started to be displayed")
            for (feature in features) {
                val properties = feature.properties()
                if (!MainActivity.user?.collectedIds?.contains(properties?.get("id").toString())!!){

                    val point = feature.geometry() as Point
                    var currency= properties?.get("currency").toString()
                    currency =currency.substring(1, currency.length -1)
                    var value = properties?.get("value").toString()
                    value = value.substring(1, value.length -1)
                    val icon: Icon = getIcon(currency)
                    val goldValue: String = getGoldValue(currency,value)
                    val latlng = LatLng(point.coordinates()[1], point.coordinates()[0])


                    //Create marker, add marker to map and to ArrayList of markers
                    val marker = map?.addMarker(MarkerOptions()
                            .snippet(goldValue)
                            .position(latlng)
                            .title(properties?.get("id").toString())
                            .icon(icon))
                    markers?.add(marker!!)
                }
            }
        }

    }

    //Initialized download of the coins that is done in the DownloadFileTask
    private fun downloadCoins() {
        //updates last download date
        MainActivity.downloadDate = currentDate

        val listener = DownloadCompleteRunner
        val downloader = DownloadFileTask(listener)
        val path = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
        downloader.execute(path)

        Log.d(tag,"Download of the coins has been executed")
    }

    //returns icon with color depending on the color it had in the geoJson file
    private fun getIcon(currency: String ): Icon {
        var vectorDrawable: Drawable = ResourcesCompat.getDrawable(resources,R.drawable.ic_marker,
                null)!!
        var bitmap: Bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)

        when {
            currency=="SHIL" ->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#292e1e"))
            currency=="PENY"->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#e8d17f"))
            currency=="QUID" ->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#66cc81"))
            else -> DrawableCompat.setTint(vectorDrawable, Color.parseColor("#ff7575"))
        }

        vectorDrawable.draw(canvas)
        return IconFactory.getInstance(this).fromBitmap(bitmap)

    }

    private fun getGoldValue(currency: String , value: String): String {
        var valueInt = value.toDouble()
        var exchangerate = rates.get(currency).toString().toDouble()
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return (df.format(valueInt.times(exchangerate)))
    }

    private fun updateUser() {
        userReference?.update(
                "lastDateSignedIn", currentDate,
                "dailyCollected", MainActivity.user?.dailyCollected,
                "dailyDistanceWalked", MainActivity.user?.dailyDistanceWalked,
                "dailyScore", MainActivity.user?.dailyCollected,
                "collectedBankable", MainActivity.user?.collectedBankable,
                "collectedSpareChange", MainActivity.user?.collectedSpareChange,
                "collectedIds", MainActivity.user?.collectedIds,
                "collectedGift", MainActivity.user?.collectedGift,
                "overallScore", MainActivity.user?.overallScore)
    }

    private fun realTimeUpdateListener() {
        userReference?.addSnapshotListener{ documentSnapshot, e ->
            when {
                e != null -> Log.e(tag, e.message)
                documentSnapshot != null && documentSnapshot.exists() -> {
                    with(documentSnapshot) {
                        val collected = data?.get("collectedGift").toString().toInt()
                        if(MainActivity.user?.collectedGift!! <collected){
                            Toast.makeText(
                                    baseContext, "You have received a gift from your friend!",
                                    Toast.LENGTH_SHORT).show()
                        }
                        MainActivity.user?.collectedGift = collected
                    }
                }
            }
        }
    }
}