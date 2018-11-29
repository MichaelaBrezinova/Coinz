package com.coinz.michaelabrezinova.coinz

import android.content.Context
import android.os.Bundle
import com.mapbox.geojson.FeatureCollection
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.mapbox.mapboxsdk.annotations.Marker
import android.text.TextUtils
import com.google.gson.Gson
import org.json.JSONObject
import android.location.Address
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.coinz.michaelabrezinova.coinz.R
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Feature
//import kotlinx.android.synthetic.main.activity_emailpassword.detail
//import kotlinx.android.synthetic.main.activity_emailpassword.emailCreateAccountButton
//import kotlinx.android.synthetic.main.activity_emailpassword.emailPasswordButtons
//import kotlinx.android.synthetic.main.activity_emailpassword.emailPasswordFields
//import kotlinx.android.synthetic.main.activity_emailpassword.emailSignInButton
import kotlinx.android.synthetic.main.activity_main.fieldEmail
import kotlinx.android.synthetic.main.activity_main.fieldPassword
import kotlinx.android.synthetic.main.activity_main.sign_in
import kotlinx.android.synthetic.main.activity_main.create_account
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

//import kotlinx.android.synthetic.main.activity_emailpassword.status
//import kotlinx.android.synthetic.main.activity_emailpassword.verifyEmailButton

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val tag= "MainActivity"
    // [START declare_auth]
    private lateinit var auth: FirebaseAuth

    private var downloadDate = ""
    private val preferencesFile = "MyPrefsFile"

    // [END declare_auth]

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Buttons
        sign_in.setOnClickListener(this)
        create_account.setOnClickListener(this)
        //signOutButton.setOnClickListener(this)
        //verifyEmailButton.setOnClickListener(this)

        // [START initialize_auth]
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // [END initialize_auth]
    }

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser

        if( auth.currentUser!=null) {
            updateUI(currentUser)
        }

        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        downloadDate = settings.getString("lastDownloadDate", "")
        //val json: String = settings.getString("markers",null)!!
        Log.d(tag, "[onStart] Recalled lastDownloadDate is ’$downloadDate’")

        //val gson = Gson()
        //val turnsType = object : TypeToken<ArrayList<Marker>>() {}.type
        //markers = gson.fromJson<ArrayList<Marker>>(json, turnsType)
       // features = something as ArrayList<Feature>

        //if (PermissionsManager.areLocationPermissionsGranted(this)) {
        //    locationEngine.requestLocationUpdates()
        //    locationLayerPlugin.onStart()
        //}
        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
        val currentDate = sdf.format(Date()).substring(0,10)

        //if(downloadDate!=currentDate) {
            downloadCoins()
        //}
    }
    // [END on_start_check_user]

    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        if (!validateForm()) {
            return
        }

        //showProgressDialog()

        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "E-mail does not exist or is already used.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    // [START_EXCLUDE]
                   // hideProgressDialog()
                    // [END_EXCLUDE]
                }
        // [END create_user_with_email]
    }

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm()) {
            return
        }

       // showProgressDialog()

        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Your e-mail or password is incorrect.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }

                    // [START_EXCLUDE]
                    //if (!task.isSuccessful) {
                     //   status.setText(R.string.auth_failed)
                   // }
                   // hideProgressDialog()
                    // [END_EXCLUDE]
                }
        // [END sign_in_with_email]
    }

    private fun signOut() {
        auth.signOut()
        updateUI(null)
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            fieldEmail.error = "Required."
            valid = false
        } else {
            fieldEmail.error = null
        }

        val password = fieldPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            fieldPassword.error = "Required."
            valid = false
        } else {
            fieldPassword.error = null
        }

        return valid
    }

    private fun updateUI(user: FirebaseUser?) {
        //hideProgressDialog()
        if (user != null) {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.create_account -> createAccount(fieldEmail.text.toString(), fieldPassword.text.toString())
            R.id.sign_in -> signIn(fieldEmail.text.toString(), fieldPassword.text.toString())
           // R.id.signOutButton -> signOut()
           // R.id.verifyEmailButton -> sendEmailVerification()
        }
    }

    override fun onStop() {
        super.onStop()

        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        Log.d(tag, "[onStop] Storing lastfeatures of $markers")

        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        val editor = settings.edit()
        val gson = Gson()
        val json: String = gson.toJson(markers)
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("markers",json)
        editor.apply()

    }

    private fun downloadCoins() {
        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
        downloadDate = sdf.format(Date()).substring(0,10)
        val listener = DownloadCompleteRunner
        val downloader = DownloadFileTask(listener)
        val path = "http://homepages.inf.ed.ac.uk/stg/coinz/" + downloadDate + "/coinzmap.geojson"
        downloader.execute(path)
    }

    companion object {
        private const val TAG = "EmailPassword"
        var features = ArrayList<Feature>()
        var markers = ArrayList<Marker>()
    }
}