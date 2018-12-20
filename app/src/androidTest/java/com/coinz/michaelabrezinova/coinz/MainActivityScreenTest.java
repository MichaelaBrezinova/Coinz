package com.coinz.michaelabrezinova.coinz;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainActivityScreenTest {

    @Rule
    public ActivityTestRule<MainActivity> mainActivityActivityTestRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    @Test
    public void clickSignInButtonWithEmptyFields_errorsShow() throws Exception {
        onView(withId(R.id.fieldEmail))
                .perform(clearText());
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText());
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.signInButton)).perform(click());
        onView(withId(R.id.fieldEmail)).check(matches(hasErrorText("Required.")));
        onView(withId(R.id.fieldPassword)).check(matches(hasErrorText("Required.")));

    }

    @Test
    public void clickSignInButtonWithShortPassword_errorsShow() throws Exception {
        onView(withId(R.id.fieldEmail))
                .perform(clearText());
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("TEST"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.signInButton)).perform(click());
        onView(withId(R.id.fieldEmail))
                .check(matches(hasErrorText("Required.")));
        onView(withId(R.id.fieldPassword))
                .check(matches(hasErrorText("At least 6 characters.")));

    }

    @Test
    public void clickCreateAccountButtonWithEmptyFields_errorsShow() throws Exception {
        onView(withId(R.id.fieldEmail))
                .perform(clearText());
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText());
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.createAccountButton)).perform(click());
        onView(withId(R.id.fieldEmail)).check(matches(hasErrorText("Required.")));
        onView(withId(R.id.fieldPassword)).check(matches(hasErrorText("Required.")));

    }

    @Test
    public void clickSignInButtonNonExistingUser_toastShow() throws Exception {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("nonexistantuser@gmail.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("somepassword"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.signInButton)).perform(click());
        onView(withText("Your e-mail or password is incorrect," +
                " please check your information and try again."))
                .inRoot(withDecorView(not(is(mainActivityActivityTestRule
                        .getActivity()
                        .getWindow()
                        .getDecorView()))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void clickCreateAccountButtonWithExistingUser_toastShow() throws Exception {
        onView(withId(R.id.fieldEmail))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldPassword))
                .perform(clearText())
                .perform(typeText("MB123123"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.createAccountButton)).perform(click());
        onView(withText("Sign-up failed, please, check information " +
                "provided and try again."))
                .inRoot(withDecorView(not(is(mainActivityActivityTestRule
                        .getActivity()
                        .getWindow()
                        .getDecorView()))))
                .check(matches(isDisplayed()));
    }
}