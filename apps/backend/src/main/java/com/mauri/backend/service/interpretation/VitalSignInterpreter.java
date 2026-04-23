package com.mauri.backend.service.interpretation;

import com.mauri.backend.entity.VitalSigns;
import com.mauri.backend.enums.VitalSignType;

public interface VitalSignInterpreter {

    boolean supports(VitalSignType type);

    VitalInterpretationResult interpret(VitalSigns vitalSigns, VitalInterpretationContext context);
}
