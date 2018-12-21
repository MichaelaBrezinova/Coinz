package com.coinz.michaelabrezinova.coinz

import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.clearText
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId

@RunWith(AndroidJUnit4::class)
class MapsActivityScreenTest {

    @Rule
    @JvmField
    var mainActivityActivityTestRule = ActivityTestRule(MainActivity::class.java)

    //Always grant permission to access the location
    @Rule
    @JvmField
    var grantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)!!

    //Run clearData from gradle file to clear data on the device before the test run
    //Specified in configurations
    @Test
    @Throws(Exception::class)
    fun clickWalletButton_opensWallet() {

        //Set up
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("MB123123"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.signInButton)).perform(click())
        Thread.sleep(10000)
        onView(withId(R.id.openWalletButton)).perform(click())
        Thread.sleep(5000)

        //Check if everything that should be displayed is displayed
        onView(withId(R.id.purplecircle)).check(matches(isDisplayed()))
        onView(withId(R.id.overallScore)).check(matches(isDisplayed()))
        onView(withId(R.id.overallScoreSign)).check(matches(isDisplayed()))
        onView(withId(R.id.spareChangeSign)).check(matches(isDisplayed()))
        onView(withId(R.id.collectedSpareChange)).check(matches(isDisplayed()))
        onView(withId(R.id.giftSign)).check(matches(isDisplayed()))
        onView(withId(R.id.collectedGift)).check(matches(isDisplayed()))
        onView(withId(R.id.collectedSign)).check(matches(isDisplayed()))
        onView(withId(R.id.collectedBankable)).check(matches(isDisplayed()))
        onView(withId(R.id.banktransferButton)).check(matches(isDisplayed()))
        onView(withId(R.id.friendtransferButton)).check(matches(isDisplayed()))
        onView(withId(R.id.signOutButton)).check(matches(isDisplayed()))

        //To sign-out for next tests, if clearData is not specified in configurations
        onView(withId(R.id.signOutButton)).perform(click())
    }
}