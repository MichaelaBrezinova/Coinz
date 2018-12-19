package com.coinz.michaelabrezinova.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.geojson.Feature
import kotlinx.android.synthetic.main.activity_main.fieldEmail
import kotlinx.android.synthetic.main.activity_main.fieldPassword
import kotlinx.android.synthetic.main.activity_main.signInButton
import kotlinx.android.synthetic.main.activity_main.createAccountButton
import kotlinx.android.synthetic.main.activity_main.loadingPanel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val tag= "LoginActivity"

    companion object {
        private const val TAG = "EmailPassword"
        var downloadDate =""
        var downloadedGJson = ""
        var currentUser: FirebaseUser? = null
        var user: User? = null
    }

    //Declare Authorization and fireStore
    private var userReference: DocumentReference? = null
    private lateinit var auth: FirebaseAuth
    private var fireStore: FirebaseFirestore? = null
    private val preferencesFile = "MyPrefsFile"
    private lateinit var currentDate: String


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize fireStore
        fireStore = FirebaseFirestore.getInstance()

        // Set on click listeners for buttons
        signInButton.setOnClickListener(this)
        createAccountButton.setOnClickListener(this)
        //signOutButton.setOnClickListener(this)

        // Initialize FireBase Auth
        auth = FirebaseAuth.getInstance()
    }

    @SuppressLint("SimpleDateFormat")
    public override fun onStart() {
        super.onStart()

        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = auth.currentUser

        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
        currentDate = sdf.format(Date()).substring(0,10)

        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        fireStore?.firestoreSettings = settingsFireBase
        loadingPanel.visibility = View.GONE
        if( auth.currentUser!=null) {
            updateUI()
        }

        val downloadInformation =
                getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = downloadInformation.getString("lastDownloadDate", "")!!
        downloadedGJson = downloadInformation.getString("lastDownloadedGJson", "")!!
        Log.d(tag, "[onStart] Recalled lastDownloadDate is ’$downloadDate’")
        Log.d(tag, "[onStart] Recalled lastDownloadedGJson is ’$downloadedGJson")

        //if (PermissionsManager.areLocationPermissionsGranted(this)) {
        //    locationEngine.requestLocationUpdates()
        //    locationLayerPlugin.onStart()
        //}

    }

    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        if (!validateForm()) {
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        currentUser = auth.currentUser
                        val newUserContent = mapOf(
                                "dailyDistanceWalked" to 0,
                                "overallScore" to 0,
                                "dailyScore" to 0,
                                "dailyCollected" to 0,
                                "collectedIds" to ArrayList<String>(),
                                "collectedBankable" to 0,
                                "lastDateSignedIn" to "",
                                "collectedGift" to 0,
                                "collectedSpareChange" to 0)
                        fireStore?.collection("users")
                                ?.document(currentUser!!.email!!)
                                ?.set(newUserContent)
                                ?.addOnSuccessListener {
                                    Toast.makeText(
                                            baseContext, "Your account has been created!",
                                            Toast.LENGTH_SHORT).show() }
                        updateUI()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                                baseContext, "E-mail does not exist or is already used.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun signIn(email: String, password: String) {
        Log.d(TAG, "signIn:$email")
        if (!validateForm()) {
            return
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        currentUser= auth.currentUser
                        updateUI()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Your e-mail or password is incorrect.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    //Checks if the fields for the email and password have been filled
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

    private fun updateUI() {
        loadingPanel.visibility = View.VISIBLE
        fieldEmail.visibility = View.GONE
        fieldPassword.visibility = View.GONE
        createAccountButton.visibility= View.GONE
        signInButton.visibility = View.GONE

        userReference = fireStore?.collection("users")?.document(currentUser!!.email!!)
        userReference?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(tag, "DocumentSnapshot data: " + document.data)
                        user = document.toObject(User::class.java)!!
                        if(user?.lastDateSignedIn!=currentDate) {
                            resetUser()
                            updateUser()
                        }
                        //starts Maps activity
                        val intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                        loadingPanel.visibility = View.GONE
                    } else {
                        Log.d(tag, "No such document")
                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "get failed with ", exception)
                }
    }

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.createAccountButton -> createAccount(
                    fieldEmail.text.toString(),
                    fieldPassword.text.toString())
            R.id.signInButton -> signIn(
                    fieldEmail.text.toString(),
                    fieldPassword.text.toString())
           // R.id.signOutButton -> signOut()
        }
    }

    override fun onStop() {
        super.onStop()

        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        val editor = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("lastDownloadedGJson", downloadedGJson)
        editor.apply()
    }

    //resetUser is used when the last date the user has signed in is not today, i.e. all his
    //data are reset for the new day
    private fun resetUser() {
        MainActivity.user?.collectedIds =ArrayList()
        MainActivity.user?.collectedBankable =0
        MainActivity.user?.collectedSpareChange = 0
        MainActivity.user?.dailyCollected= 0
        MainActivity.user?.dailyDistanceWalked = 0
        MainActivity.user?.dailyScore = 0
        MainActivity.user?.lastDateSignedIn = currentDate
        MainActivity.user?.collectedGift = 0
    }

    private fun updateUser(){
        userReference?.update(
                "lastDateSignedIn", currentDate,
                "dailyCollected", user?.dailyCollected,
                "dailyDistanceWalked", user?.dailyDistanceWalked,
                "dailyScore", user?.dailyScore,
                "collectedBankable", user?.collectedBankable,
                "collectedSpareChange",user?.collectedSpareChange,
                "collectedIds", user?.collectedIds,
                "collectedGift", user?.collectedGift,
                "overallScore",user?.overallScore)
    }

}