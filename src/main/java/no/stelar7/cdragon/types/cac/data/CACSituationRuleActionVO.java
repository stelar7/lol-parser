package no.stelar7.cdragon.types.cac.data;

import lombok.Data;

@Data
public class CACSituationRuleActionVO
{
    private String actionName;
    private String selfEventName;
    private String allyEventName;
    private String enemyEventName;
    private String spectatorEventName;
}
