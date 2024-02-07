package dqualizer.dqexec.config
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import kotlin.system.exitProcess

@Configuration
class StartupConfig {

    private lateinit var dbUsername: String
    private lateinit var dbPassword: String
    private lateinit var username: String
    private lateinit var password: String
    private var isUserAuthenticated: Boolean = false


    @Bean
    fun init(): CommandLineRunner {
        return CommandLineRunner {
            collectUserInput()
            authenticateUser()
        }
    }

    private fun hashString(input:String): String {
        var digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        var encodedhash: ByteArray = digest.digest(
                input.toByteArray(StandardCharsets.UTF_8))

        val hexString: StringBuilder = StringBuilder(2 * encodedhash.size)
        for (i in encodedhash.indices) {
            val hex = Integer.toHexString(0xff and encodedhash.get(i).toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private fun authenticateUser() {
        try {
            DriverManager.getConnection("jdbc:mysql://localhost:3306/authentication", getDbUsername(), getDbPassword()).use { connection ->

                val query = "SELECT COUNT(*) FROM users WHERE username = ? AND password_hash = ?"
                val preparedStatement = connection.prepareStatement(query)

                preparedStatement.setString(1, getUsername())
                preparedStatement.setString(2, hashString(password))

                val resultSet = preparedStatement.executeQuery()

                if (resultSet.next() && resultSet.getInt(1) > 0) {
                    println("Authentication successful! Ready to execute tests.")
                    isUserAuthenticated=true;
                } else {
                    println("Authentication failed! Restart application to try again.")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            exitProcess(401)
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
    fun isUserAuthenticated(): Boolean = isUserAuthenticated

}