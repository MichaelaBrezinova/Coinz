package com.coinz.michaelabrezinova.coinz

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.location.Location
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
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_maps.openWalletButton
import kotlinx.android.synthetic.main.activity_maps.changeThemeButton
import org.json.JSONObject
import timber.log.Timber
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), PermissionsListener, View.OnClickListener,
        OnMapReadyCallback,LocationEngineListener, MapboxMap.OnMarkerClickListener  {

    private val tag= "MapsActivity"

    // User information and reference
    private var user: FirebaseUser? = null
    private var fireStore: FirebaseFirestore? = null
    private var userReference: DocumentReference? = null

    // Map and markers
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var markers: ArrayList<Marker>? = null

    // Location updates information
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    // Local information
    private val preferencesFile = "MyPrefsFile"
    private lateinit var downloadDate: String
    private var darkTheme: Boolean = false

    companion object {
        var features = ArrayList<Feature>()
        var rates  = JSONObject()
        var downloadedGJson = ""
        var currentDate = ""
        var currentHour = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initialize user and fireStore
        user = MainActivity.currentUser
        fireStore = FirebaseFirestore.getInstance()

        setContentView(R.layout.activity_maps)

        //Set listeners for the buttons used
        openWalletButton?.setOnClickListener(this)
        changeThemeButton?.setOnClickListener(this)

        //Initialize map
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

    }

    //If map is ready and non-null set up its settings, enable location and display the coins
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap ==null){
            Timber.tag(tag).d("[onMapReady] mapboxMap is null")
        } else {
            Timber.tag(tag).d("[onMapReady] map has been set")
            map = mapboxMap
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            displayCoins()
            map?.setOnMarkerClickListener(this)
        }
    }

    //Enable location depending on permissions, initialize location engine and layer if permission
    //granted
    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            Timber.tag(tag).d("Permissions are granted")
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            Timber.tag(tag).d("Permissions are not granted")
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 //  every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        //Set up location to be the last location
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    //Location layer is initialized and properly set up
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        if (mapView == null) { Timber.tag(tag).d("mapView is null") }
        else {
            if (map == null) { Timber.tag(tag).d("map is null") }
            else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!,
                        map!!, locationEngine)
                locationLayerPlugin.apply {
                    isLocationLayerEnabled = true
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    //Camera position set depending on the location
    private fun setCameraPosition(location: Location) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude,location.longitude),13.0))
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //Dialog why access needed
        Timber.tag(tag).d( "Permissions: $permissionsToExplain")
        // Present popup message or dialog
        //TODO: add popup
    }

    //Check if the permissions are granted, if not, display toast and disable clicking on the map
    override fun onPermissionResult(granted: Boolean) {
        Timber.tag(tag).d( "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            //Display toast explaining why permission to use location is needed
            Toast.makeText(
                    baseContext, "Please, grant the permission to use the location, " +
                    "otherwise, you will not be able to enjoy the game fully!",
                    Toast.LENGTH_SHORT).show()
            mapView?.isClickable = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //If location has changed, changes position on the map and camera pointing
    override fun onLocationChanged(location: Location?) {
        if(location ==null) {
            Timber.tag(tag).d("[onLocationChanged] location is null")
        } else {
            Timber.tag(tag).d("[onLocationChanged] location has changed")
            originLocation = location
            setCameraPosition(originLocation)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Timber.tag(tag).d("[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    @SuppressLint("ShortAlarm")
    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        /*if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine.requestLocationUpdates()
            locationLayerPlugin.onStart()
        }*/

        //Current date is initialized
        currentDate = MainActivity.sdf?.format(Date())?.substring(0,10)!!
        currentHour = MainActivity.sdf?.format(Date())?.substring(11,13)!!

        //Get download date and geoJSON file stored in the shared preferences
        val downloadInformation =
                getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = downloadInformation.getString("lastDownloadDate", "")!!
        downloadedGJson = downloadInformation.getString("lastDownloadedGJson", "")!!
        Timber.tag(tag).d(String.format("[onStart] Recalled lastDownloadDate is " +
                "’$downloadDate’"))
        Timber.tag(tag).d( String.format("[onStart] Recalled lastDownloadedGJson is " +
                "’$downloadedGJson"))

        //FireStore settings are applied
        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        fireStore?.firestoreSettings = settingsFireBase

        //Reference to the user account(fireStore document) is initialized
        userReference = fireStore?.collection("users")
                ?.document(MainActivity.currentUser!!.email!!)

        //If the download date of coins is outdated or there are no coins downloaded,
        //download the coins, otherwise use data stored in shared preferences
        if(downloadDate!=currentDate || downloadedGJson ==""){
            downloadCoins()
        } else {
            val obj = JSONObject(downloadedGJson)
            rates = obj.getJSONObject("rates")
            val fc = FeatureCollection.fromJson(downloadedGJson)
            if(fc!=null) {
                features = fc.features()!!.toCollection(ArrayList())
            }
        }

        //Set up alarm to check the time and date
        val repeatTime = 600 //Repeat time 10 minutes
        val processTimer: AlarmManager =
                applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, ProcessTimeReceiver::class.java)
        val pendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                        this, 0,  intent, PendingIntent.FLAG_UPDATE_CURRENT)
        //Repeat alarm every 10 seconds
        processTimer.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(), (repeatTime*10000).toLong(), pendingIntent)

        //Start the update listener for updates from fireStore
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

        Timber.tag(tag).d(String.format("[onStop] Storing lastDownloadDate of " +
                "’$downloadDate’"))
        Timber.tag(tag).d( String.format("[onStop] Storing lastDownloadedGJson of " +
                "’$downloadedGJson"))

        //Save last download date and downloaded geoJSON to shared preferences
        val editor =
                getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("lastDownloadedGJson", downloadedGJson)
        editor.apply()

        //Cancel the alarm for checking the time
        val intent = Intent(this, ProcessTimeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, 0)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

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

    //Set up methods clicked on the buttons on the screen
    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.openWalletButton ->
                startActivity(Intent(applicationContext, WalletActivity::class.java))
            R.id.changeThemeButton ->  changeTheme()
        }
    }

    //Handles situation if the marker is clicked
    override fun onMarkerClick( marker: Marker): Boolean {
        Timber.tag(tag).d("[onMarkerClick]  $marker has been clicked")

        //Get marker's location
        val markerLocation: Location? = Location("")
        markerLocation?.longitude = marker.position.longitude
        markerLocation?.latitude = marker.position.latitude

        //Finds distance between user and marker, if less than 25m allows the user to pick
        //marker up
        try {
            if(originLocation.distanceTo(markerLocation)<=25) {

                //Marker removed
                map?.removeMarker(marker)
                markers?.remove(marker)

                //Marker ID is added to user's array of collected markers(coins), number of user's
                //daily collected coins is incremented and number in the indicator of collected
                //coins is incremented as well
                MainActivity.user?.collectedIds?.add(marker.title)
                MainActivity.user?.dailyCollected =
                     MainActivity.user?.dailyCollected?.plus(1)!!
                val countCollected = findViewById<TextView>(R.id.CountCollected)
                countCollected.text = MainActivity.user?.dailyCollected!!.toString()

                //Value of the coin is extracted and added to user's variable storing collected
                //bankable coins(if number of daily collected is <=25) or collected spare change
                //(if the number of collected coins is >25)
                val value = marker.snippet.toDouble().roundToInt()
                Timber.tag(tag).d("[onMarkerClick]  $marker value is $value")
                if(MainActivity.user?.dailyCollected!!<=25){
                    MainActivity.user!!.collectedBankable =
                        MainActivity.user!!.collectedBankable.plus(value)
                } else {
                    MainActivity.user!!.collectedSpareChange =
                        MainActivity.user!!.collectedSpareChange.plus(value)
                }

                //Inform the user that they successfully picked up the coin
                Toast.makeText(
                        baseContext, "You picked up the coin with value $value gold! ",
                        Toast.LENGTH_SHORT).show()

                //User information are updated in fireStore
                updateUser()
            } else {

                //Inform the user that they are too far away from the coin to pick it up
                Toast.makeText(
                        baseContext, "The coin is far away from you! Try to get closer",
                        Toast.LENGTH_SHORT).show()
            }
        } catch(e: Exception){
            Timber.tag(tag).e("[onMarkerClick] Failed to pick up with $e")

            //Inform user that they were not able to pick up the coin
            Toast.makeText(
                    baseContext, "Unable to pick up the coin! Check your location settings.",
                    Toast.LENGTH_SHORT).show()
        }
        return true
    }

    //Change map style from light to dark and dark to like depending on the current theme
    private fun changeTheme(){
        darkTheme = if(!darkTheme) {
            map?.setStyleUrl("mapbox://styles/michaelabrezinova/cjpu3m5ku5xgz2rpbg5pyq322")
            true
        } else {
            map?.setStyleUrl("mapbox://styles/michaelabrezinova/cjpu4wj8w013b2spc4zcugc4z")
            false
        }
    }

    //Display the coins
    private fun displayCoins(){
        Timber.tag(tag).d( "Displaying of coins is called")

        //Looping through features and displaying the coins on the map, setting proper color, icon
        //location, title and snippet
        if(!features.isEmpty()) {
            Timber.tag(tag).d( "[features] are non-empty and started to be displayed")
            val size = features.size

            val partDayFeatures = if (currentHour<"12"){
                Timber.tag(tag).d( "[displayCoins] is before 12, first half displayed")
                features.subList(0,(size+1)/2)
            } else {
                Timber.tag(tag).d( "[displayCoins] is after 12, second half displayed")
                features.subList((size+1)/2,size)
            }
            for (feature in partDayFeatures) {
                //checks if the user has already picked up the coin, if yes, doesn't display it
                val properties = feature.properties()
                if (!MainActivity.user?.collectedIds
                                ?.contains(properties?.get("id").toString())!!){

                    //Get marker(coin) position
                    val point = feature.geometry() as Point
                    val latlng = LatLng(point.coordinates()[1], point.coordinates()[0])

                    //Get currency and value
                    var currency= properties?.get("currency").toString()
                    currency =currency.substring(1, currency.length -1)
                    var value = properties?.get("value").toString()
                    value = value.substring(1, value.length -1)
                    val goldValue = getGoldValue(currency,value)

                    //Get icon depending on the currency
                    val icon: Icon = getIcon(currency)

                    //Create marker, add marker to map and to list of markers
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
        Timber.tag(tag).d("Download of the coins has been executed")

        //Updates last download date
        downloadDate = currentDate

        //Download
        val listener = DownloadCompleteRunner
        val downloader = DownloadFileTask(listener)
        val path = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
        downloader.execute(path)
    }

    //Returns icon with appropriate color depending of the currency of the coin, programmatically
    //changes the marker color
    private fun getIcon(currency: String ): Icon {
        Timber.tag(tag).d(" [getIcon] Setting appropriate icon color has been started")
        val vectorDrawable: Drawable =
                ResourcesCompat.getDrawable(resources,R.drawable.ic_marker, null)!!
        val bitmap: Bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)

        //Set color depending on the currency
        when (currency) {
            "SHIL" ->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#292e1e"))
            "PENY" ->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#e8d17f"))
            "QUID" ->
                DrawableCompat.setTint(vectorDrawable, Color.parseColor("#66cc81"))
            else -> DrawableCompat.setTint(vectorDrawable, Color.parseColor("#ff7575"))
        }
        vectorDrawable.draw(canvas)

        //Return icon with the right color
        return IconFactory.getInstance(this).fromBitmap(bitmap)

    }

    //Get value of the coin in gold currency
    private fun getGoldValue(currency: String , value: String): String {
        Timber.tag(tag).d("[getGoldValue] Exchanging coin value to gold currency")

        val valueInt = value.toDouble()
        val exchangeRate = rates.get(currency).toString().toDouble()
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING

        //Return value of the coin in gold currency
        return (df.format(valueInt.times(exchangeRate)))
    }

    //Updates user information to stay synchronized with the fireStore
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

    //Real time updates from the fireBase, synchronizes the local variable holding the gift money
    //amount and creates toast notifying user they have received money from a friend
    private fun realTimeUpdateListener() {
        userReference?.addSnapshotListener{ documentSnapshot, e ->
            when {
                e != null -> Timber.tag(tag).e( e.message)
                documentSnapshot != null && documentSnapshot.exists() -> {
                    with(documentSnapshot) {
                        val collectedGift = data?.get("collectedGift").toString().toInt()
                        if(MainActivity.user?.collectedGift!! <collectedGift){
                            Toast.makeText(
                                    baseContext, "You have received a gift from your friend!",
                                    Toast.LENGTH_SHORT).show()
                        }
                        MainActivity.user?.collectedGift = collectedGift
                    }
                }
            }
        }
    }
}