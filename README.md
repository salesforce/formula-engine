[![CircleCI](https://circleci.com/gh/salesforce/formula-engine/tree/main.svg?style=svg)](https://circleci.com/gh/salesforce/formula-engine/tree/main)

# formula-engine
An implementation of a reusable formula engine with javascript &amp; sql generation along with Salesforce-approved syntax.

For the end-user documentation of the formula engine's functionality, please see the [end-user facing documentation](https://help.salesforce.com/articleView?id=sf.customize_functions.htm&type=5).

In order to implement this formula engine in your application, you need to
* Include grammaticus, formula-engine-api, and formula-engine-impl in your application.
* Look at FormulaEngineHooks, FormulaValidationHooks and implement the needed ones for your application during initialization.
* Think about your type system.  You'll need a `Class` that represents the type along with a FormulaDataType implementation
  that's used manage type conversion and error handling.  You'll need something like MockFormulaDataType in your application,
  but if you're only handling strings, it can be simple.
* Override the `FormulaFactoryImpl`, probably adding `FieldReferenceCommandInfo` and `DynamicReference` to the commands,
  along with whichever implementations of FormulaCommandInfo you need for your application.  
* Have a grammaticus localizer for any exception handling.  For the minimum work, copy the `MockLocalizerContext`
  included in the test code, set the `LocalizerFactory` at the start of your application.  You may want to override
  `FormulaValidationHooks.getLocalizer()` to return the same localizer.
* Implement a `FormulaTypeSpec` implementation that returns a valid getDefaultProperties, probably as an Enum.
  If you want different type of formulas in your application, make it an Enum like in` MockFormulaType`.  If you want
  javascript generated for mobile/offline use, you'll need to override that here.
* Create a `FormulaContext` that represents your data providers.  Make sure your root context implements
  `getProperty`, `setProperty`, `getFormulaReturnType`, `isFunctionSupported` as they will be called in all circumstances.
  Extending `BaseCompositeFormulaContext` is probably easisest for your Root/Default formula context.  Then
  `addContextProvider` for the various stuff you include
* The formula engine can parse hierarchies like `Contact.Account.Name`, but each formula context will get a chance
  to return the field at each point of the hierarchy, so you can override the results

* You can use different formula engines for compliation in `FormulaEngine.getFactory().create(...)` and then reuse that
  with different runtime contexts when you call `getFormula().evaluate(...)`.  This lets you reuse the FormulaInfo
  multiple times, as formula parsing is somewhat expensive.  If you want to handle the type conversions yourself,
  call `evaluateRaw`, not `evaluate` on the formulas.

* If you're using javascript, you need to have a few functions installed in the contexnt of the org, usually as `$F`.
  See `FormulaJsTestUtils.getFunctionScript()` for the examples.  You'll also want to load the included `decimal.js`
  if you want high precision decimals client side, suitable for currencies.
  
