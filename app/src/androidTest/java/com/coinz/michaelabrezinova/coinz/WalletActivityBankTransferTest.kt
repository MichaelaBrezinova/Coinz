package com.coinz.michaelabrezinova.coinz

import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.hasErrorText
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.clearText
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId

@RunWith(AndroidJUnit4::class)
class WalletActivityBankTransferTest {

    @Rule
    @JvmField
    var mainActivityActivityTestRule = ActivityTestRule(MainActivity::class.java)

    //Always grant permission to access the location
    @Rule
    @JvmField
    var grantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)!!

    //Run clearData from gradle to clear the emulator, specified in configurations
    @Test
    @Throws(Exception::class)
    fun clickBankTransferButton_openDialog() {
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
        onView(withId(R.id.banktransferButton)).perform(click())
        Thread.sleep(2000)

        //Check if the dialog is displayed and all the ui elements
        onView(withText("Transfer to the bank"))
                .inRoot(isDialog()).check(matches(isDisplayed()))

        onView(withId(R.id.transferToBankTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.sendToBankButton)).check(matches(isDisplayed()))
        onView(withId(R.id.cancelBankButton)).check(matches(isDisplayed()))
        onView(withId(R.id.SourceBankRadioGroup)).check(matches(isDisplayed()))
        onView(withId(R.id.fieldAmountToBankTransfer)).check(matches(isDisplayed()))
        onView(withId(R.id.sendToBankButton)).perform(click())

        //Check when nothing entered
        onView(withId(R.id.fieldAmountToBankTransfer))
                .check(matches(hasErrorText("Required.")))

        onView(withId(R.id.fieldAmountToBankTransfer))
                .perform(clearText())
                .perform(typeText("0"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.sendToBankButton)).perform(click())

        //Check when invalid amount is entered
        onView(withId(R.id.fieldAmountToBankTransfer))
                .check(matches(hasErrorText("Cannot bank nothing.")))

        //Check if dialog is closed on cancel button
        onView(withId(R.id.cancelBankButton)).perform(click())
        Thread.sleep(2000)
        onView(withText("Transfer to the bank")).check(doesNotExist())

        //To sign-out for next tests, if clearData is not specified in configurations
        onView(withId(R.id.signOutButton)).perform(click())
    }
}
