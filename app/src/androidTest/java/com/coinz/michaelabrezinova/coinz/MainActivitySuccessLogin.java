package com.coinz.michaelabrezinova.coinz;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import kotlin.jvm.JvmField;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivitySuccessLogin {

    @Rule
    public ActivityTestRule<MainActivity> mainActivityActivityTestRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    /*@Rule
    @JvmField
    public IntentsTestRule<MainActivity> intentsTestRule =
            new IntentsTestRule<>(MainActivity.class);
    */
    //Always grant permission to access the location
    @Rule @JvmField
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void clickSignInButton_successfullySignIn() throws Exception {
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

        //Check
        //Intents.init();
        //intended(hasComponent(MapsActivity.class.getName()));
        onView(withId(R.id.mapboxMapView)).check(matches(isDisplayed()));
        onView(withId(R.id.openWalletButton)).check(matches(isDisplayed()));
        onView(withId(R.id.changeThemeButton)).check(matches(isDisplayed()));
        onView(withId(R.id.CountCollected)).check(matches(isDisplayed()));
        onView(withId(R.id.coins)).check(matches(isDisplayed()));
    }
}
