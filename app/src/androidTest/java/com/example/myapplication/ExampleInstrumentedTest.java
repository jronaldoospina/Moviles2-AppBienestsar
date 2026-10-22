package com.example.myapplication;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See <a href="http://d.android.com/tools/testing">testing documentation</a>.
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        assertEquals("com.example.myapplication",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName());
    }
}
