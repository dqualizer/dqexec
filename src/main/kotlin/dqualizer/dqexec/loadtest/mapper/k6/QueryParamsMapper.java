package dqualizer.dqexec.loadtest.mapper.k6;

import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.exception.NoReferenceFoundException;
import dqualizer.dqexec.util.SafeFileReader;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Maps the url-parameter to Javascript-Code
 */
@Component
@RequiredArgsConstructor
public class QueryParamsMapper implements k6Mapper {

    private final PathConfig paths;
    private final SafeFileReader reader;

    @Override
    public String map(Request request) {
        Map<String, String> queryParams = request.getQueryParams();
        Optional<String> maybeReference = queryParams.values().stream().findFirst();
        if(maybeReference.isEmpty()) throw new NoReferenceFoundException(queryParams);

        String referencePath = paths.getResourcePath() + maybeReference.get();
        String queryParamsObject = reader.readFile(referencePath);

        return """
                const queryParams = %s
                const searchParams = queryParams['params'];
                
                """.formatted(queryParamsObject);
    }
}
