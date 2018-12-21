package com.coinz.michaelabrezinova.coinz

import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not


import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.clearText
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.RootMatchers.withDecorView
import android.support.test.espresso.matcher.ViewMatchers.hasErrorText
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.view.View

@RunWith(AndroidJUnit4::class)
class MainActivityScreenTest {

    @Rule @JvmField
    var mainActivityActivityTestRule = ActivityTestRule(MainActivity::class.java)

    //Run clearData from gradle to clear the data before tests, implemented in the configurations
    @Test
    @Throws(Exception::class)
    fun clickSignInButtonWithEmptyFields_errorsShow() {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.signInButton)).perform(click())
        onView(withId(R.id.fieldEmail)).check(matches(hasErrorText("Required.")))
        onView(withId(R.id.fieldPassword)).check(matches(hasErrorText("Required.")))

    }

    @Test
    @Throws(Exception::class)
    fun clickSignInButtonWithShortPassword_errorsShow() {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("TEST"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.signInButton)).perform(click())
        onView(withId(R.id.fieldEmail))
                .check(matches(hasErrorText("Required.")))
        onView(withId(R.id.fieldPassword))
                .check(matches(hasErrorText("At least 6 characters.")))

    }

    @Test
    @Throws(Exception::class)
    fun clickCreateAccountButtonWithEmptyFields_errorsShow() {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.createAccountButton)).perform(click())
        onView(withId(R.id.fieldEmail)).check(matches(hasErrorText("Required.")))
        onView(withId(R.id.fieldPassword)).check(matches(hasErrorText("Required.")))

    }

    @Test
    @Throws(Exception::class)
    fun clickSignInButtonNonExistingUser_toastShow() {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("nonexistantuser@gmail.com"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("somepassword"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.signInButton)).perform(click())
        onView(withText("Your e-mail or password is incorrect,"
                + " please check your information and try again."))
                .inRoot(withDecorView(not<View>(`is`<View>(mainActivityActivityTestRule
                        .activity
                        .window
                        .decorView))))
                .check(matches(isDisplayed()))
    }

    @Test
    @Throws(Exception::class)
    fun clickCreateAccountButtonWithExistingUser_toastShow() {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("MB123123"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.createAccountButton)).perform(click())
        onView(withText("Sign-up failed, please, check information "
                + "provided and try again."))
                .inRoot(withDecorView(not<View>(`is`<View>(mainActivityActivityTestRule
                        .activity
                        .window
                        .decorView))))
                .check(matches(isDisplayed()))
    }
}