package rapture.dp.invocable.step;

import rapture.common.CallingContext;
import rapture.common.dp.Steps;
import rapture.dp.AbstractStep;

public class Step2 extends AbstractStep {

    public Step2(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        System.out.println("X");
        return Steps.NEXT;
    }

}
