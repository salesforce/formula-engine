/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import com.force.formula.*;
import com.force.formula.FormulaRuntimeContext.InaccessibleFieldStrategy;
import com.force.formula.commands.*;
import com.force.formula.parser.gen.SfdcFormulaTokenTypes;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.util.FormulaTextUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Utf8;

import antlr.Token;

/**
 * @author dchasman
 * @since 140
 */
public abstract class BaseFormulaInfoImpl implements RuntimeFormulaInfo {
    public BaseFormulaInfoImpl(FormulaContext context, String source, FormulaProperties properties) throws FormulaException {
        this(context, source, properties, false, false,false);
    }

    public BaseFormulaInfoImpl(FormulaContext context, String originalSource, FormulaProperties properties,
            boolean existingFormula, boolean forceDisabled, boolean isCreateOrEditFormula) throws FormulaException {
        long startTime = System.currentTimeMillis();

        this.context = context;
        this.properties = properties;
        
        try {
            boolean isCheckingSqlLengthLimit = (isCreateOrEditFormula && !FormulaValidationHooks.get().parseHook_ignoreSqlTextLengthLimit());
            this.context.setProperty(FormulaContext.CHECK_SQL_LENGTH_LIMIT, isCheckingSqlLengthLimit);
        
            // Propagating the info if FormulaInfo is being created for Runtime/Design time.
            this.properties.setExistingFormula(existingFormula);
    
            String source = FormulaUtils.decode(context, originalSource, properties);
            if (forceDisabled) this.properties.setDisabled(true);
    
            if (source == null) {
                source = "";
            } else {
                source = autoStripCurlyBangs(source, this.properties);
            }
    
            if (source.length() > MAX_FORMULA_LENGTH && !properties.getParseAsTemplate() && properties.getCheckDecodedSize()) {
                throw new FormulaTooLongException(source.length(), MAX_FORMULA_LENGTH);
            }
            this.source = source;
    
            // if the formula is disabled, make it an error. Bug: 137381
            if (this.properties.isDisabled()) {
                this.formula = new InvalidFormula(FormulaValidationHooks.get().parseHook_getFormulaDisabledException());
                this.sqlSize = null;
                this.fieldReferences = new ContextualFormulaFieldInfo[0];
                this.referenceEncryptedFields = false;
            } else {
                FormulaAST astRoot = FormulaUtils.parse(this.source, properties);
    
                if (!properties.getAllowCycles()) {
                    FormulaValidationHooks.get().parseHook_checkForCycles(context, (String) context.getProperty(FIELD_NAME_OVERRIDE), getReferencedNames(astRoot, properties));
                }
    
                FormulaAST ast = (FormulaAST) astRoot.getFirstChild();
                
                // Handle inaccessible fields before annotating to prevent treatNullAsNull setting from interfering
                handleInaccessibleFields(ast, context);
                
                AnnotationVisitor annotationVisitor = annotateAst(ast, context, properties);
                ast = enrichRoot(ast, context, properties);
    
                // Finish annotation
                fieldReferences = annotationVisitor.getFieldReferences();
    
                if (isCreateOrEditFormula) {
                    this.referenceEncryptedFields = FormulaValidationHooks.get().parseHook_hasEncryptedDataReferences(context, ast, properties);
                } else {
                    this.referenceEncryptedFields = false;
                }
    
                BitSet propertyBits = new BitSet(NUM_PROPERTIES);
    
                // This has utility value, really only if we trying to generate SQL or operating
                // on the results in bulk.
                // That is generally not true for templates.
                if (!properties.isDisabled() && !properties.getParseAsTemplate()) {
                    ast = optimizeParseTree(ast,context,propertyBits,properties);
                }
    
                List<FormulaCommand> commandList = new LinkedList<FormulaCommand>();
                propertyBits.or(generate(ast, commandList, context, properties));
                boolean producesHTML = propertyBits.get(PRODUCES_HTML_INDEX);
                boolean getsSessionId = propertyBits.get(GETS_SESSION_ID_INDEX);
    
                TableAliasRegistry registry = new TableAliasRegistry();
                SQLPair sqlPair;
    
                if (properties.getGenerateSQL()) {
                    sqlPair = visitPostorder(ast, context, registry);
                    this.sqlSize = calculateSqlSize(sqlPair, isCheckingSqlLengthLimit, this.properties.getMaxSqlSize());
                    if (isCheckingSqlLengthLimit) {
                        // SQL generated for length limit checks must be discarded
                        sqlPair = new SQLPair(null, null);
                    }
                } else {
                    sqlPair = new SQLPair(null, null);
                    this.sqlSize = null;
                }
    
                // Log if it is greater than the Maximum Nested Case
                if (isCreateOrEditFormula) {
                    FormulaValidationHooks.get().parseHook_validateNestedIf(astRoot, source, sqlPair);
                }
    
                JsValue javascript;
                if (properties.getGenerateJavascript() || FormulaValidationHooks.get().parseHook_generateJavascript()) {
                    if (properties.getGenerateJavascript()) {
                        assert context.getGlobalProperties().getFormulaType().getDefaultProperties()
                                .getGenerateJavascript() : "Formula type must be enabled for JavaScript generation";
                    }
                    final long start = System.currentTimeMillis();
                    try {
                        javascript = visitJavascript(ast, context);
                        if (isCreateOrEditFormula && javascript != null && javascript.js.length() > MAX_JS_SIZE) {
                            throw new JSTooBigException(javascript.js.length(), true);
                        }
                    } catch (JSTooBigException e) { // NOPMD
                        // Log formulas that have been stopped mid-generation.
                        LogInfo logInfo = getLogInfo(astRoot, properties);
                        FormulaValidationHooks.get().parseHook_logOfflineFormula(start, e.getSize(), e.isFinished(),
                                logInfo.commands);
                        throw e;
                    }
                } else {
                    javascript = JsValue.NULL;
                }
    
                boolean usesCustomFields = false;
                for (FormulaFieldInfo formulaFieldInfo : getReferences()) {
                    if ((formulaFieldInfo instanceof FormulaProvider)
                        && (((FormulaProvider)formulaFieldInfo).getFormula() != null)) {
                        Formula formula = ((FormulaProvider)formulaFieldInfo).getFormula();
                        if (formula.hasAttribute(Formula.REFERENCES_CUSTOM_FIELDS)) {
                            usesCustomFields = true;
                        }
                    } else if (formulaFieldInfo.isCustom()) {
                        usesCustomFields = true;
                    }
                }
    
                this.formula = new FormulaImpl(commandList.toArray(new FormulaCommand[commandList.size()]), sqlPair.sql,
                    sqlPair.guard, javascript, properties, context.getFormulaReturnType(), ast.getDataType(), producesHTML, getsSessionId,
                    usesCustomFields, registry);
    
                logSuccess(startTime, isCreateOrEditFormula, source, astRoot, javascript);
            }
        } catch (Exception e) {
            logFailure(startTime, isCreateOrEditFormula, originalSource, e);
            throw e;
        }
    }

    /**
     * Logs a successful formula parsing
     */
    private void logSuccess(long startTime, boolean isCreateOrEditFormula, String source,
            FormulaAST astRoot, JsValue javascript) {
        // If it's create or edit we always log, during runtime we sample.
        if (isCreateOrEditFormula || FormulaEngine.getHooks().shouldLogRuntime()) {
        	LogInfo logInfo = getLogInfo(astRoot, properties);
   
            if (isCreateOrEditFormula) {
                FormulaEngine.getHooks().logFormulaDesignTime(startTime, source.length(),
                        logInfo.commands,
                        logInfo.globalVariables,
                        logInfo.hasPolymorphicFields,
                        context.getFormulaReturnType().getDataType().getName(),
                        context.getClass().getSimpleName(),
                        null /*exception*/);
            } else {
                FormulaEngine.getHooks().logFormulaRuntime(startTime, source.length(),
                		logInfo.commands,
                		logInfo.globalVariables,
                        sqlSize == null ? 0 : sqlSize.getSqlSize(),
                        javascript == null ? 0 : javascript.js.length(),
                        context.getClass().getSimpleName(),
                        false,
                        null /*exception*/);
            }
        }
    }

    /**
     * Logs a failed formula parsing
     */
    private void logFailure(long startTime, boolean isCreateOrEditFormula, String originalSource,
            Exception exception) {
        try {
            if (isCreateOrEditFormula || FormulaEngine.getHooks().shouldLogRuntime()) {
                String contextName = this.context.getClass().getSimpleName();

                // Protect against NPE since we are just logging this info.
                FormulaReturnType returnType = this.context.getFormulaReturnType();
                FormulaDataType dataType = returnType != null ? returnType.getDataType() : null;
                String dataTypeString = dataType != null ? dataType.getName() : null;
                int length = originalSource != null ? originalSource.length() : 0;
                if (isCreateOrEditFormula) {
                    FormulaEngine.getHooks().logFormulaDesignTime(startTime, length, 
                            null, null, null,
                            dataTypeString,
                            contextName,
                            exception);
                } else {
                    FormulaEngine.getHooks().logFormulaRuntime(startTime, length,
                            null, null, 0, 0,
                            contextName,
                            false,
                            exception);
                }
            }
        } catch (Exception e) { // NOPMD
            // We are logging a failed formula. Don't gack because of exceptions here.
        }
    }

    /**
     * Traverse the formula AST and possibly take action on a field not accessible to a user
     */
    private void handleInaccessibleFields(FormulaAST ast, FormulaContext context) throws FormulaException {
        InaccessibleFieldStrategy strategy = context.getProperty(FormulaRuntimeContext.HANDLE_INACCESSIBLE_FIELDS);

        // By default, allow fields a user doesn't have access to (take no action).
        if (strategy == null || strategy == InaccessibleFieldStrategy.ALLOW) {
            return;
        }

        FormulaASTVisitor visitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                // Only look at field references
                if (node.getType() == SfdcFormulaTokenTypes.IDENT) {
                    ContextualFormulaFieldInfo fieldReference = context.lookup(node.getText());
                    if (fieldReference.getFieldOrColumnInfo() != null &&
                            !FormulaEngine.getHooks().isFieldReadable(fieldReference.getFieldOrColumnInfo().getFieldInfo())) {
                        switch (strategy) {
                        case REPLACE_WITH_NULL:
                            node.setType(SfdcFormulaTokenTypes.NULL);
                            node.setText("null");
                            node.setCanBeNull(true);
                            break;
                        case THROW_EXCEPTION:
                        default:
                            throw new InvalidFieldReferenceException(fieldReference.getName(), "Not accessible", true);
                        }
                    }
                    return;
                }
            }
        };

        visit(ast, visitor, properties);
    }
    
    private static class LogInfo {
    	public final boolean hasPolymorphicFields;
    	public final String commands;
    	public final String globalVariables;
		public LogInfo(boolean hasPolymorphicFields, String commands, String globalVariables) {
			super();
			this.hasPolymorphicFields = hasPolymorphicFields;
			this.commands = commands;
			this.globalVariables = globalVariables;
		}
    }

    /**
     * Visits the formula AST to get the list of functions and global variables
     * and determine if there are polymorphic fields referenced.
     * @return Triple: (hasPolymorphicFields, commandList, globalVariablesList)
     */
    private LogInfo getLogInfo(FormulaAST astRoot, FormulaProperties props) {
        final List<String> commands = new ArrayList<>();
        final List<String> globalVariables = new ArrayList<>();
        final AtomicBoolean hasPolymorphicFields = new AtomicBoolean(false);
        FormulaASTVisitor logVisitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                String fieldName = node.getText();
                if (node.getType() == SfdcFormulaTokenTypes.IDENT) {
                    if (!hasPolymorphicFields.get() && fieldName.contains(":")) {
                        hasPolymorphicFields.set(true);
                    }
                    // Include global variables
                    if (!fieldName.isEmpty() && fieldName.charAt(0) == '$') {
                        int i = fieldName.indexOf('.');
                        if (i > -1) {
                            // Can only log the type since the actual field name may be customer metadata
                            globalVariables.add(fieldName.substring(0, i).toUpperCase());
                        }
                    }
                } else if (node.getType() != SfdcFormulaTokenTypes.ROOT && !node.isLiteral()
                        && node.getType() != SfdcFormulaTokenTypes.TEMPLATE_STRING_LITERAL
                        && node.getType() != SfdcFormulaTokenTypes.DYNAMIC_REF_IDENT
                        && node.getText() != null && !"template".equals(node.getText())) {
                    commands.add(node.getText().toUpperCase());
                }
            }
        };
        try {
            visit(astRoot, logVisitor, props);
        } catch (FormulaException e) {
            // Don't throw exceptions, we were just trying to log
            return new LogInfo(false, null, null);
        }

        return new LogInfo(
            hasPolymorphicFields.get(),
            Joiner.on(',').join(commands),
            Joiner.on(',').join(globalVariables));
    }

    @Override
    public Formula getFormula() throws FormulaException {
        return formula;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public FormulaProperties getProperties() {
        return properties;
    }

    protected FormulaContext getContext() {
        return this.context;
    }

    @Override
    public String encode() throws FormulaException {
        final List<Token> tokens = new ArrayList<Token>();

        FormulaASTVisitor visitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                if (node.getType() == SfdcFormulaTokenTypes.IDENT) {
                    tokens.add(node.getToken());
                }
            }
        };

        try {
            BaseFormulaInfoImpl.visit(FormulaUtils.getAST(source, properties), visitor, properties);
        } catch (FormulaException fe) {
            if (!properties.isDisabled()) {
                /*
                 * Bug: 148114
                 * Somehow the formula was already invalid and while that should
                 * not happen, it sometimes does.
                 * We ignore that here, so that we can soft-delete the formula.
                 * That means the set of tokens will be empty, which in
                 * turn means that the formula will now be stored as a string
                 * and we lose <fieldname>__c to id translations. Since the formula has
                 * to be manually "undisabled" anyway that is not so bad, and what else can
                 * we do anyway?
                 */
                throw fe;
            }
        }

        StringBuilder encodedSource = new StringBuilder(FormulaInfoFactory.ENCODED_PREFIX);

        // Add annotation
        if (!properties.getParseAsTemplate()) {
            if (!properties.getTreatNullNumberAsZero()) {
                encodedSource.append(FormulaUtils.TREAT_NULL_AS_NULL_ANNOTATION);
            }
            if (properties.isDisabled()) {
                encodedSource.append(FormulaUtils.DISABLE_ANNOTATION);
            }
        }

        int previousLocation = 0;
        for (Token token : tokens) {
            String tokenText = token.getText();

            String durableName;
            try {
                durableName = context.toDurableName(tokenText);
            }
            catch (InvalidFieldReferenceException x) {
                if (properties.getFailOnEmbeddedFormulaExceptions()) {
                    throw x;
                }

                durableName = tokenText;
            }

            String encodedFieldRef = "{!" + ID_PREFIX + durableName + "}";

            int location = token.getColumn() - 1;
            encodedSource.append(source.substring(previousLocation, location)).append(encodedFieldRef);

            previousLocation = location + tokenText.length();
        }

        if (previousLocation < source.length()) {
            encodedSource.append(source.substring(previousLocation));
        }

        String encodedSourceString = encodedSource.toString();

        if (!properties.getParseAsTemplate()) {
            // this same check happens in StringField - however for pre-validation we don't get into that code so I have
            // to have this here too.  bah
            int l;
            if ((l = Utf8.encodedLength(encodedSourceString)) > MAX_STORAGE_SIZE) {
                throw new FormulaException(FormulaI18nUtils.getLocalizer().getLabel("Exception", "EncodedByteLengthTooLong", l, MAX_STORAGE_SIZE)) {
                    private static final long serialVersionUID = 1L;
                };
            }
        }

        return encodedSourceString;
    }

    @Override
    public ContextualFormulaFieldInfo[] getReferences() {
        return fieldReferences;
    }

    private static AnnotationVisitor annotateAst(FormulaAST ast, FormulaContext context, FormulaProperties properties) throws FormulaException {
        // Annotate with datatype information, build reference table, and check
        // for cycles
        AnnotationVisitor annotationVisitor = new AnnotationVisitor(context, properties);
        visit(ast, annotationVisitor, properties);
        return annotationVisitor;
    }

    private static FormulaAST enrichRoot(FormulaAST ast, FormulaContext context, FormulaProperties properties) throws FormulaException {
        // Allow the root node to enrich itself
        FormulaCommandInfo commandInfo = FormulaCommandInfoRegistry.get(ast);
        if (commandInfo instanceof FormulaCommandEnricher)
            ast = ((FormulaCommandEnricher)commandInfo).enrich(ast, context, properties);

        if (!properties.isPolymorphicReturnType()) {
            // Type check the root against the type of the field
            FormulaDataType expectedType = context.getFormulaReturnType().getDataType();

            Type actualType = ast.getDataType();

            // Special logic for Flow Trigger parameters - we insert a TEXT function for fields that
            // need translation to strings which is supported in the Flow Interface (W-1935153)
            if (context.getGlobalProperties().getFormulaType().allowSObjectRowReference() && // ONLY ACTIONPARAM
                    expectedType.isSimpleTextOrClob() &&
                    !FormulaTypeUtils.isTypeText(actualType) &&
                    // These are the types we support conversion
                    (actualType == BigDecimal.class || actualType == FormulaDateTime.class || actualType == Date.class || FormulaTypeUtils.isTypePicklist(actualType))) {
                FormulaAST newParent = new FormulaAST();
                newParent.setType(SfdcFormulaTokenTypes.FUNCTION_CALL);
                newParent.setText("TEXT");
                newParent.setDataType(String.class);
                ast.reparent(newParent);
                return newParent;
            }

            boolean ok = (actualType == ConstantNull.class)
                || (actualType == RuntimeType.class)
                || ((expectedType.isCurrency()) && actualType == BigDecimal.class)
                || ((expectedType.isNumber()) && actualType == BigDecimal.class)
                || ((expectedType.isSimpleTextOrClob()) && FormulaTypeUtils.isTypeText(actualType))
                || ((expectedType.isEncrypted() && expectedType.isTextOrEncrypted()) && actualType == String.class)  // ENCRYPTEDTEXT
                || ((expectedType.isDate()) && actualType == FormulaDateTime.class)
                || ((expectedType.isTimeOnly()) && actualType == FormulaTime.class)
                || ((expectedType.isDateOnly()) && actualType == Date.class)
                || ((expectedType.isText() && expectedType.isSingleDynamicPicklist()) && FormulaTypeUtils.isTypeText(actualType)) // TEXTENUM
                || ((expectedType.isPickval() && FormulaTypeUtils.isTypeText(actualType)))
                || ((expectedType.isMultiEnum() && FormulaTypeUtils.isTypeText(actualType)))
                || ((expectedType.isBoolean()) && actualType == Boolean.class)
                || ((expectedType.isSingleDynamicPicklist()) && FormulaTypeUtils.isTypeText(actualType));

            if (!ok && expectedType.isId() && actualType instanceof FormulaTypeWithDomain.IdType) {
                // For Ids we need to check domains and handle special sobjectrow self reference
                FormulaSchema.FieldOrColumn info = context.getFormulaReturnType().getFieldOrColumnInfo();
                if (((FormulaTypeWithDomain.IdType)actualType).isApplicable(info)) {
                    // domain checks out - make sure if we are expecting a sobjectrow then we for sure have a sobjectrow reference - no other id type will work
                    ok = (FormulaUtils.isTypeSobjectRow(expectedType)) ==  !(((FormulaTypeWithDomain.IdType)actualType).isTypeText());
                }
            }

            if (!ok && (actualType instanceof FormulaTypeWithDomain)) {
                FormulaSchema.FieldOrColumn info = context.getFormulaReturnType().getFieldOrColumnInfo();
                ok = (FormulaTypeUtils.isTypeIdList(actualType, expectedType, info));
            }

            if (!ok) {
                throw new WrongExpressionTypeException(expectedType, actualType, context.getFormulaReturnType().getFieldOrColumnInfo());
            }
        }
        return ast;
    }

    private static FormulaSQLSize calculateSqlSize(SQLPair sqlPair, boolean isCheckingSqlLengthLimit, int maxSqlSize) throws FormulaException {
        int sqlSize = (sqlPair.sql != null) ? sqlPair.sql.length() : 0;
        int guardSize = (sqlPair.guard != null) ? sqlPair.guard.length() : 0;

        if (isCheckingSqlLengthLimit && sqlSize > maxSqlSize) {
            throw new SQLTooBigException(sqlSize, maxSqlSize);
        }

        return new FormulaSQLSize(sqlSize, guardSize);
    }


    @Override
    public int getSQLSize() {
        return sqlSize.getSqlSize();
    }
    @Override
    public int getGuardSQLSize() {
        return sqlSize.getGuardSize();
    }

    /**
     * Proceed through tree in InFix order. Eliminate all constant expression by evaluating them
     * and replacing them with their result at compile time.
     *
     * @author Lars Hofhansl
     */
    protected static FormulaAST optimizeParseTree(FormulaAST ast, FormulaContext context, BitSet properties, FormulaProperties formulaProperties) throws FormulaException {
        boolean isConstant = true;
        boolean canBeNull = false;

        // leaves
        if (ast.getNumberOfChildren() == 0) {
            isConstant = ast.isLiteral();
            canBeNull = ast.getType() == SfdcFormulaTokenTypes.NULL || ast.getType() == SfdcFormulaTokenTypes.IDENT;
        }

        // 1/3 is a more efficient representation than 0.33333333333333333333333333333333333333333333333
        // We also may lose precision, so do not precompile divisions and let SQL deal with it.
        if (ast.getType() == SfdcFormulaTokenTypes.DIV) {
            isConstant = false;
        }

        // depth first traversal
        FormulaAST child = (FormulaAST)ast.getFirstChild();
        while (child != null) {
            child = optimizeParseTree(child, context, properties, formulaProperties);
            isConstant &= child.isConstantExpression();
            if (!(FormulaAST.isFunctionNode(ast, "nullvalue") && child.getNextSibling() != null)) {
                // ignore NULLVALUE's first argument
                canBeNull |= child.canBeNull();
            }
            child = (FormulaAST)child.getNextSibling();
        }

        ast.setCanBeNull(canBeNull);
        ast.setConstantExpression(isConstant);

        // give formula commands a chance to optimize themselves
        FormulaCommandInfo commandInfo = FormulaCommandInfoRegistry.get(ast);
        if (commandInfo instanceof FormulaCommandOptimizer)
            ast = ((FormulaCommandOptimizer)commandInfo).optimize(ast, context);

        Type type = ast.getDataType();
        if (ast.isConstantExpression() && !ast.isLiteral() && (type == BigDecimal.class || type == String.class || type == Boolean.class || type == ConstantNull.class)) {
           try {
               // if it is constant... run it now, and replace the AST node with the result
               // i.e. evaluate constant portions of the formula at compile time
               // This also gets rid of superfluous NVL, etc, etc.
               Deque<Object> stack = new FormulaStack();
               List<FormulaCommand> commandList = new LinkedList<FormulaCommand>();
               properties.or(generateOne(ast, commandList, context, formulaProperties));
               FormulaRuntimeContext r = new ConstantFormulaContext(context);

               for (FormulaCommand command : commandList) {
                   command.execute(r, stack);
               }

               // now replace the constant subexpression with its result
               // this is propagated up the parse tree by the depth first traversal
               ast.setConstantExpression(true);
               ast.removeChildren();
               Object res = stack.pop();
               if (res == null) {
                   ast.setType(SfdcFormulaTokenTypes.NULL);
                   ast.setText("null");
                   ast.setCanBeNull(true);
               } else if (res instanceof BigDecimal) {
                   BigDecimal num = (BigDecimal)res;
                   FormulaAST node;
                   if (num.compareTo(BigDecimal.ZERO) == -1) {
                       // constants like -1 are normally parsed into two nodes: '-' and '1'
                       // this makes sure that we create the same parse tree.
                       ast.setType(SfdcFormulaTokenTypes.MINUS);
                       ast.setText("-");
                       ast.setCanBeNull(false);
                       node = new FormulaAST();
                       ast.addChild(node);
                       num = num.abs();
                   } else {
                       node = ast;
                   }
                   // add the value node
                   node.setType(SfdcFormulaTokenTypes.NUMBER);
                   node.setText(num.toPlainString());
                   node.setCanBeNull(false);
               } else if (res instanceof Boolean) {
                   boolean tmp = (Boolean) res;
                   ast.setType(tmp ? SfdcFormulaTokenTypes.TRUE : SfdcFormulaTokenTypes.FALSE);
                   ast.setText(""+tmp);
                   ast.setCanBeNull(false);
               } else if (res instanceof String) {
                   ast.setType(SfdcFormulaTokenTypes.STRING_LITERAL);
                   ast.setText("\""+FormulaTextUtil.escapeForFormulaString(res.toString())+"\"");
                   ast.setCanBeNull(false);
               } else {
                   throw new RuntimeException("Unknown constant type");
               }
           }
           catch (Exception x) {  // NOPMD
               // there was an error... too bad... do not optimize anything
           }
        }
        return ast;
   }

    private static SQLPair visitPostorder(FormulaAST ast, FormulaContext context, TableAliasRegistry registry) throws FormulaException {
        // Process the children
        int numChildren = ast.getNumberOfChildren();
        String[] args = new String[numChildren];
        String[] guards = new String[numChildren];

        FormulaAST child = null;
        for (int i = 0; i < numChildren; i++) {
            child = (FormulaAST)(i == 0 ? ast.getFirstChild() : child.getNextSibling());
            SQLPair result = visitPostorder(child, context, registry);
            args[i] = result.sql;
            guards[i] = result.guard;
        }
        // Process this node
        FormulaCommandInfo commandInfo = FormulaCommandInfoRegistry.get(ast);
        return commandInfo.getSQL(ast, context, args, guards, registry);
    }

    private static JsValue visitJavascript(FormulaAST ast, FormulaContext context) throws FormulaException {
        // Process the children
        int numChildren = ast.getNumberOfChildren();
        JsValue[] args = new JsValue[numChildren];

        int argsLength = 0;
        FormulaAST child = null;
        for (int i = 0; i < numChildren; i++) {
            child = (FormulaAST)(i == 0 ? ast.getFirstChild() : child.getNextSibling());
            JsValue result = visitJavascript(child, context);
            args[i] = result;

            argsLength += args[i].js != null ? args[i].js.length() : 0;
            if (argsLength > MAX_JS_SIZE_HARD_LIMIT) {
                throw new JSTooBigException(argsLength, false);
            }
        }
        // Process this node
        FormulaCommandInfo commandInfo = FormulaCommandInfoRegistry.get(ast);
        return commandInfo.getJavascript(ast, context, args);
    }


    public static void visit(FormulaAST node, FormulaASTVisitor visitor, FormulaProperties properties)
        throws FormulaException {
        FormulaAST firstSibling = (FormulaAST)node.getNextSibling();

        try {
            FormulaAST firstChild = (FormulaAST)node.getFirstChild();
            if (firstChild != null) {
                visit(firstChild, visitor, properties);
            }

            visitor.visit(node);
        }
        catch (FormulaException x) {
            handleVisitExceptions(x, node, visitor, properties);
        }

        try {
            if (firstSibling != null) {
                visit(firstSibling, visitor, properties);
            }
        }
        catch (FormulaException x) {
            handleVisitExceptions(x, node, visitor, properties);
        }
    }

    private static void handleVisitExceptions(FormulaException x, FormulaAST node, FormulaASTVisitor visitor,
        FormulaProperties properties) throws FormulaException {
        // Let the exception propogate if we are configured to throw exceptions on embedded formula exceptions or we're not the top of the AST tree
        boolean isTopLevelInTemplate = FormulaAST.isTopOfTemplateExpression(node);
        if (!properties.getParseAsTemplate() || properties.getFailOnEmbeddedFormulaExceptions()
            || !isTopLevelInTemplate) {
            if (x instanceof InvalidFieldReferenceException) {
                InvalidFieldReferenceException ifre = (InvalidFieldReferenceException) x;
                if (ifre.getLocation() < 0) {
                    ifre.setLocation(node.getToken().getColumn());
                }
            }
            throw x;
        }

        // Turn the entire expression into an empty string
        node.removeChildren();
        node.setType(SfdcFormulaTokenTypes.STRING_LITERAL);
        node.setText("\"\"");

        visitor.visit(node);
    }

    static String autoStripCurlyBangs(String source, FormulaProperties properties) {
        // Auto strip extraneous {! }'s for non-template contexts
        if (!properties.getParseAsTemplate()) {
            StringBuffer autoStrippedSource = new StringBuffer();
            Matcher matcher = FormulaUtils.REFERENCE_PATTERN.matcher(source);
            while (matcher.find()) {
                String reference = matcher.group(1);
                FormulaUtils.padIfNeeded(autoStrippedSource, source, matcher, reference);
            }

            matcher.appendTail(autoStrippedSource);
            return autoStrippedSource.toString();
        }
        return source;
    }

    static Set<String> getReferencedNames(FormulaAST ast, FormulaProperties properties) throws FormulaException {
        // Get the list of field references
        final Set<String> references = new HashSet<String>();
        FormulaASTVisitor visitor = new FormulaASTVisitor() {
            @Override
            public void visit(FormulaAST node) throws FormulaException {
                if (node.getType() == SfdcFormulaTokenTypes.IDENT) {
                    // Strip off ID prefix
                    String reference = node.getText();
                    if (reference.startsWith(ID_PREFIX)) {
                        reference = reference.substring(ID_PREFIX.length());
                    }

                    references.add(reference);
                }
            }
        };

        BaseFormulaInfoImpl.visit(ast, visitor, properties);

        return references;
    }

    static boolean isEncoded(String formulaSource) {
        return formulaSource != null && formulaSource.startsWith(FormulaInfoFactory.ENCODED_PREFIX);
    }

    static boolean isDisabled(String formulaSource) {
        return formulaSource != null && formulaSource.contains(FormulaUtils.DISABLE_ANNOTATION);
    }

    @Override
    public boolean isDeterministic() {
        return formula.isDeterministic(context);
    }

    @Override
    public boolean hasAIPredictionFieldReference() {
        return formula.hasAIPredictionFieldReference(context);
    }

    @Override
    public boolean referenceEncryptedFields() {
        return this.referenceEncryptedFields;
    }

    @Override
    public void validateMergeFieldsForFormulaType() throws FormulaException {
        FormulaCommandVisitorImpl.MergeFieldValidator visitor = new FormulaCommandVisitorImpl.MergeFieldValidator(context);
        formula.visitFormulaCommands(visitor);
        visitor.throwIfNecessary();
    }

    private final FormulaContext context;
    private final ContextualFormulaFieldInfo[] fieldReferences;
    private final String source;
    private final FormulaProperties properties;
    private final FormulaWithSql formula;
    private final FormulaSQLSize sqlSize;
    private final boolean referenceEncryptedFields;

    // Derived properties of formulas. Indices are into the BitSet returned by
    // the generate methods.
    public static final int NUM_PROPERTIES = 2;
    public static final int PRODUCES_HTML_INDEX = 0;
    public static final int GETS_SESSION_ID_INDEX = 1;

    protected static final String ID_PREFIX = "ID:";

    private static class FormulaSQLSize {
        private int sqlSize;
        private int guardSize;

        public int getSqlSize() {
            return this.sqlSize;
        }

        public int getGuardSize() {
            return this.guardSize;
        }

        private FormulaSQLSize(int sqlSize, int guardSize) {
            this.sqlSize = sqlSize;
            this.guardSize = guardSize;
        }

    }

    private static BitSet generate(FormulaAST ast, List<FormulaCommand> result, FormulaContext context, FormulaProperties formulaProperties) throws FormulaException {
        // Handle this node and its arguments
        BitSet properties = generateOne(ast, result, context, formulaProperties);

        // Handle siblings of this node
        FormulaAST firstSibling = (FormulaAST)ast.getNextSibling();
        if (firstSibling != null)
            properties.or(generate(firstSibling, result, context, formulaProperties));
        return properties;
    }

    private static BitSet generateOne(FormulaAST ast, List<FormulaCommand> result, FormulaContext context,FormulaProperties formulaProperties) throws FormulaException {
        BitSet properties = new BitSet(NUM_PROPERTIES);

        FormulaCommand command = null;

        String name = ast.getText().toUpperCase();
        if (ast.getType() != SfdcFormulaTokenTypes.TEMPLATE_STRING_LITERAL) {
            FormulaValidationHooks.ShortCircuitBehavior scb = FormulaValidationHooks.get().parseHook_caseShortCircuit(name);
            
            FormulaAST current = (FormulaAST)ast.getFirstChild();

            // Generate code for the arguments to this command.  But not if it is template text that just
            // happens to match a function name
            switch (scb) {
            case THUNK_REST:
                // Don't delay the first argument
                // To make CASE conditional, add it to this branch
                properties.or(generateOne(current, result, context, formulaProperties));
                // Delay the rest of the arguments
                current = (FormulaAST)current.getNextSibling();
                while (current != null) {
                    LinkedList<FormulaCommand> currentResult = new LinkedList<FormulaCommand>();
                    properties.or(generateOne(current, currentResult, context, formulaProperties));
                    result.add(new Thunk(currentResult.toArray(new FormulaCommand[currentResult.size()])));
                    current = (FormulaAST)current.getNextSibling();
                }
                break;
            case THUNK_ALL:
                // PrevGroupVal and ParentGroupVal arguments are used at parse time only and somewhat costly to evaluate
                // Delay all of the arguments
                while (current != null) {
                    LinkedList<FormulaCommand> currentResult = new LinkedList<FormulaCommand>();
                    properties.or(generateOne(current, currentResult, context, formulaProperties));
                    result.add(new Thunk(currentResult.toArray(new FormulaCommand[currentResult.size()])));
                    current = (FormulaAST)current.getNextSibling();
                }
                break;
            case THUNK_VLOOKUP:
                List<FormulaCommand> vlookupCommands = new ArrayList<FormulaCommand>(3);
                for (int i = 0; i < 3; i++) {
                    List<FormulaCommand> tempCommands = new LinkedList<FormulaCommand>();
                    properties.or(generateOne(current, tempCommands, context, formulaProperties));

                    if (i == 2) {
                        List<FormulaCommand> swapCommands = new LinkedList<FormulaCommand>();
                        swapCommands.add(new Thunk(tempCommands.toArray(new FormulaCommand[tempCommands.size()])));
                        tempCommands = swapCommands;
                    }

                    vlookupCommands.addAll(tempCommands);
                    current = (FormulaAST)current.getNextSibling();
                }

                command = FormulaValidationHooks.get().parseHook_generateFunctionVLookup(FormulaCommandInfoRegistry.get(ast),
                        vlookupCommands);
                break;
            case THUNK_PREDICT:
                List<FormulaCommand> predictCommands = new ArrayList<>();
                // Don't delay the first argument
                properties.or(generateOne(current, predictCommands, context, formulaProperties));
                // Delay the rest of the arguments
                current = (FormulaAST)current.getNextSibling();
                while (current != null) {
                    LinkedList<FormulaCommand> currentResult = new LinkedList<FormulaCommand>();
                    properties.or(generateOne(current, currentResult, context, formulaProperties));
                    predictCommands.add(new Thunk(currentResult.toArray(new FormulaCommand[currentResult.size()])));
                    current = (FormulaAST)current.getNextSibling();
                }
                command = FormulaValidationHooks.get().parseHook_generateFunctionPredict(
                        FormulaCommandInfoRegistry.get(ast), predictCommands);
                break;
            case EVAL_ALL:
                // Standard treatment
                if (current != null) properties.or(generate(current, result, context, formulaProperties));
            }
        }

        if (command == null) {
            command = FormulaCommandInfoRegistry.get(ast).getCommand(ast, context);
        }

        result.add(command);

        FormulaValidationHooks.get().parseHook_validateJavascriptInCommands(ast, name, command, properties, formulaProperties);

        return properties;
    }

}
