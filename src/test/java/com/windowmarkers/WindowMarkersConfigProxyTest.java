package com.windowmarkers;

import java.lang.reflect.*;
import java.util.*;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WindowMarkersConfigProxyTest
{
    /** Use RuneLite's actual handler and JDK proxy, without loading a user's profile. */
    static WindowMarkersConfig proxy(Map<String,String> saved) throws Exception
    {
        ConfigManager manager=mock(ConfigManager.class,CALLS_REAL_METHODS);
        doAnswer(call -> saved.get(call.getArgument(1))).when(manager).getConfiguration(eq("windowmarkers"),anyString());
        Constructor<?> constructor=Class.forName("net.runelite.client.config.ConfigInvocationHandler")
            .getDeclaredConstructor(ConfigManager.class);
        constructor.setAccessible(true);
        InvocationHandler handler=(InvocationHandler)constructor.newInstance(manager);
        return (WindowMarkersConfig)Proxy.newProxyInstance(WindowMarkersConfig.class.getClassLoader(),
            new Class<?>[]{WindowMarkersConfig.class},handler);
    }

    @Test public void everyConfigDefaultCanBeReadThroughRunelitesProxy() throws Exception
    {
        WindowMarkersConfig actual=proxy(Collections.emptyMap()),defaults=new WindowMarkersConfig(){};
        for(Method method:WindowMarkersConfig.class.getMethods()) if(method.isAnnotationPresent(ConfigItem.class))
            assertEquals(method.getName(),method.invoke(defaults),method.invoke(actual));
    }

    @Test public void activityModeRetainsEveryExistingStoredValue() throws Exception
    {
        Method method=WindowMarkersConfig.class.getMethod("geActivityFilter");
        assertEquals("geActivityFilter",method.getAnnotation(ConfigItem.class).keyName());
        for(Object value:method.getReturnType().getEnumConstants()) {
            WindowMarkersConfig config=proxy(Collections.singletonMap("geActivityFilter",((Enum<?>)value).name()));
            assertSame(value,config.geActivityFilter());
        }
    }
}
