/**
 * Copyright 2017 www.infotel.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.aspects;

import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.server.DefaultDriverFactory;
import org.openqa.selenium.remote.server.DriverProvider;

import com.infotel.seleniumrobot.grid.CustomCapabilitiesComparator;

@Aspect
public class CapabilitiesComparatorAspect {
	
	@Around("call(static * org.openqa.selenium.remote.server.CapabilitiesComparator.getBestMatch (..))")
	public Object changeDriver(ProceedingJoinPoint joinPoint) throws Throwable {
		System.out.println("coucou2");
		
		Capabilities desired = (Capabilities)joinPoint.getArgs()[0];
		Collection<Capabilities> toCompare = (Collection<Capabilities>)joinPoint.getArgs()[1];
		
		return CustomCapabilitiesComparator.getBestMatch(desired, toCompare);
	}
//
//	@Around("call(* org.openqa.selenium.remote.server.DefaultDriverFactory.getProviderMatching (..))")
//	public Object changeDriver(ProceedingJoinPoint joinPoint) throws Throwable {
//		System.out.println("coucou2");
//		
//		Capabilities desired = (Capabilities)joinPoint.getArgs()[0];
//		DefaultDriverFactory factory = (DefaultDriverFactory)joinPoint.getTarget();
//		Field capabilitiesToDriverProviderField = DefaultDriverFactory.class.getDeclaredField("capabilitiesToDriverProvider");
//		Map<Capabilities, DriverProvider> capabilitiesToDriverProvider = (Map<Capabilities, DriverProvider>) capabilitiesToDriverProviderField.get(factory);
//		
//		// We won't be able to make a match if no drivers have been registered.
//	    checkState(!capabilitiesToDriverProvider.isEmpty(), "No drivers have been registered, will be unable to match %s", desired);
//	    Capabilities bestMatchingCapabilities = CustomCapabilitiesComparator.getBestMatch(desired, capabilitiesToDriverProvider.keySet());
//	    return capabilitiesToDriverProvider.get(bestMatchingCapabilities);
//		
//	}
}
