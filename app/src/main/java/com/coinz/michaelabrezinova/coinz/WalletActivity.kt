package com.coinz.michaelabrezinova.coinz

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.View

import kotlinx.android.synthetic.main.activity_wallet.*
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.Html
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
    private var currentDate: String = ""

    private var bankable: TextView? = null
    private var gift: TextView? = null
    private var spareChange: TextView? = null
    private var overallScore: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        setSupportActionBar(toolbar)
        fireStore = FirebaseFirestore.getInstance()
        friendtransferButton.setOnClickListener(this)
        banktransferButton.setOnClickListener(this)

        bankable = findViewById(R.id.collectedBankable)
        gift = findViewById(R.id.collectedGift)
        spareChange = findViewById(R.id.collectedSpareChange)
        overallScore = findViewById(R.id.overallScore)

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

        bankable?.text = MainActivity.user?.collectedBankable!!.toString()
        gift?.text = MainActivity.user?.collectedGift!!.toString()
        spareChange?.text = MainActivity.user?.collectedSpareChange!!.toString()
        overallScore?.text = MainActivity.user?.overallScore!!.toString()

        realTimeUpdateListener()
    }

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.friendtransferButton -> transferToFriendDialog()
            R.id.banktransferButton -> transferToBankDialog()
        }
    }

    private fun transferToFriendDialog() {

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(false)
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
            if (validateFriendTransferForm(emailToTransfer,amountToTransfer,possession)) {
                friendMoneyTransfer(dialog, email, amount, isSpareChange)
            }
        }

        dialog.show()

    }

    private fun transferToBankDialog() {

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.bank_transfer_dialog)
        dialog.setTitle("Transfer to the bank")

        //Cancel button, if clicked, closes the dialog without a change
        val cancelButton = dialog.findViewById(R.id.cancelBankButton) as Button
        cancelButton.setOnClickListener { dialog.dismiss() }

        val sendToFriendButton = dialog.findViewById(R.id.sendToBankButton) as Button
        sendToFriendButton.setOnClickListener {
            val amountToTransfer =
                    dialog.findViewById<EditText>(R.id.fieldAmountToBankTransfer)
            val amount = amountToTransfer.text.toString()

            //Find selected radio button
            val radioGroup = dialog.findViewById<RadioGroup>(R.id.SourceBankRadioGroup)
            val selectedId = radioGroup.getCheckedRadioButtonId();
            val radioButton =  dialog.findViewById<RadioButton>(selectedId)
            val isCollected = selectedId==R.id.radioBankCollected
            var possession = if (isCollected){
                MainActivity.user!!.collectedBankable
            } else {
                MainActivity.user!!.collectedGift
            }
            if (validateBankForm(amountToTransfer,possession)) {
                bankMoneyTransfer(dialog, amount, isCollected)
            }
        }

        dialog.show()

    }

    //Checks if the fields for the email and password have been filled
    private fun validateFriendTransferForm(emailToTransfer: EditText, amountToTransfer: EditText,
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

    private fun friendMoneyTransfer(
            dialog:Dialog, email: String, amount: String, isSpareChange: Boolean) {

        val userReference =
                fireStore?.collection("users")?.document(email)
        userReference?.get()
                ?.addOnSuccessListener { document ->
                    if (document != null) {
                        var originalGift = document.get("collectedGift").toString().toInt()
                        userReference.update("collectedGift", originalGift + amount.toInt())
                        if(isSpareChange){
                            MainActivity.user?.collectedSpareChange =
                                    MainActivity.user?.collectedSpareChange!!.minus(amount.toInt())
                            spareChange?.text = MainActivity.user?.collectedSpareChange!!.toString()
                        } else {
                            MainActivity.user?.collectedGift =
                                    MainActivity.user?.collectedGift!!.minus(amount.toInt())
                            gift?.text = MainActivity.user?.collectedGift!!.toString()
                        }
                        updateUser()
                        dialog.dismiss()
                    } else {
                        Log.d(tag, "No such document")

                    }
                }
                ?.addOnFailureListener { exception ->
                    Log.d(tag, "get failed with ", exception)
                }
    }

    //Checks if the amount field is correctly filled in
    private fun validateBankForm(
            amountToTransfer: EditText, possession: Int): Boolean {

        val amount = amountToTransfer.text.toString()


        when {
            TextUtils.isEmpty(amount) -> {
                amountToTransfer.error = "Required."
                // Html.fromHtml("<font color='blue'>this is the error</font>")
                return false
            }
            amount.toInt()==0 -> {
                amountToTransfer.error = "Cannot bank nothing."
                return false
            }
            amount.toInt()>possession -> {
                amountToTransfer.error = "Insufficient funds."
                return false
            }
            else -> amountToTransfer.error = null
        }
        return true
    }

    //Sends money to the bank account of the current user
    private fun bankMoneyTransfer(
            dialog:Dialog, amount: String, isCollected: Boolean) {

        MainActivity.user?.overallScore = MainActivity.user?.overallScore!!.plus(amount.toInt())
        overallScore?.text = Integer.toString(MainActivity.user?.overallScore!!)
        if(isCollected){
            MainActivity.user?.collectedBankable =
                    MainActivity.user?.collectedBankable!!.minus(amount.toInt())
            bankable?.text =
                    Integer.toString(MainActivity.user?.collectedBankable!!)
        } else {
            MainActivity.user?.collectedGift =
                    MainActivity.user?.collectedGift!!.minus(amount.toInt())
            gift?.text =
                    Integer.toString(MainActivity.user?.collectedGift!!)
        }
        updateUser()
        dialog.dismiss()
    }

    //Updates user information in the database to stay sync
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
                "overallScore", MainActivity.user?.overallScore
        )
    }

    private fun realTimeUpdateListener() {
        userReference?.addSnapshotListener{ documentSnapshot, e ->
            when {
                e != null -> Log.e(tag, e.message)
                documentSnapshot != null && documentSnapshot.exists() -> {
                    with(documentSnapshot) {
                        val collected = data?.get("collectedGift").toString().toInt()
                        MainActivity.user?.collectedGift = collected
                        var gift = findViewById<TextView>(R.id.collectedGift)
                        gift?.text =
                                Integer.toString(MainActivity.user?.collectedGift!!)
                    }
                }
            }
        }
    }
}
