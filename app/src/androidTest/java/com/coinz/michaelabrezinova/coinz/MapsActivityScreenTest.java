package com.coinz.michaelabrezinova.coinz;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import kotlin.jvm.JvmField;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MapsActivityScreenTest {

    @Rule
    public ActivityTestRule<MainActivity> mainActivityActivityTestRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    //Always grant permission to access the location
    @Rule @JvmField
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    /*@Rule
    @JvmField
    public IntentsTestRule<MapsActivity> intentsTestRule =
            new IntentsTestRule<>(MapsActivity.class);*/

    @Test
    public void clickWalletButton_opensWallet() throws Exception {

        //Set up
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("MB123123"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(10000);
        onView(withId(R.id.openWalletButton)).perform(click());
        Thread.sleep(5000);

        //Check
        onView(withId(R.id.purplecircle)).check(matches(isDisplayed()));
        onView(withId(R.id.overallScore)).check(matches(isDisplayed()));
        onView(withId(R.id.overallScoreSign)).check(matches(isDisplayed()));
        onView(withId(R.id.spareChangeSign)).check(matches(isDisplayed()));
        onView(withId(R.id.collectedSpareChange)).check(matches(isDisplayed()));
        onView(withId(R.id.giftSign)).check(matches(isDisplayed()));
        onView(withId(R.id.collectedGift)).check(matches(isDisplayed()));
        onView(withId(R.id.collectedSign)).check(matches(isDisplayed()));
        onView(withId(R.id.collectedBankable)).check(matches(isDisplayed()));
        onView(withId(R.id.banktransferButton)).check(matches(isDisplayed()));
        onView(withId(R.id.friendtransferButton)).check(matches(isDisplayed()));
        onView(withId(R.id.signOutButton)).check(matches(isDisplayed()));
    }
}