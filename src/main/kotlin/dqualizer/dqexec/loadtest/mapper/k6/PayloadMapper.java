package dqualizer.dqexec.loadtest.mapper.k6;

import dqualizer.dqexec.config.PathConfig;
import dqualizer.dqexec.exception.NoReferenceFoundException;
import dqualizer.dqexec.util.SafeFileReader;
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the payload to Javascript-Code
 */
@Component
@RequiredArgsConstructor
public class PayloadMapper implements k6Mapper {

    private final PathConfig paths;
    private final SafeFileReader reader;

    @Override
    public String map(Request request) {
        Map<String, String> payload = request.getPayload();
        Optional<String> maybeReference = payload.values().stream().findFirst();
        if(maybeReference.isEmpty()) throw new NoReferenceFoundException(payload);

        String referencePath = paths.getResourcePath() + maybeReference.get();
        String payloadObject = reader.readFile(referencePath);

        return """
                const payloadData = %s
                const payloads = payloadData['payloads'];
                
                """.formatted(payloadObject);
    }
}