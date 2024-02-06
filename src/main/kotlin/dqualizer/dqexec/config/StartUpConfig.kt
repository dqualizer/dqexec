package dqualizer.dqexec.config
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import kotlin.time.measureTimedValue

@Configuration
class StartupConfig {

    private lateinit var dbUsername: String
    private lateinit var dbPassword: String
    private lateinit var username: String
    private lateinit var password: String

    @Bean
    fun init(): CommandLineRunner {
        return CommandLineRunner {
            collectUserInput()
        }
    }

    private fun collectUserInput() {
        val scanner = Scanner(System.`in`)

        System.out.print("###\n" +
                "###\n" +
                "###\n")

        System.out.flush()
        System.out.println(">>>>>>>>>>>>>> Please enter MySQL username: ")
        System.out.flush()
        dbUsername = scanner.nextLine()

        //TODO hide input in console
        System.out.println(">>>>>>>>>>>>>> Please enter MySQL password: ")
        System.out.flush()
        dbPassword = scanner.nextLine()

        System.out.println(">>>>>>>>>>>>>> Please enter authentication username: ")
        System.out.flush()
        username = scanner.nextLine()

        //TODO hide input in console
        System.out.println(">>>>>>>>>>>>>> Please enter authentication password: ")
        System.out.flush()
        password = scanner.nextLine()

        scanner.close()
    }

    fun getUsername(): String = username
    fun getPassword(): String = password
    fun getDbUsername(): String = dbUsername
    fun getDbPassword(): String = dbPassword

}