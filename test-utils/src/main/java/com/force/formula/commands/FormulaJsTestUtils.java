package com.force.formula.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
        StringBuilder fContext = new StringBuilder();
        fContext.append("var $F={};").append("$F.nvl=function(a,b){return a!=null?a:b};")
                .append("$F.anl=function(a) {for (var i in a) {"
                        + "if (a[i] == null) { return true; } } return false; };")
                .append("$F.noe=function noe(value,ifNull)"
                        + "{if(value===undefined||value===null||value===''){return ifNull;}"
                        + "if(Array.isArray(value)){return value.length===0?ifNull:value;}"
                        + "else if(typeof value==='object'&&Object.prototype.toString.call(value)==='[object Object]'){return Object.keys(value).length===0?ifNull:value;}"
                        + "return value;};")
                .append("$F.lpad=function(a,b,c) {return !a||!b||b<1?null:(b<=a.length?a.substring(0,b):((Array(256).join(c)+'').substring(0,b-a.length))+a)};")

                // Note, these add months does what java does, but may not do what oracle/postgres does
                
                .append("$F.addmonths=function(a,b) {if (a==null||!b) return a;var d=new Date(a.getTime()+86400000);d.setUTCMonth(d.getUTCMonth()+Math.trunc(b));return new Date(d.getTime()-86400000);};\n")
                // Use this if you want to support fractional dates
        		//.append("$F.addmonths=function(a,b) {if (a==null||!b) return a;var d=new Date(a.getTime()+86400000);d.setUTCMonth(d.getUTCMonth()+Math.trunc(b));d.setUTCDate(d.getUTCDate()+Math.trunc((b%1)*365.24/12));return new Date(d.getTime()-86400000);};");

                // ISO week/day functions
        		.append("$F.isoweek=function(a) {if (!a) return a;"
        				+ "var d = new Date(Date.UTC(a.getFullYear(), a.getMonth(), a.getDate()));"
        				+ "var dayNum = d.getUTCDay() || 7;"
        				+ "d.setUTCDate(d.getUTCDate() + 4 - dayNum);"
        				+ "var yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));"
        				+ "return Math.ceil((((d - yearStart)/86400000) + 1)/7);}\n")
        		.append("$F.isoyear=function(a) {if (!a) return a;"
        				+ "var d = new Date(Date.UTC(a.getFullYear(), a.getMonth(), a.getDate()));"
        				+ "var dayNum = d.getUTCDay() || 7;"
        				+ "d.setUTCDate(d.getUTCDate() + 4 - dayNum);"
        				+ "var yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));"
        				+ "return yearStart.getUTCFullYear();}\n")
        		// Javascript doesn't have day of year either.
        		.append("$F.dayofyear=function(a) {if (!a) return a;"
        				+ "var d = new Date(Date.UTC(a.getFullYear(), a.getMonth(), a.getDate()));"
        				+ "var yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));"
        				+ "return Math.ceil((1+(d - yearStart))/86400000);}\n")
        		// Init cap... Normalize and use unicode to match postgres/oracle behavior
        		.append("$F.initcap=function(a) {if (!a) return a;"
        				+ "return a.toLowerCase().replace(/(?:^|[^\\p{Ll}\\p{Lm}\\p{Lu}\\p{N}])[\\p{Ll}]/gu, function (m) {return m.toUpperCase();})}\n")
        		;

        return fContext.toString();
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
                		result = new Date(val.invokeMember("getTime").asLong());
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

    public Context getGraalContext() {
        Context context = ROOT_CONTEXT.get();
        if (context == null) {
            Context.Builder builder = Context.newBuilder("js");
            builder.allowNativeAccess(true);
            builder.allowIO(true);
            builder.option("js.intl-402", "true"); // Support now ubiquitous Intl object for formatcurrency and the like.
            builder.option("engine.WarnInterpreterOnly", "false");  // Don't warn about being in openjdk with graaljs.
            context = builder.build();

            try {
                evalGraalContextGlobals(context);

                // Need native access for this. :-/.  It also requires a file on disk.
                InputStream decimalUrlStream = FormulaJsTestUtils.class.getClassLoader().getResourceAsStream("com/force/formula/decimal.js");
                byte[] buffer = ByteStreams.toByteArray(decimalUrlStream);
                File tmp = File.createTempFile("decimal", ".js");
                tmp.deleteOnExit();
                Files.write(buffer, tmp);
                context.eval("js", "load('" + tmp.getAbsolutePath() + "')");
                context.eval("js",  "Object.defineProperty($F, 'Decimal', { value : Decimal});");
                ROOT_CONTEXT.set(context);
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
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
    

    /**
     * Convert an object from a nashorn type to a formula type.  Mostly around date &amp; number convertion.
     * @param engine the nashorn script engine
     * @param obj an object returned from nashorn
     * @return a formula engine useful version.
     */
    @SuppressWarnings({ "removal", "deprecation" })
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
        
        if (obj instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
            jdk.nashorn.api.scripting.ScriptObjectMirror mirror = (jdk.nashorn.api.scripting.ScriptObjectMirror) obj;
            if (mirror.hasMember("getTime")) {
                // It's a ScriptObjectMirror with 'time'.  Assume date
                JsDateWrapper wrapper = ((Invocable)engine).getInterface(obj, JsDateWrapper.class);
                return new Date(wrapper.getTime());
            } else {
                return new BigDecimal((String)mirror.callMember("toString"));
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
}