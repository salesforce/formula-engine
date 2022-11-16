package com.force.formula.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.coveo.nashorn_modules.Require;
import com.coveo.nashorn_modules.ResourceFolder;
import com.force.formula.Formula;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.util.FormulaDateUtil;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class FormulaJsTestUtils {
    private static final Logger logger = Logger.getLogger("com.force.formula.js");
    
    private static final AtomicReference<FormulaJsTestUtils> INSTANCE = new AtomicReference<>(new FormulaJsTestUtils());
    
    public static final FormulaJsTestUtils get() {
        return INSTANCE.get();
    }
    
    protected static void setTestUtils(FormulaJsTestUtils override) {
        INSTANCE.set(override);
    }
    
    /**
     * Whether we are in GraalVM or not.  This is set to true to use graaljs even on openjdk
     */
    //public final static boolean IS_GRAAL = System.getProperty("java.vendor.version", "").contains("GraalVM");
	public final static boolean IS_GRAAL = Boolean.TRUE;

    private AtomicReference<Bindings> BINDINGS = new AtomicReference<>();
    private AtomicReference<ScriptEngine> BINDINGS_ENGINE = new AtomicReference<>();

    /**
     * @return a script engine with an appropriate global context for $Api so that we don't have to revaluate the
     *         functions constantly If you want to add stuff to bindings while running tests, just comment out the if
     *         and the closing brace
     */
    public ScriptEngine getScriptEngine() {
        if (IS_GRAAL) {
            return getGraalEngine();
        } else {
            return getNashornEngine();
        }
    }

    public Object evaluateFormula(Formula formula, FormulaDataType columnType, FormulaContext context,
            Map<String, Object> jsMap) {
        return evaluateFormula(formula, columnType, context, jsMap, null);
    }

    /**
     * Evaluate the formula using Nashorn or Graal
     *
     * @param formula
     *            the formula to evaluate in javascript
     * @param columnType
     *            the column type of the result
     * @param context
     *            the formula context. Used for determining number precision
     * @param jsMap
     *            the "context" map. Will be in the global map as "constant". Should contain a "record" value, but could
     *            have other stuff.
     * @param globalMap
     *            the "global" map. Should have cached and constant things like "$User" that would be stored locally and
     *            semi-permanently.
     * @return the result of evaluating the formula in javascript
     */
    public Object evaluateFormula(Formula formula, FormulaDataType columnType, FormulaContext context,
            Map<String, Object> jsMap, Map<String, Object> globalMap) {
        // Establish the context for the Javascript engine
   	
    	if (IS_GRAAL) {
            Context jsContext = getGraalContext();
            if (jsMap != null) {
                Object graaled = convertToGraal(jsContext, jsMap, context);
                jsContext.getBindings("js").putMember("context", graaled);
            }
            if (globalMap != null) {
                globalMap.entrySet().stream().forEach((e) -> jsContext.getBindings("js").putMember(e.getKey(),
                        convertToGraal(jsContext, e.getValue(), context)));
            }
            Value result = jsContext.eval("js", formula.toJavascript());
            return convertResultFromGraal(result, columnType);
        } else {
            Object result;
            try {
                ScriptEngine engine = getScriptEngine();
                if (jsMap != null) {
                    convertToNashorn(engine, jsMap, context);
                    engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("context", jsMap);
                }
                if (globalMap != null) {
                    globalMap.entrySet().stream().forEach((e) -> engine.getBindings(ScriptContext.GLOBAL_SCOPE)
                            .put(e.getKey(), convertToNashorn(engine, e.getValue(), context)));
                }
                result = engine.eval(formula.toJavascript());
                return convertResultFromJs(result, engine, columnType);
            } catch (ScriptException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * @return a string to create a "$Api" variable for evaluating FormulaEngine helper functions.
     */
    protected String getApiContextScript() {
        return null;
    }

    /**
     * @return a string to create a "$F" variable for evaluating FormulaEngine helper functions.
     */
    protected String getFunctionScript() {
        return "$F;";  // Everything should be loaded from formulaEngine.js above
    }

    /**
     * @return a string to create a "$System" variable for evaluating SystemFormulaContext
     */
    protected String getSystemContextScript() {
        // Setting the $System.originDateTime for FormulaGenericTests - Account - testOriginDateTime
        StringBuilder systemContext = new StringBuilder();
        systemContext.append("var $System={};").append("$System.OriginDateTime= '1900-01-01 00:00:00.000';"); // for
                                                                                                             // testOriginTime
        return systemContext.toString();
    }
    
    // Nashorn only
    String getLowerUpperScript() {
        // Nashorn only
        StringBuilder lowerUpper = new StringBuilder();
        lowerUpper.append(
                "String.prototype.toLocaleUpperCase = function(s){ if (!s.startsWith('tr')) return toUpperCase(this);"
                        + "var string = this;"
                        + "var letters = { \"i\": \"İ\", \"ş\": \"Ş\", \"ğ\": \"Ğ\", \"ü\": \"Ü\", \"ö\": \"Ö\", \"ç\": \"Ç\", \"ı\": \"I\" };"
                        + "string = string.replace(/(([iışğüçö]))+/g, function(letter){ return letters[letter]; });"
                        + "return string.toUpperCase();};");
        lowerUpper.append(
                "String.prototype.toLocaleLowerCase = function(s){ if (!s.startsWith('tr')) return toLowerCase(this);"
                        + "var string = this;"
                        + "var letters = { \"İ\": \"i\", \"I\": \"ı\", \"Ş\": \"ş\", \"Ğ\": \"ğ\", \"Ü\": \"ü\", \"Ö\": \"ö\", \"Ç\": \"ç\" };"
                        + "string = string.replace(/(([İIŞĞÜÇÖ]))+/g, function(letter){ return letters[letter]; });"
                        + "return string.toUpperCase();};");
        lowerUpper.append("String");
        return lowerUpper.toString();
    }

    public ScriptEngine getNashornEngine() {
        Bindings bindings = BINDINGS.get();
        if (bindings == null) {
            // Create an engine just to compile the $AP
            ScriptEngine compEngine = new ScriptEngineManager().getEngineByName("JavaScript");
            BINDINGS_ENGINE.set(compEngine);


            try {
                bindings = compEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
                makeNashornBindings(compEngine, bindings);
                BINDINGS.set(bindings);
            } catch (ScriptException x) {
                logger.log(Level.INFO, "Cannot compute generic bindings", x);
                // Ignore script exception for now, and recreate it everytime
            }
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        return engine;
    }
    
    public ScriptEngine getBindingsEngineForUnwrappingStuff() {
        return BINDINGS_ENGINE.get();
    }

    /**
     * Deal with Nashorn eval -&gt; Internal Formula differences
     *
     * @param result the value returned from javascript
     * @param engine the javascript engine
     * @param columnType the type expected for the result
     * @return the value suitable for use in Java
     */
    public Object convertResultFromJs(Object result, ScriptEngine engine, FormulaDataType columnType) {
        if (IS_GRAAL) {
            return convertResultFromGraal(result, columnType);
        } else {
            return convertResultFromNashorn(result, engine, columnType);
        }
    }

    /**
     * Deal with Nashorn eval -&gt; Internal Formula differences
     *
     * @param result the value returned from javascript
     * @param engine the javascript engine
     * @param columnType the type expected for the result
     * @return the value suitable for use in Java
     */
    public Object convertResultFromNashorn(Object result, ScriptEngine engine, FormulaDataType columnType) {
        try {
            result = convertFromNashorn(engine, result);
        } catch (IllegalArgumentException ex) {
            // If the date object is returned from a function in the bindings, it, uh, freaks out, because the engines
            // are different
            // This uses a hidden variable. Annoying, but necessary. That's also why this is in test code. Sorry.
            result = convertFromNashorn(getBindingsEngineForUnwrappingStuff(),
                    result);
        }
        if (columnType.isDateTime() && result instanceof Date) {
            result = new FormulaDateTime((Date)result);
        }
        if (columnType.isTimeOnly() && result instanceof Date) {
            result = FormulaEngine.getHooks().constructTime(((Date)result).getTime());
        }
        return result;
    }

    /**
     * Deal with Graal eval -&gt; Internal Formula differences
     *
     * @param result the value returned from graaljs
     * @param columnType the expected columntype
     * @return the value suitable for use in Java
     */
    public Object convertResultFromGraal(Object result, FormulaDataType columnType) {
        if (result instanceof Value) {
            Value val = (Value)result;
            if (val.isHostObject()) {
                result = val.asHostObject();
            } else {
                String type = val.getMetaObject().toString();
                switch (type) {
                case "null":
                    return null;
                case "boolean":
                    return val.asBoolean();
                case "number":
                    result = val.asDouble();
                    break;
                case "string":
                    String strVal = val.asString();
                    return (!"".equals(strVal)) ? strVal : null; // Oracle compatibility. Sorry.
                // Old GraalVM
                case "Date":
                    try {
                        result = FormulaDateUtil.parseISO8601(result.toString());
                    } catch (ParseException x) {
                        throw new RuntimeException(x);
                    }
                    break;
                case "undefined":
                    // I'm not sure this is right, but missing fields end up as undefined, not null in JS.
                    return null;
                case "Object":
                    return new BigDecimal(val.invokeMember("toNumber").toString());
                default:
                	// New GraalVM
                	if (val.hasMember("getTime")) {
                	    Value time = val.invokeMember("getTime");
                	    if (time.fitsInLong()) {
                	        result = new Date(time.asLong());
                	    } else {
                	        // Invalid date has NaN for its getTime.
                	        throw new IllegalArgumentException("Javascript cannot parse time");
                	    }
                	} else if (val.hasMember("toNumber")) {
                        return new BigDecimal(val.invokeMember("toNumber").toString());
                	} else {
                		throw new UnsupportedOperationException(type);
                	}
                }
            }
        }
        try {
            result = convertFromGraal(result, columnType);
        } catch (IllegalArgumentException ex) {
            throw ex;
        }
        if (columnType.isDateTime() && result instanceof Date) {
            result = new FormulaDateTime((Date)result);
        }
        if (columnType.isTimeOnly() && result instanceof Date) {
            result = FormulaEngine.getHooks().constructTime(((Date)result).getTime());
        }
        return result;
    }
    
    public ScriptEngine getGraalEngine() {
        Bindings bindings = BINDINGS.get();
        if (bindings == null) {
            ScriptEngine compEngine = new ScriptEngineManager().getEngineByName("graal.js");
            BINDINGS_ENGINE.set(compEngine);

            try {
                bindings = compEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
                makeGraalBindings(compEngine, bindings);
                BINDINGS.set(bindings);
            } catch (ScriptException x) {
                logger.log(Level.CONFIG, "Cannot evaluate graal bindings", x);
                // Ignore script exception for now, and recreate it everytime
            }
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        return engine;
    }
    
    /**
     * Bind the script to the given bindings.  Evals the script and then the global and then binds.
     * @param compEngine the scriptengine to use to evaluate
     * @param bindings the bindings to assign the global variable to
     * @param eval the script that creates the global
     * @param global the global variable, "usually $..."
     * @throws ScriptException if an exception occurs during binding
     */
    protected void bindScriptEngineGlobal(ScriptEngine compEngine, Bindings bindings, String eval, String global) throws ScriptException {
        if (eval != null) {
            bindings.put(global, compEngine.eval(eval, bindings) + "\n" + global);
        }
    }
    
    /**
     * Bind the global script to the given bindings.  Calls bindScriptEngineGlobal for
     * $F, $Api, and $System.
     * @param compEngine the scriptengine to use to evaluate
     * @param bindings the bindings to assign the global variables to (global scope)
     * @param engineBindings the bindings with engine scope.  Not sure it's needed, but if you don't bind modules, it causes issues.
     * @throws ScriptException if an exception occurs during binding
     */
    protected void bindScriptEngineGlobals(ScriptEngine compEngine, Bindings bindings, Bindings engineBindings)  throws ScriptException {
        bindScriptEngineGlobal(compEngine, bindings, getApiContextScript(), "$Api");
        bindScriptEngineGlobal(compEngine, bindings, getFunctionScript(), "$F");
        bindScriptEngineGlobal(compEngine, bindings, getSystemContextScript(), "$System");
    }
    
    /**
     * Set the global context for nashorn evaluation 
     * @param compEngine the scriptengine to use to evaluate
     * @param bindings the bindings to assign the global variables to (global scope)
     * @throws ScriptException if an exception occurs during binding
     */
    protected void makeNashornBindings(ScriptEngine compEngine, Bindings bindings) throws ScriptException {
        Bindings engineBindings = compEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindScriptEngineGlobals(compEngine, bindings, engineBindings);
        bindings.put("module", compEngine.eval("new Object()"));
        bindings.put("String", compEngine.eval(getLowerUpperScript()));

        ResourceFolder folder = ResourceFolder.create(FormulaJsTestUtils.class.getClassLoader(), "com/force/formula",
                Charsets.UTF_8.toString());
        // We want to load Decimal as a module into the bindngs, but the coveo stuff's interface is
        // weird and requires access to the NashornScriptEngine directly. That's not cool.
        for (Method m : Require.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 3) {
                assert "enable".equals(m.getName()); // Avoid referencing NashornScriptEngine directly to avoid
                                                     // "restriction" error
                try {
                    m.invoke(null, compEngine, folder, bindings);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                    throw new RuntimeException(x);
                }
            }
        }
        Object decimal = compEngine.eval("require('./decimal.js')", engineBindings);
        engineBindings.put("Decimal", decimal);
        bindings.put("$F.Decimal", compEngine.eval("Object.defineProperty($F, 'Decimal', { value : Decimal});"));        
    }
    
    /**
     * Set the global context for graal evaluation  using the javax.script.ScriptEngine 
     * Avoid it
     * @param compEngine the scriptengine to use to evaluate
     * @param bindings the bindings to assign the global variables to (global scope)
     * @throws ScriptException if an exception occurs during binding
     */
    protected void makeGraalBindings(ScriptEngine compEngine, Bindings bindings) throws ScriptException {
        Bindings engineBindings = compEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindScriptEngineGlobals(compEngine, bindings, engineBindings);
        
        // Need native access for this. :-/
        // URL decimalUrl = FormulaJsTestUtils.class.getClassLoader().getResource("com/force/formula/decimal.js");
        // Object decObj = compEngine.eval("load('"+new File(decimalUrl.toURI()).getAbsolutePath()+"')");
        try (Reader decimal = new InputStreamReader(
                FormulaJsTestUtils.class.getClassLoader().getResourceAsStream("com/force/formula/decimal.js"))) {
            compEngine.eval(decimal);
            Object decObj = compEngine.eval("Decimal");
            engineBindings.put("Decimal", decObj);
            bindings.put("$F.Decimal", compEngine.eval("Object.defineProperty($F, 'Decimal', { value : Decimal});"));
        } catch (IOException e) {
            logger.log(Level.CONFIG, "Cannot load decimal.js", e);
        }             
    }

    

    
    /**
     * Evaluate the global context for Graal.  Doesn't need the same rigamarole as ScriptEngine
     * $F, $Api, and $System.  Override this to load other modules for testing
     * @param context the bindings to assign the global variables to
     */
    protected void evalGraalContextGlobals(Context context) {
        context.eval("js", getFunctionScript());
        if (getApiContextScript() != null) {
            context.eval("js", getApiContextScript());
        }
        if (getSystemContextScript() != null) {
            context.eval("js", getSystemContextScript());
        }
    }
    

    private AtomicReference<Context> ROOT_CONTEXT = new AtomicReference<>();

    /**
     * Load the file from the resource and put it on disk, so graalvm can load it normally.
     * @param resourcePath the path relative to this classloader
     * @param tmpName the tmpName to use for the jsfile
     * @param ext the extension to use, like '.js'
     * @return a file that will be deleted on exit
     * @throws IOException if the file can't be found.
     */
    private static File makeJsFileFromResource(String resourcePath, String tmpName, String ext) throws IOException {
        InputStream decimalUrlStream = FormulaJsTestUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        byte[] buffer = ByteStreams.toByteArray(decimalUrlStream);
        File tmp = File.createTempFile(tmpName, ext);
        tmp.deleteOnExit();
        Files.write(buffer, tmp);
        return tmp;
    }
    
    public Context getGraalContext() {
        Context context = ROOT_CONTEXT.get();
        if (context == null) {
            Context.Builder builder = Context.newBuilder("js");
            builder.fileSystem(new JarFS());
            builder.allowIO(true);  // Needed to load modules
            builder.option("js.intl-402", "true"); // Support now ubiquitous Intl object for formatcurrency and the like.
            builder.option("engine.WarnInterpreterOnly", "false");  // Don't warn about being in openjdk with graaljs.

            // This is required to try mjs (see below)
            // builder.allowExperimentalOptions(true);
            // builder.option("js.esm-eval-returns-exports", "true");

            context = builder.build();

            context.eval("js", "load('com/force/formula/formulaEngine.js'); var $F = FormulaEngine;");

            // You can't redefine stuff in modules, and you need the options above, and then it's still questionable
            //File tmp = makeJsFileFromResource("com/force/formula/formulaEngine.mjs", "formulaEngine", ".mjs");
            //Value feModule = context.eval(Source.newBuilder("js", "import {FormulaEngine} from '" + tmp.getAbsolutePath() + "'; FormulaEngine;", "test.mjs").build());
            //context.getBindings("js").putMember("$F", feModule);
            //context.getBindings("js").putMember("FormulaEngine", feModule);
            
            // Allow overrides
            evalGraalContextGlobals(context);
            context.eval("js", "load('com/force/formula/decimal.js')");
            context.eval("js",  "Object.defineProperty($F, 'Decimal', { value : Decimal});");
            
            context.eval("js", "load('com/force/formula/jsonpath.js')");
            context.eval("js",  "Object.defineProperty($F, 'jsonPath', { value : jsonPath});");

            ROOT_CONTEXT.set(context);
        }
        return context;
    }

    /**
     * Convert an object from a formula type to a graal type. Mostly to fix dates
	 *
     * @param jsContext the graal context
     * @param obj 
     *            an object returned from formulas
     * @param context  the formula context 
     * @return a graal useful version.
     */
    @SuppressWarnings("unchecked")
    public Object convertToGraal(Context jsContext, Object obj, FormulaContext context) {
        if (obj == null) { return null; }
        if (obj instanceof Date) {
            return jsContext.eval("js", "new Date(" + ((Date)obj).getTime() + ")");
        } else if (obj instanceof FormulaDateTime) {
            return jsContext.eval("js", "new Date(" + ((FormulaDateTime)obj).getTime() + ")");
        } else if (obj instanceof Number) {
            if (context.useHighPrecisionJs()) {
                return jsContext.eval("js", "new $F.Decimal(" + ((Number)obj).toString() + ")");
            } else if (obj instanceof BigDecimal) { return jsContext.eval("js", ((Number)obj).toString()); }
        } else if (obj instanceof Map) {
            // This'll help convert deep objects. We'll do it in place to keep the cost down.
            ((Map<String, Object>)obj).entrySet().stream()
                    .forEach((e) -> e.setValue(convertToGraal(jsContext, e.getValue(), context)));
            return ProxyObject.fromMap((Map<String, Object>)obj);
        }
        return jsContext.asValue(obj);
    }

    /**
     * Convert an object with values to match what those values would look like on the client
     *
     * @param apiMap
     *            - the object
     * @return the apiMap with all values converted to javascript format (mostly formatting dates)
     */
    public Map<String, Object> makeJSMap(Map<String, Object> apiMap) {
        Map<String, Object> jsMap = new HashMap<>();
        SimpleDateFormat apiDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for (String field : apiMap.keySet()) {
            if (apiMap.get(field) instanceof Date) {
                jsMap.put(field, apiDf.format((Date)apiMap.get(field)));
            } else {
                jsMap.put(field, apiMap.get(field));
            }
        }
        return jsMap;
    }

    
    // Nashorn was removed in JDK 17, but this is here to support 11.
    private static final Class<?> SCRIPT_OBJECT_MIRROR;
    private static final MethodHandle SOM_HASMEMBER;
    private static final MethodHandle SOM_CALLMEMBER;
    static
    {
        Class<?> som;
        MethodHandle hasMember;
        MethodHandle callMember;
        try {
            som = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
            hasMember = MethodHandles.publicLookup().findVirtual(som, "hasMember", MethodType.methodType(String.class));
            callMember = MethodHandles.publicLookup().findVirtual(som, "callMember", MethodType.methodType(String.class, Object[].class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException ex) {
            som = null;
            hasMember = null;
            callMember = null;
        }
        SCRIPT_OBJECT_MIRROR = som;
        SOM_HASMEMBER = hasMember;
        SOM_CALLMEMBER = callMember;
    }
    

    /**
     * Convert an object from a nashorn type to a formula type.  Mostly around date &amp; number convertion.
     * @param engine the nashorn script engine
     * @param obj an object returned from nashorn
     * @return a formula engine useful version.
     */
    public Object convertFromNashorn(ScriptEngine engine, Object obj) {
        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                return obj;
            } else if (obj instanceof Double) {
                double val = ((Number)obj).doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) return null;
                if (val == 0) return BigDecimal.ZERO;
                if (val == 1) return BigDecimal.ONE;
                return new BigDecimal(val).setScale(7, RoundingMode.HALF_EVEN).stripTrailingZeros(); // NOPMD
            } else {
                return new BigDecimal(((Number)obj).intValue());
            }
        }
        
        if (SCRIPT_OBJECT_MIRROR != null && SCRIPT_OBJECT_MIRROR.isInstance(obj)) {
            try {
                if ((Boolean) SOM_HASMEMBER.invoke(obj, "getTime")) {
                    // It's a ScriptObjectMirror with 'time'.  Assume date
                    JsDateWrapper wrapper = ((Invocable)engine).getInterface(obj, JsDateWrapper.class);
                    return new Date(wrapper.getTime());
                } else {
                    return new BigDecimal((String) SOM_CALLMEMBER.invoke(obj, "toString", null));
                }
            } catch (Throwable e) {
                logger.log(Level.FINEST, "Can't convert from nashorn", e);
            }
        }
        if ("".equals(obj)) return null;  // Oracle compatibility
        return obj;
    }
    
    /**
     * Required implementation for Nashorn.  To be removed.
     * @author stamm
     * @since 0.0.2
     */
    public interface JsDateWrapper {
        long getTime();
        int getTimezoneOffset();
    }
    
    
    /**
     * Convert an object from a Graal type to a formula type.  Mostly around date &amp; number convertion.
     * @param obj an object returned from nashorn
     * @param type the expected type of the value
     * @return a formula engine useful version.
     */
    public Object convertFromGraal(Object obj, FormulaDataType type) {
        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                return obj;
            } else if (obj instanceof Double) {
                double val = ((Number)obj).doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) return null;
                if (val == 0) return BigDecimal.ZERO;
                if (val == 1) return BigDecimal.ONE;
                BigDecimal result = new BigDecimal(val).setScale(7, RoundingMode.HALF_EVEN).stripTrailingZeros();  // NOPMD
                if (result.scale() < 0) {
                    result = result.setScale(0);  // No 3E+2
                }
                return result;
            } else {
                return new BigDecimal(((Number)obj).intValue());
            }
        }
        
        if (obj instanceof Map) {
            if (type.isDate() || type.isTimeOnly()) {
                // Get the tostring and parse it
                try {
                    return FormulaDateUtil.parseISO8601(obj.toString());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            } else if (type.isNumber()) {
                return new BigDecimal(obj.toString());
            } 
        }
        if ("".equals(obj)) return null;  // Oracle compatibility
        return obj;
    }  
    
    /**
     * Convert an object from a formula type to a nashorn type.  Mostly to fix dates
     * @param engine the nashorn script engine
     * @param obj an object returned from formulas
     * @param context the formula context
     * @return a nashorn useful version.
     */
    @SuppressWarnings("unchecked")
    public Object convertToNashorn(ScriptEngine engine, Object obj, FormulaContext context) {
        try {
            if (obj instanceof Date) {
                return engine.eval("new Date("+((Date)obj).getTime()+")");
            } else if (obj instanceof FormulaDateTime) {
                return engine.eval("new Date("+((FormulaDateTime)obj).getTime()+")");
            } else if (obj instanceof Number && context.useHighPrecisionJs()) {
                return engine.eval("new $F.Decimal("+((Number)obj).toString()+")");
            } else if (obj instanceof Map) {
                // This'll help convert deep objects.  We'll do it in place to keep the cost down.
                ((Map<?,Object>)obj).entrySet().stream().forEach((e)->e.setValue(convertToNashorn(engine, e.getValue(), context)));
            }
            // TODO: Convert BigDecimal -> Double?
        } catch (ScriptException x) {
            logger.log(Level.FINE, "Cannot convert to nashorn", x);
            // Ignore it for now
        }
        return obj;
    }
    
    /**
     * Trivial Truffle Filesystem that calls into {@link #makeJsFileFromResource(String, String, String)}.
     */
    protected static class JarFS implements FileSystem {
        JarFS() {
        }
        @Override
        public Path parsePath(URI uri) {
            return Paths.get(uri);
        }
        @Override
        public Path parsePath(String path) {
            return Paths.get(path);
        }
        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) {
        }
        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) {
            throw new AssertionError();
        }
        @Override
        public void delete(Path path) {
            throw new AssertionError();
        }
        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            String resource = path.toString();
            File f = makeJsFileFromResource(resource, "temp", resource.substring(resource.lastIndexOf('.')));
            return FileChannel.open(f.toPath(), StandardOpenOption.READ);
        }
        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
            throw new AssertionError();
        }
        @Override
        public Path toAbsolutePath(Path path) {
            return path.toAbsolutePath();
        }
        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) {
            return path;
        }
        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
            // Size isn't really used.
            return ImmutableMap.of("isRegularFile",  Boolean.TRUE,
                    "size", 0L);
        }
    }
}