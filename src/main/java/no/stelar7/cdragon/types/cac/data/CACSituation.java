package no.stelar7.cdragon.types.cac.data;

import lombok.Data;

import java.util.List;

@Data
public class CACSituation
{
    private String                 situationName;
    private List<CACSituationRule> rules;
}
