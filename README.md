# dqexec
The dqexec component executes the RQA configuration by utilizing state-of-the-art monitoring, load testing 
and resilience testing tooling. The specification is received in a generic format and then mapped to the 
input models of the external analysis tooling. Besides delegating the RQA execution, dqexec is also 
responsible for choosing the most appropriate analysis tool, repeating tests to reach a certain accuracy 
and enriching the tests with tool-specific default values.

A more detailed description of this component's architecture is provided in the 
[arc42 document](https://dqualizer.github.io/dqualizer). 

## Current state

Load testing with k6 is possible, however the specified workload will not be taken into consideration.

Monitoring is still under development. The Docker client probably has to be refactored. 
Also, the inspectIT Ocelot instrumentation might be extended.
**All monitoring tests are currently disabled!**

## Development
Executing the project requires to configure Gradle according to the instructions in the 
[dqlang documentation](https://github.com/dqualizer/dqlang).
