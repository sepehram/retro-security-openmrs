# retro-security-openmrs
Retrospective Security Module for OpenMRS

BUILDING MODULE

1. Install XSB Prolog

2. Go to rewriter/config/config and specify

    2.1. the module path, i.e., the path to "retrosecurity" directory, e.g., /home/uname/repo/retrosecurity

    2.2. the XSB Prolog bin path, e.g., /home/uname/XSB/bin

3. Go to rewriter/json/LS.json and specify the logging specification. A sample specification is currently given in LS.json. 

4. Run the shell script rewrite/logging_mod.sh to generate the OpenMRS module that enforces the given logging specification. 


JSON SPECIFICATION 

The JSON file represents a logic program, i.e., a list of Horn clauses consisting of a head of the Horn clause, and the body of the Horn clause. The head comprises a a single literal and the body comprises a list of literals. 

Each literal contains information about symbol type, literal type, literal name and a list of arguments. The symbol type represents whether the predicate symbol is user-defined or built-in in XSB. The literal name evidently shows the name of the predicate symbol. The literal type defines the role of the predicate in the horn clause. It could be a logging evenet invocation, a trigger invocation, timestamp comparison, extra condition, or normal type otherwise, e.g., in case it is defining the predicate in the head of the logging specification.

Each argument has a name and a type. The type shows whether the argument is a variable or a value.



