package com.infotel.seleniumrobot.grid.tests;

import org.mockito.MockitoAnnotations;
import org.mockito.quality.Strictness;
import org.mockito.testng.MockitoSettings;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

@Listeners({MockitoTestNGListener.class})
@MockitoSettings(strictness = Strictness.LENIENT)
public class BaseMockitoTest {

	@BeforeMethod(alwaysRun=true)  
	public void beforeMethod() throws Exception {
		MockitoAnnotations.initMocks(this); 
	}
}
