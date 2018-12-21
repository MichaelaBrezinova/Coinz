package com.coinz.michaelabrezinova.coinz

import android.app.AlarmManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_wallet.*
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import timber.log.Timber

class WalletActivity : AppCompatActivity(),View.OnClickListener {

    private val tag= "WalletActivity"

    private var fireStore: FirebaseFirestore? = null
    private lateinit var auth: FirebaseAuth
    private var userReference: DocumentReference? = null

    private var bankable: TextView? = null
    private var gift: TextView? = null
    private var spareChange: TextView? = null
    private var overallScore: TextView? = null

    private var currentDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet)
        setSupportActionBar(toolbar2)


        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        //Set buttons in the activity to be clickable
        friendtransferButton.setOnClickListener(this)
        banktransferButton.setOnClickListener(this)
        signOutButton.setOnClickListener(this)

        //Initialize textViews displaying scores
        bankable = findViewById(R.id.collectedBankable)
        gift = findViewById(R.id.collectedGift)
        spareChange = findViewById(R.id.collectedSpareChange)
        overallScore = findViewById(R.id.overallScore)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onStart() {
        super.onStart()

        //Set fireStore settings
        val settingsFireBase = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        fireStore?.firestoreSettings = settingsFireBase

        //Initialize user's fireBase information, i.e. get reference to user's document
        userReference = fireStore?.collection("users")
                ?.document(MainActivity.currentUser!!.email!!)

        //Set the current date
        currentDate = MainActivity.currentDate

        //Set the text of the textViews to correspond to given values of variables
        bankable?.text = MainActivity.user?.collectedBankable!!.toString()
        gift?.text = MainActivity.user?.collectedGift!!.toString()
        spareChange?.text = MainActivity.user?.collectedSpareChange!!.toString()
        overallScore?.text = MainActivity.user?.bankAccount!!.toString()

        //Set up alarm(time checker) to check the time and date
        val repeatTime = 120 //Repeat time -2 minutes
        val timeChecker: AlarmManager =
                applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, ProcessTimeReceiver::class.java)
        val pendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                        this, 0,  intent, PendingIntent.FLAG_UPDATE_CURRENT)
        //Repeat alarm every 5 seconds
        timeChecker.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(), (repeatTime*5000).toLong(), pendingIntent)

        //Initialize listener to updates from fireStore
        realTimeUpdateListener()
    }

    override fun onStop() {
        super.onStop()

        //Cancel the alarm for checking the time
        val intent = Intent(this, ProcessTimeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, 0)
        val timeChecker = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        timeChecker.cancel(pendingIntent)
    }

    //Set up functions called by pressing given buttons
    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.friendtransferButton -> transferToFriendDialog()
            R.id.banktransferButton -> transferToBankDialog()
            R.id.signOutButton -> signOut()
        }
    }

    //Sign out the user and call the login screen - MainActivity
    private fun signOut() {
        auth.signOut()
        MainActivity.currentUser = null
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    //Open dialog with option to transfer money to friend
    private fun transferToFriendDialog() {

        val dialog = Dialog(this)
        //Disable canceling the dialog from outside
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.friend_transfer_dialog)
        dialog.setTitle("Transfer to a friend")

        //Clicking on cancel button closes the dialog without making any change
        val cancelButton = dialog.findViewById(R.id.cancelButton) as Button
        cancelButton.setOnClickListener { dialog.dismiss() }

        //Send button, calls sendToFriend function verifying the fields and if valid sending money
        val sendToFriendButton = dialog.findViewById(R.id.sendToFriendButton) as Button
        sendToFriendButton.setOnClickListener {sendToFriendClicked(dialog) }

        dialog.show()

    }

    //Gets data from input fields, verifies validity and initializes transfer
    private fun sendToFriendClicked(dialog: Dialog) {

        //Input fields data
        val emailToTransfer = dialog.findViewById<EditText>(R.id.fieldEmailToTransfer)
        val amountToTransfer = dialog.findViewById<EditText>(R.id.fieldAmountToTransfer)
        val email = emailToTransfer.text.toString()
        val amount = amountToTransfer.text.toString()

        //Get source of money to transfer - SpareChange or Gift
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.SourceRadioGroup)
        val selectedId = radioGroup.checkedRadioButtonId
        val isSpareChange = selectedId==R.id.radioSpareChange

        //Gets disposable money depending on the source - Spare Change or Gift
        val possession = if (isSpareChange){
            MainActivity.user!!.collectedSpareChange
        } else {
            MainActivity.user!!.collectedGift
        }

        //If fields are valid, initialize transfer
        if (validateFriendTransferForm(emailToTransfer,amountToTransfer,possession)) {
            friendMoneyTransfer(dialog, email, amount.toInt(), isSpareChange)
        }
    }

    //Checks if the fields for the email and password have been filled correctly
    private fun validateFriendTransferForm(
            emailToTransfer: EditText, amountToTransfer: EditText, possession: Int): Boolean {

        //Input in the fields
        val email = emailToTransfer.text.toString()
        val amount = amountToTransfer.text.toString()

        //Validity tracker
        var valid = true

        //Check email field
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

        //Check amount field
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

        //Return if the fields are valid or not
        return valid
    }

    //Initialize transfer to a friend
    private fun friendMoneyTransfer(
            dialog:Dialog, email: String, amount: Int, isSpareChange: Boolean) {

        //Get reference to friend's foreStore document, get the current value of collectedGift
        //and add the amount the user is transfering to it
        val userReference =
                fireStore?.collection("users")?.document(email)
        userReference?.get()
                ?.addOnSuccessListener { document ->

                    //Checks if the user's collectedGift is not null( the user exists), if not
                    //cancels the transaction
                    if (document?.get("collectedGift") != null) {
                        val originalGift = document.get("collectedGift").toString().toInt()
                        userReference.update("collectedGift", originalGift + amount)

                        //update user information
                        if(isSpareChange){
                            MainActivity.user?.collectedSpareChange =
                                    MainActivity.user?.collectedSpareChange!!.minus(amount)
                            spareChange?.text = MainActivity.user?.collectedSpareChange!!.toString()
                        } else {
                            MainActivity.user?.collectedGift =
                                    MainActivity.user?.collectedGift!!.minus(amount)
                            gift?.text = MainActivity.user?.collectedGift!!.toString()
                        }
                        updateUser()

                        dialog.dismiss()
                    } else {
                        //Notifies the user that there is no user with such email address.
                        Timber.tag(tag).d( "No such user")
                        dialog.findViewById<EditText>(R.id.fieldEmailToTransfer).error =
                                "User does not exist."
                    }
                }
                ?.addOnFailureListener { exception ->
                    Timber.tag(tag).d(exception, "get failed with ")
                }
    }

    //Open dialog with option to transfer money to the bank
    private fun transferToBankDialog() {

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.bank_transfer_dialog)
        dialog.setTitle("Transfer to the bank")

        //Cancel button, if clicked, closes the dialog without any change
        val cancelButton = dialog.findViewById(R.id.cancelBankButton) as Button
        cancelButton.setOnClickListener { dialog.dismiss() }

        //Send button, calls sendToBank function verifying the fields and if valid sending money
        val sendToBankButton = dialog.findViewById(R.id.sendToBankButton) as Button
        sendToBankButton.setOnClickListener {sendToBankClicked(dialog)}

        dialog.show()

    }

    private fun sendToBankClicked(dialog: Dialog) {
        //Input fields data
        val amountToTransfer =
                dialog.findViewById<EditText>(R.id.fieldAmountToBankTransfer)
        val amount = amountToTransfer.text.toString()

        //Get source of money to transfer - Gift or CollectedBankable
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.SourceBankRadioGroup)
        val selectedId = radioGroup.checkedRadioButtonId
        val isCollectedBankable = selectedId==R.id.radioBankCollected

        //Gets disposable money depending on the source - Gift or CollectedBankable
        val possession = if (isCollectedBankable){
            MainActivity.user!!.collectedBankable
        } else {
            MainActivity.user!!.collectedGift
        }

        //If fields are valid, initialize transfer
        if (validateBankForm(amountToTransfer,possession)) {
            bankMoneyTransfer(dialog, amount.toInt(), isCollectedBankable)
        }
    }

    //Checks if the amount field is correctly filled in
    private fun validateBankForm(
            amountToTransfer: EditText, possession: Int): Boolean {

        //Checks if the amount entered is valid
        val amount = amountToTransfer.text.toString()
        when {
            TextUtils.isEmpty(amount) -> {
                amountToTransfer.error = "Required."
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

    //Transfers money to the bank account of the current user
    private fun bankMoneyTransfer(
            dialog:Dialog, amount: Int, isCollectedBankable: Boolean) {

        //Updates local variables(depending on the source of money) and then updates fireBase
        MainActivity.user?.bankAccount = MainActivity.user?.bankAccount!!.plus(amount)
        overallScore?.text = MainActivity.user?.bankAccount!!.toString()
        if(isCollectedBankable){
            MainActivity.user?.collectedBankable =
                    MainActivity.user?.collectedBankable!!.minus(amount)
            bankable?.text = MainActivity.user?.collectedBankable!!.toString()
        } else {
            MainActivity.user?.collectedGift =
                    MainActivity.user?.collectedGift!!.minus(amount)
            gift?.text = MainActivity.user?.collectedGift!!.toString()
        }
        updateUser()

        dialog.dismiss()
    }

    //Updates user information in the fireBase to stay sync
    private fun updateUser() {
        try{ userReference?.update(
                "collectedBankable", MainActivity.user?.collectedBankable,
                "collectedSpareChange", MainActivity.user?.collectedSpareChange,
                "collectedGift", MainActivity.user?.collectedGift,
                "bankAccount", MainActivity.user?.bankAccount)
        } catch(e:Exception) {
            Timber.tag(tag).d( e,"[updateUser] get failed with ")
            Toast.makeText(
                    baseContext, "Cannot update user info! Check your internet connection.",
                    Toast.LENGTH_SHORT).show()
        }
    }

    //Real time updates from the fireBase, synchronizes text displaying gift money amount and the
    //local variable holding the gift money amount
    private fun realTimeUpdateListener() {
        userReference?.addSnapshotListener{ documentSnapshot, e ->
            when {
                e != null -> Timber.tag(tag).e( e.message)
                documentSnapshot != null && documentSnapshot.exists() -> {
                    with(documentSnapshot) {
                        val collectedGift = data?.get("collectedGift").toString().toInt()
                        MainActivity.user?.collectedGift = collectedGift
                        val gift = findViewById<TextView>(R.id.collectedGift)
                        gift?.text =MainActivity.user?.collectedGift!!.toString()
                    }
                }
            }
        }
    }
}
