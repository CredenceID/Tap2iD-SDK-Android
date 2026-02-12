package com.credenceid.sample.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedViewModelTest {

    @Test
    fun getTitle_returnsNonEmptyString() {
        // We cannot easily instantiate SharedViewModel because it requires Application/Dependency Injection usually,
        // or just empty constructor if ViewModel() allows.
        // SharedViewModel() inherits ViewModel().
        val viewModel = SharedViewModel()
        
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        val title = viewModel.getTitle(Screen.HOME, context)
        
        assertNotNull(title)
        assertTrue(title.contains("Tap2iD-SDK"))
    }
}
