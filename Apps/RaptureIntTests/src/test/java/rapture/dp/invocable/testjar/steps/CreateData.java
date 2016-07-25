package rapture.dp.invocable.testjar.steps;


import org.apache.log4j.Logger;
import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.kernel.Kernel;

public class CreateData extends AbstractInvocable<Object> {
    private static Logger log = Logger.getLogger(CreateData.class.getName());
    
    public CreateData(String workerURI,String stepName) {
        super(workerURI,stepName);
    }
    
    @Override
    public String invoke(CallingContext ctx) {
        log.info("Starting test wf: " + getWorkerURI());
        Kernel.getDecision().writeWorkflowAuditEntry(ctx, getWorkerURI(), "***** Starting test wf for " + getWorkerURI(), false) ;
        String docUri = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DOC_URI");
        String docContent = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DOC_CONTENT");
        Kernel.getDoc().putDoc(ctx, docUri,docContent);
        log.info("Finishing test wf: " + getWorkerURI() + " by writing to " + docUri);
        Kernel.getDecision().writeWorkflowAuditEntry(ctx, getWorkerURI(), "Finishing test wf: " + getWorkerURI() + " by writing to " + docUri, false) ;
        return "next";
    }
}
