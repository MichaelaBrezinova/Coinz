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
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.hasErrorText
import android.support.test.espresso.matcher.ViewMatchers.withText

@RunWith(AndroidJUnit4::class)
class WalletActivityFriendTransferTest {

    @Rule
    @JvmField
    var mainActivityActivityTestRule = ActivityTestRule(MainActivity::class.java)

    //Always grant permission to access the location
    @Rule
    @JvmField
    var grantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)!!

    //Run clearData before each test to clear the emulator data
    @Test
    @Throws(Exception::class)
    fun clickFriendTransferButton_functionalityTested() {

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
        onView(withId(R.id.friendtransferButton)).perform(click())
        Thread.sleep(2000)

        //Check

        //Check if dialog is displayed and all the UI elements as well
        onView(withText("Transfer to a friend"))
                .inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.transferToFriendTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.sendToFriendButton)).check(matches(isDisplayed()))
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()))
        onView(withId(R.id.SourceRadioGroup)).check(matches(isDisplayed()))
        onView(withId(R.id.fieldEmailToTransfer)).check(matches(isDisplayed()))
        onView(withId(R.id.fieldAmountToTransfer)).check(matches(isDisplayed()))

        onView(withId(R.id.sendToFriendButton)).perform(click())

        //Check when nothing entered
        onView(withId(R.id.fieldEmailToTransfer))
                .check(matches(hasErrorText("Required.")))
        onView(withId(R.id.fieldAmountToTransfer))
                .check(matches(hasErrorText("Required.")))

        onView(withId(R.id.fieldEmailToTransfer))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldAmountToTransfer))
                .perform(clearText())
                .perform(typeText("0"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.sendToFriendButton)).perform(click())

        //Check when invalid values entered
        onView(withId(R.id.fieldAmountToTransfer))
                .check(matches(hasErrorText("Cannot gift nothing.")))
        onView(withId(R.id.fieldEmailToTransfer))
                .check(matches(hasErrorText("Cannot give money to yourself.")))

        onView(withId(R.id.fieldAmountToTransfer))
                .perform(clearText())
                .perform(typeText("0"))
        Espresso.closeSoftKeyboard()

        //Check if dialog is closed on cancel button
        onView(withId(R.id.cancelButton)).perform(click())
        Thread.sleep(5000)
        onView(withText("Transfer to a friend")).check(doesNotExist())


    }
}