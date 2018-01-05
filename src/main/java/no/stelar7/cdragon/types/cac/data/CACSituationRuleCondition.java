package no.stelar7.cdragon.types.cac.data;

import lombok.Data;

@Data
public class CACSituationRuleCondition
{
    private int                                           ruleCoolDown;
    private CACSituationRuleConditionNearestEnemyChampion nearestEnemyChampion;
}
