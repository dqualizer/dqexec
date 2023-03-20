# dqexec
The dqexec component executes the RQA configuration by utilizing state-of-the-art monitoring, load testing, and resilience testing tooling. The specification is received in a generic format and then mapped to the input models of the external analysis tooling. Besides delegating the RQA execution, dqexec is also responsible for choosing the most appropriate analysis tool, repeating tests to reach a certain accuracy, and enriching the tests with tool-specific default values.

A more detailed description of this component's architecture is provided in the [arc42 document](https://github.com/dqualizer/dqualizer/tree/main/docs/asciidoc). 
