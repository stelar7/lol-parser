package no.stelar7.cdragon.types.cac.data;

import lombok.Data;

@Data
public class CACSituationRule
{
    private String                    ruleName;
    private CACSituationRuleCondition conditions;
    private CACSituationRuleAction    actions;
}
