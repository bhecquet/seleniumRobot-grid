package com.infotel.seleniumrobot.grid.utils;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class SystemInfos {
	
	public static MemoryInfo getMemory() throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException, IntrospectionException {
		MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
		ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");		
		AttributeList list = mbs.getAttributes(name, new String[]{ "TotalPhysicalMemorySize", "FreePhysicalMemorySize" });
		
		if (list.isEmpty() || list.size() < 2) {
			return new MemoryInfo(-1, -1);
		}
		
		Attribute totalMemory = (Attribute)list.get(0);
		Attribute freeMemory = (Attribute)list.get(1);
		
		return new MemoryInfo((long)totalMemory.getValue(), (long)freeMemory.getValue());
	}
	
	public static double getCpuLoad() throws MalformedObjectNameException, InstanceNotFoundException, ReflectionException {
		MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
	    ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
	    AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });

	    if (list.isEmpty()) {
	    	return Double.NaN;
	    }

	    Attribute att = (Attribute)list.get(0);
	    Double value  = (Double)att.getValue();


	    // usually takes a couple of seconds before we get real values
	    if (value == -1.0) {
	    	return Double.NaN;
	    }
	    // returns a percentage value with 1 decimal point precision
	    return ((int)(value * 1000) / 10.0);
	}
	
	public static Rectangle getMainScreenResolution() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		return gd.getDefaultConfiguration().getBounds();
	}
}
