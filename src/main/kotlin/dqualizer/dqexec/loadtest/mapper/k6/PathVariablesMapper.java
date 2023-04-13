package dqualizer.dqexec.loadtest.mapper.k6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.exception.NoReferenceFoundException;
import dqualizer.dqexec.util.SafeFileReader;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps the path variables to Javascript-Code
 */
@Component
@RequiredArgsConstructor
public class PathVariablesMapper implements k6Mapper {

    private final PathConfig paths;
    private final SafeFileReader reader;

    @Override
    public String map(Request request) {
        StringBuilder pathVariablesBuilder = new StringBuilder();
        Map<String, String> pathVariables = request.getPathVariables();
        Optional<String> maybeReference = pathVariables.values().stream().findFirst();
        if (maybeReference.isEmpty()) throw new NoReferenceFoundException(pathVariables);

        String referencePath = paths.getResourcePath() + maybeReference.get();
        String pathVariablesString = reader.readFile(referencePath);
        String pathVariablesScript = String.format("%sconst path_variables = %s",
                newLine, pathVariablesString);
        pathVariablesBuilder.append(pathVariablesScript);

        try {
            JsonNode node = new ObjectMapper().readTree(pathVariablesString);

            Iterator<String> variables = node.fieldNames();
            while (variables.hasNext()) {
                String variable = variables.next();
                String particularPathVariablesScript = String.format("%sconst %s_array = path_variables['%s'];",
                        newLine, variable, variable);
                pathVariablesBuilder.append(particularPathVariablesScript);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return pathVariablesBuilder.toString();
    }
}
