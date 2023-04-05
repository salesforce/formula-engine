package com.force.formula.v2;

import com.force.formula.FormulaDataType;

public interface GoldFileOutputGenerator<T> {
    String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, T testEntity);
}
