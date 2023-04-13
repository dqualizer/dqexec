package dqualizer.dqexec.loadtest.mapper.k6;

import com.fasterxml.jackson.core.JsonProcessingException;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.options.Options;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import dqualizer.dqlang.archive.k6configurationrunner.dqlang.LoadTest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Maps the one loadtest from the inofficial k6-configuration to k6-script
 */
@Component
@RequiredArgsConstructor
public class ScriptMapper implements k6Mapper {

    private final ParamsMapper paramsMapper;
    private final PayloadMapper payloadMapper;
    private final QueryParamsMapper queryParamsMapper;
    private final PathVariablesMapper pathVariablesMapper;
    private final HttpMapper httpMapper;
    private final ChecksMapper checksMapper;

    /**
     * Map one loadtest to a k6-script
     *
     * @param baseURL  The baseURL for all loadtests inside the inofficial k6-configuration
     * @param loadTest One specified loadtest
     * @return A list of strings, which can be written to a file
     * @throws JsonProcessingException
     */
    public List<String> getScript(String baseURL, LoadTest loadTest) throws JsonProcessingException {
        List<String> script = new LinkedList<>();

        Options options = loadTest.getOptions();
        script.add(this.startScript(baseURL, options));

        Request request = loadTest.getRequest();
        String requestScript = this.map(request);
        script.add(requestScript);

        script.add("}");
        return script;
    }

    @Override
    public String map(Request request) {
        StringBuilder requestBuilder = new StringBuilder();

        String paramsScript = paramsMapper.map(request);
        requestBuilder.append(paramsScript);

        if (!request.getPayload().isEmpty()) {
            String payloadScript = payloadMapper.map(request);
            requestBuilder.append(payloadScript);
        }
        if (!request.getQueryParams().isEmpty()) {
            String queryParamsScript = queryParamsMapper.map(request);
            requestBuilder.append(queryParamsScript);
        }
        if (!request.getPathVariables().isEmpty()) {
            String pathVariablesScript = pathVariablesMapper.map(request);
            requestBuilder.append(pathVariablesScript);
        }

        String httpScript = httpMapper.map(request);
        requestBuilder.append(httpScript);
        requestBuilder.append(trackDataPerURLScript());

        if (request.getChecks() != null) {
            String checksScript = checksMapper.map(request);
            requestBuilder.append(checksScript);
        }
        requestBuilder.append(this.sleepScript());

        return requestBuilder.toString();
    }

    /**
     * Write a the beginning of Javascript-file
     *
     * @param baseURL The baseURL for all loadtests inside the inofficial k6-configuration
     * @param options The k6 'options' object
     * @return String that can be written at the beginning of a Javascript file
     * @throws JsonProcessingException
     */
    private String startScript(String baseURL, Options options) throws JsonProcessingException {
        String optionsString = objectMapper.writeValueAsString(options);
        String trackDataPerURL = this.trackDataPerURLInitScript();
        return """
                import http from 'k6/http';
                import {URLSearchParams} from 'https://jslib.k6.io/url/1.0.0/index.js';
                import {check, sleep} from 'k6';
                %s

                let baseURL = '%s';

                export let options = %s;
                                
                """.formatted(trackDataPerURL, baseURL, optionsString);
    }

    /**
     * Write functions to track data for every url used during the loadtest
     *
     * @return String that can be written inside a Javascript file
     */
    private String trackDataPerURLInitScript() {
        return """
                import {Counter} from 'k6/metrics';
                                
                export const epDataSent = new Counter('data_sent_endpoint');
                export const epDataRecv = new Counter('data_received_endpoint');
                                
                function sizeOfHeaders(headers) {
                    return Object.keys(headers).reduce((sum, key) => sum + key.length + headers[key].length, 0);
                }
                                
                function trackDataMetricsPerURL(res) {
                    epDataSent.add(sizeOfHeaders(res.request.headers) + res.request.body.length, { url: res.url });
                    epDataRecv.add(sizeOfHeaders(res.headers) + res.body.length, { url: res.url });
                }
                """;
    }

    private String trackDataPerURLScript() {
        return String.format("trackDataMetricsPerURL(response);%s", newLine);
    }

    private String sleepScript() {
        Random random = new Random();
        int duration = random.nextInt(5) + 1;
        return String.format("sleep(%d);%s",
                duration, newLine);
    }


}