package dqualizer.dqexec.loadtest.mapper.k6;

import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.util.SafeFileReader;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Maps the request-parameter to Javascript-Code
 */
@Component
@RequiredArgsConstructor
public class ParamsMapper implements k6Mapper {

    private final PathConfig paths;
    private final SafeFileReader reader;

    @Override
    public String map(Request request) {
        Map<String, String> params = request.getParams();
        Optional<String> maybeReference = params.values().stream().findFirst();
        if(maybeReference.isEmpty()) return String.format("%sconst params = {}%s", newLine, newLine);

        String referencePath = paths.getResourcePath() + maybeReference.get();
        String paramsObject = reader.readFile(referencePath);

        return String.format("%sconst params = %s%s",
                newLine, paramsObject, newLine);
    }
}