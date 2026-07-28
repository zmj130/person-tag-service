package com.qianfan.tag.dto;

import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.IndicatorOption;

import java.util.List;

public class IndicatorDetail {
    private IndicatorDefinition definition;
    private List<IndicatorOption> options;
    private List<String> operators;

    public IndicatorDetail(IndicatorDefinition definition, List<IndicatorOption> options, List<String> operators) {
        this.definition = definition;
        this.options = options;
        this.operators = operators;
    }

    public IndicatorDefinition getDefinition() { return definition; }
    public List<IndicatorOption> getOptions() { return options; }
    public List<String> getOperators() { return operators; }
}
