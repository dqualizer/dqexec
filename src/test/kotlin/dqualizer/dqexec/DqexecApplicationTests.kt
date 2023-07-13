package dqualizer.dqexec

import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Checks
import dqualizer.dqlang.archive.k6adapter.dqlang.k6.request.Request
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@SpringBootTest
class DqexecApplicationTests {


    @Autowired
//    @Qualifier("jsRes")
    lateinit var templateEngine: SpringTemplateEngine

    @Test
    fun contextLoadsTest() {

    }

    @Test
    fun testTemplateEngine() {

        val request = Request("HTTP", "/v1/dosomething", mapOf(), mapOf(), mapOf(), mapOf(), Checks())

        val context = Context()
        context.setVariable("request", request)
        context.setVariable("payloads", listOf("{\n" +
                "      \"typ\": \"KUNDE_PRIVAT\",\n" +
                "      \"id\": 10,\n" +
                "      \"name\": \"Donnie\"\n" +
                "    }",
                "    {\n" +
                "      \"typ\": \"KUNDE_PRIVAT\",\n" +
                "      \"id\": 11,\n" +
                "      \"name\": \"Heiko\"\n" +
                "    }"))

        val out = templateEngine.process("classpath:k6/template.js", context)

        println(out)
    }

}
