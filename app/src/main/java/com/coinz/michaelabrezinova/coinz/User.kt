package com.coinz.michaelabrezinova.coinz

data class User (
        var collectedIds: String,
        var collectedBankable: Int,
        var collectedSpareChange: Int,
        var dailyCollected: Int,
        var dailyDistanceWalked: Int,
        var dailyScore: Int,
        var overallScore: Int
)