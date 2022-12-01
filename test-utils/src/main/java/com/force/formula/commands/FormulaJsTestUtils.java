package com.force.formula.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.force.formula.Formula;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.util.FormulaDateUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

/**
 * Utilities for managing Javascript to evaluate formulas.
 * @author stamm
 * @since 0.1
 */
public class FormulaJsTestUtils {
    private static final Logger logger = Logger.getLogger("com.force.formula.js");
    
    private static final AtomicReference<FormulaJsTestUtils> INSTANCE = new AtomicReference<>(new FormulaJsTestUtils());
    
    /**
     * @return get the JsTestUtils
     */
    public static final FormulaJsTestUtils get() {
        return INSTANCE.get();
    }
    
    /**
     * param override set the JsTestUtils to use.
     */
    protected static void setTestUtils(FormulaJsTestUtils override) {
        INSTANCE.set(override);
    }
    
    private AtomicReference<Bindings> BINDINGS = new AtomicReference<>();
    private AtomicReference<ScriptEngine> BINDINGS_ENGINE = new AtomicReference<>();

    /**
     * @return a script engine with an appropriate global context for $Api so that we don't have to revaluate the
     *         functions constantly If you want to add stuff to bindings while running tests, just comment out the if
     *         and the closing brace
     */
    public ScriptEngine getScriptEngine() {
        return getGraalEngine();
    }

    public Object evaluateFormula(Formula formula, FormulaDataType columnType, FormulaContext context,
            Map<String, Object> jsMap) {
        return evaluateFormula(formula, columnType, context, jsMap, null);
    }

    /**
     * Evaluate the formula using Graal
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
    

    /**
     * Deal with Graal/JS eval -&gt; Internal Formula differences
     *
     * @param result the value returned from javascript
     * @param engine the javascript engine
     * @param columnType the type expected for the result
     * @return the value suitable for use in Java
     */
    public Object convertResultFromJs(Object result, ScriptEngine engine, FormulaDataType columnType) {
        return convertResultFromGraal(result, columnType);
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
    
    /**
     * @return the graaljs engine using the ScriptEngine (JSR 223) interface
     */
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
    
    /**
     * The context to be built, allowing for setting GraalJs options
     * @param builder the context buidler
     */
    protected void setContextOptions(Context.Builder builder) {
        // This is required to try mjs (see below)
        // builder.allowExperimentalOptions(true);
        // builder.option("js.esm-eval-returns-exports", "true");
    }
    
    /**
     * @return the graal context to use when evaluating a formula
     */
    public Context getGraalContext() {
        Context context = ROOT_CONTEXT.get();
        if (context == null) {
            Context.Builder builder = Context.newBuilder("js");
            builder.fileSystem(new JarFS());
            builder.allowIO(true);  // Needed to load modules
            builder.option("js.intl-402", "true"); // Support now ubiquitous Intl object for formatcurrency and the like.
            builder.option("engine.WarnInterpreterOnly", "false");  // Don't warn about being in openjdk with graaljs.

            setContextOptions(builder);

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
    
    /**
     * Convert an object from a Graal type to a formula type.  Mostly around date &amp; number convertion.
     * @param obj an object returned from graal
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
     * Trivial Truffle Filesystem that calls into {@link #makeJsFileFromResource(String, String, String)}.
     */
    protected static class JarFS implements FileSystem {
        protected JarFS() {
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