package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke-test screenshot that renders the app theme with a simple label.
 *
 * Note: this originally called a `Greeting(name: String)` composable from the
 * default Android Studio "Empty Activity" template. That composable was never
 * part of the Qusin Flashcard UI and does not exist in this codebase, which is
 * what broke `compileDebugUnitTestKotlin` (unresolved reference). Rather than
 * delete the test, it now renders a real, resolvable composable so the
 * MyApplicationTheme + Roborazzi screenshot pipeline is still exercised.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Surface {
          Text(text = "Qusin Flashcard", modifier = Modifier.padding(16.dp))
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
