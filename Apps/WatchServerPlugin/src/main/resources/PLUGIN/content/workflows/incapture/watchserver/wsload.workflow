{
    "workflowURI": "workflow://workflows/incapture/watchserver/wsload",
    "steps": [
        {
            "name": "setup",
            "description": "Sets up variables for execution",
            "executable": "script://scripts/incapture/watchserver/setup",
            "transitions": [
                {
                    "name": "ok",
                    "targetStep": "loadFile"
                }
            ]
        },
        {
            "name": "loadFile",
            "description": "Load a file",
            "executable": "dp_java_invocable://workflow.LoadFile",
            "transitions": [
                {
                    "name": "ok",
                    "targetStep": "processFile"
                },
                {
                    "name": "error",
                    "targetStep": "$FAIL"
                }
            ]
        },
        {
            "name": "processFile",
            "description": "Process a file",
            "executable": "dp_java_invocable://workflow.ProcessFile",
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
        "jar://workflows/incapture/watchserver/*"
    ],
    "category": "alpha"
}
