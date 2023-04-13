package dqualizer.dqexec.loadtest.mapper.k6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.exception.UnknownRequestTypeException;
import dqualizer.dqexec.util.SafeFileReader;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the specified request to a k6 'default function()'
 */
@Component
@RequiredArgsConstructor
public class HttpMapper implements k6Mapper {

    private final PathConfig paths;
    private final SafeFileReader reader;

    @Override
    public String map(Request request) {
        StringBuilder httpBuilder = new StringBuilder();
        httpBuilder.append(this.exportFunctionScript());
        String path = request.getPath();
        String type = request.getType().toUpperCase();
        String method = switch (type) {
            case "GET" -> "get";
            case "POST" -> "post";
            case "PUT" -> "put";
            case "DELETE" -> "del";
            default -> throw new UnknownRequestTypeException(type);
        };

        //Code for choosing one random variable for every existing path variable
        Map<String, String> pathVariables = request.getPathVariables();
        Optional<String> maybeReference = pathVariables.values().stream().findFirst();
        if (maybeReference.isPresent()) {
            String referencePath = paths.getResourcePath() + maybeReference.get();
            String pathVariablesString = reader.readFile(referencePath);

            try {
                JsonNode node = new ObjectMapper().readTree(pathVariablesString);

                Iterator<String> variables = node.fieldNames();
                while (variables.hasNext()) {
                    String variable = variables.next();
                    String randomPathVariable = this.randomPathVaribleScript(variable);
                    httpBuilder.append(randomPathVariable);
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        //Code for using the request-parameter and payload inside the http method
        Map<String, String> payload = request.getPayload();
        Map<String, String> queryParams = request.getQueryParams();
        String extraParams = "";
        if (!payload.isEmpty() || !queryParams.isEmpty()) {
            if (!payload.isEmpty() && !queryParams.isEmpty()) {
                httpBuilder.append(this.randomQueryParamScript());
                httpBuilder.append(this.randomPayloadScript());
                extraParams = " + `?${urlSearchParams.toString()}`, JSON.stringify(payload)";
            } else if (!payload.isEmpty()) {
                httpBuilder.append(this.randomPayloadScript());
                extraParams = ", JSON.stringify(payload)";
            } else {
                httpBuilder.append(this.randomQueryParamScript());
                extraParams = " + `?${urlSearchParams.toString()}`";
            }
        }
        String httpRequest = String.format("%slet response = http.%s(baseURL + `%s`%s, params);%s",
                newLine, method, path, extraParams, newLine);
        httpBuilder.append(httpRequest);

        return httpBuilder.toString();
    }

    private String exportFunctionScript() {
        return String.format("%sexport default function() {%s",
                newLine, newLine);
    }

    private String randomPathVaribleScript(String variable) {
        return String.format("%slet %s = %s_array[Math.floor(Math.random() * %s_array.length)];%s",
                newLine, variable, variable, variable, newLine);
    }

    private String randomPayloadScript() {
        return String.format("%slet payload = payloads[Math.floor(Math.random() * payloads.length)];%s",
                newLine, newLine);
    }

    private String randomQueryParamScript() {
        return """
                let currentSearchParams = searchParams[Math.floor(Math.random() * searchParams.length)];
                let urlSearchParams = new URLSearchParams(currentSearchParams);
                """;
    }
}