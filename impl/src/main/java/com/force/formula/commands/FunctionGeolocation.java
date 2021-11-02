package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Function to instantiate a GeoLocation within the formula engine
 *
 * @author ahersans
 * @since 178
 * @see FunctionDistance
 */
@AllowedContext(section=SelectorSection.MATH,nonDisplayOnly=true)
public class FunctionGeolocation extends FormulaCommandInfoImpl implements FormulaCommandValidator {
	// TODO SLT FIXME from FormulaFieldInfoFieldInfoAdapter
	public static final String LOCATION_DELIMITER = "**";
    static final int LAT_MIN = -90;
    static final int LAT_MAX = 90;
    static final int LON_MIN = -180;
    static final int LON_MAX = 180;

    public FunctionGeolocation() {
        // location constructor arguments: latitude, longitude
        super("GEOLOCATION", FormulaGeolocation.class, new Class[] { BigDecimal.class, BigDecimal.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new GeolocationCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = args[0] + FunctionGeolocation.LOCATION_DELIMITER + args[1];
        FormulaAST latitudeNode = (FormulaAST) node.getFirstChild();
        FormulaAST longitudeNode = (FormulaAST) latitudeNode.getNextSibling();
        String latGuard = "";
        String lonGuard = "";
        String notNullGuard = "";
        if (!latitudeNode.isLiteral()) {
            latGuard = args[0] + " < " + LAT_MIN + " OR " + LAT_MAX + " < " + args[0];
        }
        if (!longitudeNode.isLiteral()) {
            lonGuard = args[1] + " < " + LON_MIN + " OR " + LON_MAX + " < " + args[1];
        }
        if (!latitudeNode.isLiteral() && !longitudeNode.isLiteral())  {
            notNullGuard = " OR (" + args[0] + " IS NULL AND " + args[1] + " IS NOT NULL) " + "OR (" + args[0] + " IS NOT NULL AND " + args[1] + " IS NULL) ";
        }
        
        String fullGuard = latGuard + ((latGuard.isEmpty() || lonGuard.isEmpty()) ? "" : " OR ") + lonGuard + notNullGuard;
        return new SQLPair(sql, SQLPair.generateGuard(guards, (fullGuard.isEmpty() ? null : fullGuard)));
    }     

    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {

        if (node.getNumberOfChildren() != 2)
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);

        FormulaAST latitudeNode = (FormulaAST)node.getFirstChild();
        FormulaAST longitudeNode = (FormulaAST)latitudeNode.getNextSibling();

        if (latitudeNode.getDataType() != BigDecimal.class)
            throw new WrongArgumentTypeException(latitudeNode.getText(), new Class[] { BigDecimal.class }, latitudeNode);
        if (longitudeNode.getDataType() != BigDecimal.class)
            throw new WrongArgumentTypeException(longitudeNode.getText(), new Class[] { BigDecimal.class }, longitudeNode);

        if (latitudeNode.isLiteral()) {
            double lat = Double.valueOf(latitudeNode.getText());
            if (lat < LAT_MIN || lat > LAT_MAX) {
                throw new WrongArgumentException("GEOLOCATION");
            }
        }
        if (longitudeNode.isLiteral()) {
            double longitude = Double.valueOf(longitudeNode.getText());
            if (longitude < LON_MIN || longitude > LON_MAX) {
                throw new WrongArgumentException("GEOLOCATION");
            }
        }
        return FormulaGeolocation.class;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("{latitude:"+args[0]+",longitude:"+args[1]+"}", args);
    }

}

class GeolocationCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	GeolocationCommand(FormulaCommandInfo info) {
        super(info);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        Number longitude = checkNumberType(stack.pop());
        Number latitude = checkNumberType(stack.pop());

        if (latitude != null && (latitude.doubleValue() < FunctionGeolocation.LAT_MIN || latitude.doubleValue() > FunctionGeolocation.LAT_MAX)) {
            throw new FormulaEvaluationException("Latitude out of range: " + (latitude == null ? "null" : latitude.doubleValue()));
        }
        if (longitude != null && (longitude.doubleValue() < FunctionGeolocation.LON_MIN || longitude.doubleValue() > FunctionGeolocation.LON_MAX)) {
            throw new FormulaEvaluationException("Longitude is out of range: " + (longitude == null ? "null" : longitude.doubleValue()));
        }

        if (latitude == null && longitude != null)  {
            throw new FormulaEvaluationException("Latitude can be null if and only if longitude is null" + (latitude == null ? "null" : latitude.doubleValue()) + 
                    " Longitude = " + (longitude == null ? "null" : longitude.doubleValue()));
        }
        else if (latitude != null && longitude == null)  {
            throw new FormulaEvaluationException("Longitude can be null if and only if latitude is null" + (latitude == null ? "null" : latitude.doubleValue()) + 
                    " Longitude = " + (longitude == null ? "null" : longitude.doubleValue()));
        }

        stack.push(FormulaEngine.getHooks().constructGeolocation(latitude, longitude));
    }
}
