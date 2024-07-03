package dqualizer.dqexec

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(MockRabbitMQConnection::class)
class DqexecApplicationTests {

  @Test
  fun contextLoadsTest() {

  }

}
