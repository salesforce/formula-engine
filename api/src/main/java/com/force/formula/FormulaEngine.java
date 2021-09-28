/**
 *
 */
package com.force.formula;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Provides access to the set of hooks
 * @author stamm
 * @since 0.0.1
 */
public class FormulaEngine {

    private static final String[] JAVA_VERSION_PARTS = System.getProperty("java.version").split("\\.|_");

    private final static AtomicReference<FormulaEngineHooks> hooksRef = new AtomicReference<>();
    private final static AtomicReference<FormulaFactory> factoryRef = new AtomicReference<>();

    // Yes, we're using a static.  This should be overrideable in a test context in a less global way.
    public static FormulaEngineHooks getHooks() {
        return hooksRef.get();
    }

    public static FormulaEngineHooks setHooks(FormulaEngineHooks hooks) {
    	Preconditions.checkNotNull(hooks);
        return hooksRef.getAndSet(hooks);
    }

    public static FormulaFactory getFactory() {
        return factoryRef.get();
    }

    public static FormulaFactory setFactory(FormulaFactory factory) {
    	Preconditions.checkNotNull(factory);
        return factoryRef.getAndSet(factory);
    }

    private static final String FACTORYIMPLCLASSNAME = "com.force.formula.impl.FormulaFactoryImpl";

    // Get an invocation handler that will call the defaults for any interface.
    private static final Constructor<MethodHandles.Lookup> CONSTRUCTOR;

    static {
        if (Integer.parseInt(JAVA_VERSION_PARTS[0]) < 9) {
            try {
                CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
            if (!CONSTRUCTOR.canAccess(null)) {
                CONSTRUCTOR.setAccessible(true);
            }
        } else {
            CONSTRUCTOR = null;
        }
    }

    // If the hooks are overridden on FormulaValidationHooks, you need this to call the overridden method instead of the FormulaEngineHooks
    private final static AtomicReference<Class<?>> hooksClassRef = new AtomicReference<>();
    private static Set<String> VALIDATION_OVERRIDES = ImmutableSet.of("getSqlStyle", "getFormulaGeolocationService");
    
    // This calls the default handler on an interface, which is harder than it probably should be.
    private static InvocationHandler call_default_handler = (proxy, method, args) -> {
        if (method.isDefault()) {
            final Class<?> declaringClass = method.getDeclaringClass();
            if (CONSTRUCTOR != null) {
                return CONSTRUCTOR.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).unreflectSpecial(method, declaringClass).bindTo(proxy)
                        .invokeWithArguments(args);
            } else if (VALIDATION_OVERRIDES.contains(method.getName()) && hooksClassRef.get() != null) {
                return MethodHandles.lookup()
                        .findSpecial(hooksClassRef.get(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), hooksClassRef.get())
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            } else {
                return MethodHandles.lookup()
                        .findSpecial(declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), declaringClass)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            }
        }

        // proxy impl of not defaults methods
        return null;
    };

    private static InvocationHandler unsupported = (proxy, method, args) -> {
        throw new UnsupportedOperationException("Couldn't load " + FACTORYIMPLCLASSNAME + " into FactoryImpl\nInclude formula-engine-impl in your pom.xml or check your classloader.");
    };

    static {
        // TODO: Use a properties file to auto-instantiate this using reflection

        List<Class<?>> hooksClasses = new ArrayList<>();
        hooksClasses.add(FormulaEngineHooks.class);
        try {
        	Class<?> validationHooks = Class.forName("com.force.formula.impl.FormulaValidationHooks");
            hooksClasses.add(validationHooks);
            hooksClassRef.set(validationHooks);
        } catch (ClassNotFoundException e1) {
        	boolean isInTest = false;
			for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
				if (element.getClassName().startsWith("junit.framework.") ||
						element.getClassName().startsWith("org.junit.")) {
					isInTest = true;
					break;
				}
			}
			if (!isInTest) {
				throw new RuntimeException(e1);
			}
        }
        hooksRef.set((FormulaEngineHooks) Proxy.newProxyInstance(FormulaEngine.class.getClassLoader(), hooksClasses.toArray(new Class[hooksClasses.size()]), call_default_handler));

        try {
            factoryRef.set(Class.forName(FACTORYIMPLCLASSNAME).asSubclass(FormulaFactory.class).getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            // We don't have the impl... Don't die yet.  put in an invalid one.
            factoryRef.set((FormulaFactory) Proxy.newProxyInstance(FormulaEngine.class.getClassLoader(), new Class[]{FormulaFactory.class}, unsupported));
        }
    }

}
