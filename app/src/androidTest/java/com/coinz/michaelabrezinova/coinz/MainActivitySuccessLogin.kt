package com.coinz.michaelabrezinova.coinz

import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.clearText
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId

@RunWith(AndroidJUnit4::class)
class MainActivitySuccessLogin {

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
    fun clickSignInButton_successfullySignIn() {

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

        //Check
        onView(withId(R.id.mapboxMapView)).check(matches(isDisplayed()))
        onView(withId(R.id.openWalletButton)).check(matches(isDisplayed()))
        onView(withId(R.id.changeThemeButton)).check(matches(isDisplayed()))
        onView(withId(R.id.CountCollected)).check(matches(isDisplayed()))
        onView(withId(R.id.coins)).check(matches(isDisplayed()))
    }
}
