package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.*;
import com.force.i18n.commons.text.TextUtil;

/**
 * Implementation of a function to compute the distance between two locations defined by their latitude and longitude
 * The function will also need a unit argument to know which unit to use for the resulting distance.
 *
 * The actual computation of the function, either at runtime or for the SQL generation is delegated to a {@link FormulaGeolocationService}.
 *
 * @author ahersans
 * @since 178
 */
@AllowedContext(section=SelectorSection.MATH,nonDisplayOnly=true)
public class FunctionDistance extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    private static final String DIAMETER_EXPRESSION;
    private static final String UNIT_GUARD_EXPRESSION;
    private static final String TREAT_NULL_AS_ZERO = "com.force.formula.commands.FunctionDistance.TREAT_NULL_AS_ZERO";

    static {
        StringBuilder diameterExpression = new StringBuilder();
        diameterExpression.append("CASE %s ");
        for (DistanceUnit unit : DistanceUnit.values())
            diameterExpression.append("WHEN '").append(unit.getAbbreviation()).append("' THEN ").append(unit.getEarthMeanDiameter()).append(" ");
        diameterExpression.append("END");
        DIAMETER_EXPRESSION = diameterExpression.toString();

        StringBuilder unitGuard = new StringBuilder();
        DistanceUnit[] units = DistanceUnit.values();
        unitGuard.append("%s NOT IN (");
        for (int index = 0; index < units.length; index++) {
            unitGuard.append("'").append(units[index].getAbbreviation()).append("'");
            if (index < units.length - 1)
                unitGuard.append(",");
        }
        unitGuard.append(")");
        UNIT_GUARD_EXPRESSION = unitGuard.toString();
    }

    public FunctionDistance() {
        // distance arguments: location_1, location_2, unit
        super("DISTANCE", BigDecimal.class, new Class[] { FormulaGeolocation.class, FormulaGeolocation.class, String.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new DistanceCommand(this);
    }

    private FormulaGeolocationService getGeolocationService() {
        return FormulaEngine.getHooks().getFormulaGeolocationService();
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        List<String> firstCoordinates = TextUtil.splitSimple(FormulaGeolocationService.LOCATION_DELIMITER, args[0]);
        List<String> secondCoordinates = TextUtil.splitSimple(FormulaGeolocationService.LOCATION_DELIMITER, args[1]);
        String unit = args[2];
        Boolean treatNullAsZero = (Boolean)context.getProperty(TREAT_NULL_AS_ZERO);
        if(treatNullAsZero == null){
            treatNullAsZero = false;
        }
        String[] arg1 = getDistanceArguments(firstCoordinates, treatNullAsZero);
        String[] arg2 = getDistanceArguments(secondCoordinates, treatNullAsZero);
        String sql = getGeolocationService().generateDistanceSqlExpression(arg1[0], arg1[1], arg1[2], arg2[0], arg2[1], arg2[2], String.format(DIAMETER_EXPRESSION, unit));

        // Needed because ASIN is double/double only.
        if (context.getSqlStyle() != null && context.getSqlStyle().isPostgresStyle()) {
            sql = sql + "::numeric";
        }
        
        return new SQLPair(sql, SQLPair.generateGuard(guards, String.format(UNIT_GUARD_EXPRESSION, unit)));
    }

    private String[] getDistanceArguments(List<String> coordinates, boolean treatNullAsZero) {
        //Extract the XYZ column from the coordinates. Use array size to determine which field type we have.
        int coordinateCount = coordinates.size();
        
        if (coordinateCount == 2)  {
            // Geolocation() expression specifying latitude and longitude
            return parseLatitudeLongitude(coordinates.get(0), coordinates.get(1));
        } 

        // Allow the hook to handle it for 3 or more values
        return FormulaValidationHooks.get().getDistanceSql(coordinates, treatNullAsZero);
    }

    private String[] parseLatitudeLongitude(String latitudeExpr, String longitudeExpr) {
        try {
            // Let's see if they are simple constants
            double latitudeValue = Double.valueOf(stripParentheses(latitudeExpr));
            double longitudeValue = Double.valueOf(stripParentheses(longitudeExpr));

            return getGeolocationService().getXyzStrings(latitudeValue, longitudeValue);
        } catch (NumberFormatException e) {
            // If not, we'll have to do it the very long way.
            return getGeolocationService().getXyzStrings(latitudeExpr, longitudeExpr);
        }
    }

    private static String stripParentheses(String str) {
        while (str.length() >= 2 && str.charAt(0) == '(' && str.charAt(str.length()-1) == ')') {
            str = str.substring(1, str.length()-1);
        }
        return str;
    }

    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {

        if (node.getNumberOfChildren() != 3)
            throw new WrongNumberOfArgumentsException(node.getText(), 3, node);

        FormulaAST firstLocationNode = (FormulaAST)node.getFirstChild();
        if (firstLocationNode.getDataType() != FormulaGeolocation.class)
            throw new WrongArgumentTypeException(firstLocationNode.getText(), new Class[] { FormulaGeolocation.class }, firstLocationNode);

        FormulaAST secondLocationNode = (FormulaAST)firstLocationNode.getNextSibling();
        if (secondLocationNode.getDataType() != FormulaGeolocation.class)
            throw new WrongArgumentTypeException(secondLocationNode.getText(), new Class[] { FormulaGeolocation.class }, secondLocationNode);

        FormulaAST unitNode = (FormulaAST)secondLocationNode.getNextSibling();
        if (unitNode.getDataType() != String.class)
            throw new WrongArgumentTypeException(unitNode.getText(), new Class[] { String.class }, unitNode);

        String unitString = FormulaTextUtil.removeEnclosingQuotes(unitNode.getText());
        if(unitNode.isLiteral()&& DistanceUnit.getUnitByName(unitString) == null) {
            throw new WrongArgumentException(getName(), "'mi'/'km'", unitNode);
        }
        context.setProperty(TREAT_NULL_AS_ZERO, properties.getTreatNullNumberAsZero());
        return BigDecimal.class;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("typeof $formula==='undefined'?undefined:$formula.disance("+args[0]+","+args[1]+","+args[2]+")", args);
    }
}

class DistanceCommand extends AbstractFormulaCommand {
	private static final long serialVersionUID = 1L;
	private final FormulaGeolocationService locationService = FormulaEngine.getHooks().getFormulaGeolocationService();

    DistanceCommand(FormulaCommandInfo info) {
        super(info);
    }

    private FormulaGeolocationService getGeolocationService() {
        return this.locationService;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String unitAbbreviation     = checkStringType(stack.pop());
        DistanceUnit unit = null;
        for (DistanceUnit tmpUnit : DistanceUnit.values())
            if (tmpUnit.getAbbreviation().equals(unitAbbreviation))
                unit = tmpUnit;

        if (unit == null)
            throw new FormulaEvaluationException("Invalid unit for the distance() function: " + unitAbbreviation);

        FormulaGeolocation secondLocation = checkGeoLocationType(stack.pop());
        FormulaGeolocation firstLocation  = checkGeoLocationType(stack.pop());

        Double distance = getGeolocationService().computeDistance(firstLocation, secondLocation, unit);
        BigDecimal bigDistance = null;

        if (distance != null) {
            bigDistance = BigDecimal.valueOf(distance);
        }

        // Push even if the value is null so that compound formulas
        // that use this value as an operand will be complete
        stack.push(bigDistance);
    }
}
