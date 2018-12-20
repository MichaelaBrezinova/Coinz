package com.coinz.michaelabrezinova.coinz

//Class User corresponding to the user's information in the FireStore
data class User (
        var collectedIds: ArrayList<Any?> = ArrayList(),
        var collectedBankable: Int = 0,
        var collectedSpareChange: Int = 0,
        var dailyCollected: Int = 0,
        var dailyDistanceWalked: Int = 0,
        var bankAccount: Int = 0,
        var collectedGift: Int = 0,
        var lastDateSignedIn: String = ""
)


