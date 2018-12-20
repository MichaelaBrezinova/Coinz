package com.coinz.michaelabrezinova.coinz

import android.os.Bundle
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.fieldEmail
import kotlinx.android.synthetic.main.activity_main.fieldPassword
import kotlinx.android.synthetic.main.activity_main.signInButton
import kotlinx.android.synthetic.main.activity_main.createAccountButton
import kotlinx.android.synthetic.main.activity_main.loadingPanel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val tag= "LoginActivity"
    private val tagEmailPassword="EmailPassword"

    companion object {
        var sdf: SimpleDateFormat? = null
        var currentUser: FirebaseUser? = null
        var user: User? = null
        var currentDate: String = ""
    }

    //Declare Authorization and fireStore
    private var userReference: DocumentReference? = null
    private lateinit var auth: FirebaseAuth
    private var fireStore: FirebaseFirestore? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //Initialize FireBase FireStore and Auth
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set on click listeners for buttons
        signInButton.setOnClickListener(this)
        createAccountButton.setOnClickListener(this)
    }

    public override fun onStart() {
        super.onStart()

        //Set up the currentDate
        sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss", Locale.US)
        currentDate = sdf?.format(Date())?.substring(0,10)!!

        user = User(ArrayList(), 0, 0, 0, 0, 0, 0, currentDate)
        //Set the fireStore settings
        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        fireStore?.firestoreSettings = settingsFireBase

        //Make the loading panel invisible
        loadingPanel.visibility = View.GONE

        //Checks if the user is signed-in(non-null)
        //If yes, updates UI accordingly
        currentUser = auth.currentUser
        if( currentUser!=null) {
            proceedToGame()
        }
    }

    //Set up onClick methods that create an account/sign-in the user
    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.createAccountButton -> createAccount(
                    fieldEmail.text.toString(),
                    fieldPassword.text.toString())
            R.id.signInButton -> signIn(
                    fieldEmail.text.toString(),
                    fieldPassword.text.toString())
        }
    }

    //Create a new user account
    private fun createAccount(email: String, password: String) {
        Timber.tag(tagEmailPassword).d(  "createAccount:$email")
        //If the fields are not filled correctly, show errors to the fields and do not proceed
        if (!validateForm()) {
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        //Creating of user successful, user is being signed in and their
                        //document i.e. account is created and set
                        Timber.tag(tagEmailPassword).d(  "createUserWithEmail:success")
                        currentUser = auth.currentUser
                        val newUserContent = mapOf(
                                "dailyDistanceWalked" to 0,
                                "bankAccount" to 0,
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
                                    //Toast notifying user about success of account creation
                                    Toast.makeText(
                                            baseContext, "Your account has been created!",
                                            Toast.LENGTH_SHORT).show() }

                        //UI is updated, MapsActivity is called
                        proceedToGame()
                    } else {
                        //If sign-up fails, display a message to the user and do not proceed.
                        Timber.tag(tagEmailPassword).d( task.exception,
                                "createUserWithEmail:failure")
                        Toast.makeText(
                                baseContext, "Sign-up failed, please, check information " +
                                "provided and try again.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    //Sign-in the user
    private fun signIn(email: String, password: String) {
        Timber.tag(tagEmailPassword).d( "signIn:$email")
        //If the fields are not filled correctly, show errors to the fields and do not proceed
        if (!validateForm()) {
            return
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Timber.tag(tagEmailPassword).d( "signInWithEmail:success")
                        currentUser= auth.currentUser
                        proceedToGame()
                    } else {
                        // If sign in fails, display a message to the user.
                        Timber.tag(tagEmailPassword).d( task.exception,
                                "signInWithEmail:failure")
                        Toast.makeText(baseContext, "Your e-mail or password is incorrect," +
                                " please check your information and try again.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    //Checks if the fields for the email and password have been filled
    private fun validateForm(): Boolean {
        var valid = true

        //Check e-mail field
        val email = fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            fieldEmail.error = "Required."
            valid = false
        } else {
            fieldEmail.error = null
        }

        //Check password field
        val password = fieldPassword.text.toString()
        when {
            TextUtils.isEmpty(password) -> {
                fieldPassword.error = "Required."
                valid = false
            }
            password.length<6 -> fieldPassword.error = "At least 6 characters."
            else -> fieldPassword.error = null
        }

        //Return validity of the input fields
        return valid
    }

    //If sign-in/sign-up successful, proceed to the game
    private fun proceedToGame() {

        //Show loading panel and hide other ui components
        loadingPanel.visibility = View.VISIBLE
        fieldEmail.visibility = View.GONE
        fieldPassword.visibility = View.GONE
        createAccountButton.visibility= View.GONE
        signInButton.visibility = View.GONE

        //Get user information and store them to local object User
        //If user's last sign-in date is not today, reset their information
        userReference = fireStore?.collection("users")
                ?.document(currentUser!!.email!!)
        userReference?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        try {
                            Timber.tag(tag).d(
                                    String.format("DocumentSnapshot data: " + document.data))
                            user = document.toObject(User::class.java)!!
                            if(user?.lastDateSignedIn!=currentDate) {
                                resetUser(user?.bankAccount!!)
                                updateUser()
                            }
                            //starts Maps activity
                            val intent = Intent(this, MapsActivity::class.java)
                            startActivity(intent)
                        } catch(e:Exception) {
                            Timber.tag(tag).d("[proceedToGame] wrong fireStore info")
                            //User has wrong information in the fireStore, reset their information
                            //try to retrieve bankAccount score
                            val bankAccount = document.get("bankAccount")
                                    .toString().toIntOrNull()
                            if(bankAccount!=null){
                                resetUser(bankAccount)
                                updateUser()
                            }
                            Toast.makeText(
                                    baseContext, "Your information were reset.",
                                    Toast.LENGTH_SHORT).show()

                        }
                    } else {
                        Timber.tag(tag).d( "No such document")
                        Toast.makeText(
                                baseContext, "Cannot proceed! Check your internet " +
                                "connection and information entered!",
                                Toast.LENGTH_SHORT).show()
                    }
                }
                ?.addOnFailureListener { exception ->
                    Timber.tag(tag).d( exception,"get failed with ")
                    Toast.makeText(
                            baseContext, "Cannot proceed! Check your internet connection.",
                            Toast.LENGTH_SHORT).show()
                }
    }

    //resetUser is used when the last date the user has signed in is not today, i.e. all his
    //data are reset for the new day
    private fun resetUser(bankAccount: Int) {
        user = User(ArrayList(), 0, 0, 0, 0, 0, bankAccount, currentDate)
    }

    private fun updateUser(){
        try{userReference?.update(
                "lastDateSignedIn", currentDate,
                "dailyCollected", user?.dailyCollected,
                "dailyDistanceWalked", user?.dailyDistanceWalked,
                "collectedBankable", user?.collectedBankable,
                "collectedSpareChange",user?.collectedSpareChange,
                "collectedIds", user?.collectedIds,
                "collectedGift", user?.collectedGift,
                "bankAccount",user?.bankAccount)
        } catch (e: Exception) {
            Timber.tag(tag).d( e,"[updateUser] get failed with ")
            Toast.makeText(
                    baseContext, "Cannot update user info! Check your internet connection.",
                    Toast.LENGTH_SHORT).show()
        }
    }

}