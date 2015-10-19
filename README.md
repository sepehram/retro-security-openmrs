# retro-security-openmrs
Retrospective Security Module for OpenMRS

In order to build Retrospective Security Module for OpenMRS:

    Install XSB Prolog

    Go to rewriter/config/config and specify

    2.1. the module path, i.e., the path to "retrosecurity" directory, e.g., /home/uname/repo/retrosecurity

    2.2. the XSB Prolog bin path, e.g., /home/uname/XSB/bin

    Go to rewriter/json/LS.json and specify the logging specification. A sample specification is currently given in LS.json.

    Run the shell script rewrite/logging_mod.sh to generate the OpenMRS module that enforces the given logging specification.

