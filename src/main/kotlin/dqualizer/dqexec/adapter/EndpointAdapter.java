package dqualizer.dqexec.adapter;

import dqualizer.dqexec.exception.UnknownTermException;
import dqualizer.dqexec.input.ConstantsLoader;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.LoadTestConstants;
import dqualizer.dqlang.archive.k6adapter.dqlang.constants.ResponseTime;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Checks;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Endpoint;
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.Response;
import dqualizer.dqlang.archive.k6adapter.dqlang.loadtest.ResponseMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapts one endpoint to a Request object
 */
@Component
public class EndpointAdapter {

    @Autowired
    private ConstantsLoader constantsLoader;

    /**
     * @param endpoint        Endpoint for one loadtest
     * @param responseMeasure Information for response measures
     * @return An inoffical k6 Request object
     */
    public Request adaptEndpoint(Endpoint endpoint, ResponseMeasure responseMeasure) {
        String field = endpoint.getField();
        String path = this.markPathVariables(field);
        String type = endpoint.getOperation();
        Map<String, String> pathVariables = endpoint.getPathVariables();
        Map<String, String> queryParams = endpoint.getUrlParameter();
        Map<String, String> params = endpoint.getRequestParameter();
        Map<String, String> payload = endpoint.getPayload();

        int duration = this.getDuration(responseMeasure);
        LinkedHashSet<Integer> statusCodes = this.getStatusCodes(endpoint);
        Checks checks = new Checks(statusCodes, duration);

        return new Request(type, path, pathVariables, queryParams, params, payload, checks);
    }

    /**
     * At first the method finds all path variables inside the field with the help of a regex pattern.
     * The pattern is looking for text that is enclosed by curly brackets {}.
     * Those variables are saved inside a list, including the brackets. Duplicates will be combined to one variable.
     * After that a "$"-symbol will be added to all found variables inside the field
     *
     * @param field Path with unmarked variables
     * @return Path with marked variables, for example '{id}' turns into '${id}'
     */
    private String markPathVariables(String field) {
        Pattern pattern = Pattern.compile("\\{.*?}");
        Matcher matcher = pattern.matcher(field);
        List<String> variables = new LinkedList<>();

        while (matcher.find()) {
            String foundVariable = matcher.group();
            variables.add(foundVariable);
        }

        String markedPathVariables = variables.stream()
                .distinct()
                .reduce(field, (path, variable) -> path.replace(variable, "$" + variable));
        return markedPathVariables;
    }

    /**
     * Get the expected answer duration for this request specified in the modeling
     *
     * @param responseMeasure Information for response measures
     * @return The expected answer duration for this request
     */
    private int getDuration(ResponseMeasure responseMeasure) {

        LoadTestConstants constants = constantsLoader.load();
        ResponseTime responseTime = constants.getResponseTime();

        String responseTimeTerm = responseMeasure.getResponseTime();
        int duration;
        switch (responseTimeTerm) {
            case "SATISFIED" -> duration = responseTime.getSatisfied();
            case "TOLERATED" -> duration = responseTime.getTolerated();
            case "FRUSTRATED" -> duration = responseTime.getFrustrated();
            default -> throw new UnknownTermException(responseTimeTerm);
        }
        return duration;
    }

    /**
     * Get a set of expected status codes for this request
     *
     * @param endpoint Endpoint for this request
     * @return A set of expected status codes
     */
    private LinkedHashSet<Integer> getStatusCodes(Endpoint endpoint) {
        LinkedHashSet<Response> responses = endpoint.getResponses();
        LinkedHashSet<Integer> statusCodes = new LinkedHashSet<>();

        for (Response response : responses) {
            int statusCode = response.getExpectedCode();
            statusCodes.add(statusCode);
        }
        return statusCodes;
    }
}