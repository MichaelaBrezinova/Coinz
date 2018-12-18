package com.coinz.michaelabrezinova.coinz

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.View

import kotlinx.android.synthetic.main.activity_wallet.*
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.widget.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class WalletActivity : AppCompatActivity(),View.OnClickListener {

    private val tag= "WalletActivity"
    private var fireStore: FirebaseFirestore? = null
    private var userReference: DocumentReference? = null
    private val preferencesFile = "MyPrefsFile"
    private var currentDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        setSupportActionBar(toolbar)
        fireStore = FirebaseFirestore.getInstance()
        friendtransferButton.setOnClickListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onStart() {
        super.onStart()
        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        fireStore?.firestoreSettings = settingsFireBase
        userReference = fireStore?.collection("users")
                ?.document(MainActivity.currentUser!!.email!!)
        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss")
        currentDate = sdf.format(Date()).substring(0,10)
    }

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.friendtransferButton -> transferToFriendDialog(v)
            //R.id.changeThemeButton ->  mapView?.setStyleUrl("@string/mapbox_style_dark")
        }
    }

    override fun onStop() {
        super.onStop()
        userReference?.update(
                "lastDateSignedIn", currentDate,
                "dailyCollected", MainActivity.user?.dailyCollected,
                "dailyDistanceWalked", MainActivity.user?.dailyDistanceWalked,
                "dailyScore", 555,
                "collectedBankable", MainActivity.user?.collectedBankable,
                "collectedSpareChange", MainActivity.user?.collectedSpareChange,
                "collectedIds", MainActivity.user?.collectedIds,
                "collectedGift", MainActivity.user?.collectedGift)
        val editor = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE).edit()
        editor.putString("lastDownloadDate", MainActivity.downloadDate)
        editor.putString("lastDownloadedGJson", MainActivity.downloadedGJson)
        editor.apply()
    }

    private fun transferToFriendDialog(view: View) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.friend_transfer_dialog)
        dialog.setTitle("Transfer to a friend")

        //Cancel button, if clicked, closes the dialog without a change
        val cancelButton = dialog.findViewById(R.id.cancelButton) as Button
        cancelButton.setOnClickListener { dialog.dismiss() }

        val sendToFriendButton = dialog.findViewById(R.id.sendToFriendButton) as Button
        sendToFriendButton.setOnClickListener {
            val emailToTransfer = dialog.findViewById<EditText>(R.id.fieldEmailToTransfer)
            val amountToTransfer = dialog.findViewById<EditText>(R.id.fieldAmountToTransfer)
            val email = emailToTransfer.text.toString()
            val amount = amountToTransfer.text.toString()

            //Find selected radio button
            val radioGroup = dialog.findViewById<RadioGroup>(R.id.SourceRadioGroup)
            val selectedId = radioGroup.getCheckedRadioButtonId();
            val radioButton =  dialog.findViewById<RadioButton>(selectedId)
            val isSpareChange = selectedId==R.id.radioSpareChange
            var possession = if (isSpareChange){
                MainActivity.user!!.collectedSpareChange
            } else {
                MainActivity.user!!.collectedGift
            }
            if (validateForm(emailToTransfer,amountToTransfer,possession)) {
                transferMoney(dialog, email, amount, isSpareChange)
            }
        }

        dialog.show()

    }

    //Checks if the fields for the email and password have been filled
    private fun validateForm(emailToTransfer: EditText, amountToTransfer: EditText,
                             possession: Int): Boolean {
        var valid = true

        val email = emailToTransfer.text.toString()
        when {
            TextUtils.isEmpty(email) -> {
                emailToTransfer.error = "Required."
                valid = false
            }
            email == MainActivity.currentUser!!.email -> {
                emailToTransfer.error = "Cannot give money to yourself."
                valid = false
            }
            else -> emailToTransfer.error = null
        }

        val amount = amountToTransfer.text.toString()
        when {
            TextUtils.isEmpty(amount) -> {
                amountToTransfer.error = "Required."
                valid = false
            }
            amount.toInt()==0 -> {
                amountToTransfer.error = "Cannot gift nothing."
                valid = false
            }
            amount.toInt()>possession -> {
                amountToTransfer.error = "Insufficient funds."
                valid = false
            }
            else -> amountToTransfer.error = null
        }
        return valid
    }

    private fun transferMoney(dialog:Dialog, email: String, amount: String, isSpareChange: Boolean) {
        val userReference =
                fireStore?.collection("users")?.document(email)
        userReference?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        var gift = document.get("collectedGift").toString().toInt()
                        userReference.update("collectedGift", gift + amount.toInt())
                        updateUserValues(isSpareChange, amount.toInt())
                        dialog.dismiss()
                    } else {
                        Log.d(tag, "No such document")

                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "get failed with ", exception)
                }
    }

    private fun updateUserValues(isSpareChange: Boolean, amount: Int){
        if(isSpareChange){
            MainActivity.user?.collectedSpareChange =
                    MainActivity.user?.collectedSpareChange!!.minus(amount)
                    findViewById<TextView>(R.id.overallScore).setText(15)
        } else {
            MainActivity.user?.collectedGift =
                    MainActivity.user?.collectedSpareChange!!.minus(amount)
        }
    }

}
