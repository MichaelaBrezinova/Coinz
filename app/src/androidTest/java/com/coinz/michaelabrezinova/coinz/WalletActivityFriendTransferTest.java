package com.coinz.michaelabrezinova.coinz;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.view.View;
import android.widget.TextView;

import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import org.hamcrest.Matcher;
import org.junit.Before;
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
public class WalletActivityFriendTransferTest {

    @Rule
    public ActivityTestRule<MainActivity> mainActivityActivityTestRule =
            new ActivityTestRule<MainActivity>(MainActivity.class);

    //Always grant permission to access the location
    @Rule @JvmField
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void clickFriendTransferButton_functionalityTested() throws Exception {

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
        onView(withId(R.id.friendtransferButton)).perform(click());
        Thread.sleep(2000);
        //String text = getText(withId(R.id.overallScore));

        onView(withText("Transfer to a friend")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withId(R.id.transferToFriendTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.sendToFriendButton)).check(matches(isDisplayed()));
        onView(withId(R.id.cancelButton)).check(matches(isDisplayed()));
        onView(withId(R.id.SourceRadioGroup)).check(matches(isDisplayed()));
        onView(withId(R.id.fieldEmailToTransfer)).check(matches(isDisplayed()));
        onView(withId(R.id.fieldAmountToTransfer)).check(matches(isDisplayed()));

        onView(withId(R.id.sendToFriendButton)).perform(click());

        //Check when nothing entered
        onView(withId(R.id.fieldEmailToTransfer))
                .check(matches(hasErrorText("Required.")));
        onView(withId(R.id.fieldAmountToTransfer))
                .check(matches(hasErrorText("Required.")));

        onView(withId(R.id.fieldEmailToTransfer))
                .perform(clearText())
                .perform(typeText("brezina.michaela@gmail.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.fieldAmountToTransfer))
                .perform(clearText())
                .perform(typeText("0"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.sendToFriendButton)).perform(click());

        //Check when invalid values entered
        onView(withId(R.id.fieldAmountToTransfer))
                .check(matches(hasErrorText("Cannot gift nothing.")));
        onView(withId(R.id.fieldEmailToTransfer))
                .check(matches(hasErrorText("Cannot give money to yourself.")));

        onView(withId(R.id.fieldAmountToTransfer))
                .perform(clearText())
                .perform(typeText("0"));
        Espresso.closeSoftKeyboard();

        //Check if dialog is closed on cancel button
        onView(withId(R.id.cancelButton)).perform(click());
        Thread.sleep(5000);
        onView(withText("Transfer to a friend")).check(doesNotExist());


    }

   /* private String getText(final Matcher<View> matcher) {
        final String[] stringHolder = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }*/
}