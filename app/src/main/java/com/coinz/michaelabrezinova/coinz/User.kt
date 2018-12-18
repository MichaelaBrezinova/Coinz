package com.coinz.michaelabrezinova.coinz

data class User (
        var collectedIds: ArrayList<Any?> = ArrayList(),
        var collectedBankable: Int = 0,
        var collectedSpareChange: Int = 0,
        var dailyCollected: Int = 0,
        var dailyDistanceWalked: Int = 0,
        var dailyScore: Int = 0,
        var overallScore: Int = 0,
        var collectedGift: Int = 0,
        var lastDateSignedIn: String = ""
)