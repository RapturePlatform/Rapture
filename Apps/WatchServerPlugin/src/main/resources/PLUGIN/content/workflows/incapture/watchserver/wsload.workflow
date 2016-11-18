{
    "workflowURI": "workflow://workflows/incapture/watchserver/wsload",
    "steps": [
        {
            "name": "setup",
            "description": "Sets up variables for execution",
            "executable": "script://scripts/incapture/wsload/setup",
            "transitions": [
                {
                    "name": "ok",
                    "targetStep": "stepA"
                }
            ]
        },
        {
            "name": "stepA",
            "description": "Step A Description",
            "executable": "dp_java_invocable://workflow.StepA",
            "transitions": [
                {
                    "name": "ok",
                    "targetStep": "stepB"
                },
                {
                    "name": "error",
                    "targetStep": "$FAIL"
                }
            ]
        },
        {
            "name": "stepB",
            "description": "Step B Description",
            "executable": "dp_java_invocable://workflow.StepB",
            "transitions": [
                {
                    "name": "ok",
                    "targetStep": "$RETURN"
                },
                {
                    "name": "error",
                    "targetStep": "$FAIL"
                }
            ]
        }
    ],
    "startStep": "setup",
    "jarUriDependencies": [
        "jar://workflows/incapture/wsload/*"
    ],
    "category": "alpha"
}
