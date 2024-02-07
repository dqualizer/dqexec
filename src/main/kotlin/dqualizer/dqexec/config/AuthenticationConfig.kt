package dqualizer.dqexec.config
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${dqualizer.dqexec.mysql.host}")
    private lateinit var mySqlHost: String
    @Value("\${dqualizer.dqexec.mysql.port}")
    private lateinit var mySqlPort: String
    private lateinit var dbUsername: String
    private lateinit var dbPassword: String
    private lateinit var username: String
    private lateinit var password: String
    private var isUserAuthenticated: Boolean = false


    @Bean
    fun init(): CommandLineRunner {
        return CommandLineRunner {
            collectUserAuthenticationInput()
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
            DriverManager.getConnection("jdbc:mysql://${mySqlHost}:${mySqlPort}/authentication", getDbUsername(), getDbPassword()).use { connection ->

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

    private fun collectUserAuthenticationInput() {
        val scanner = Scanner(System.`in`)

        print("###\n" +
                "###\n" +
                "###\n")

        println(">>>>>>>>>>>>>> Please enter MySQL username: ")
        dbUsername = scanner.nextLine()

        // Hides the password input, but is only available with true console usage, not from IDE
        if (System.console() != null){
            dbPassword = System.console().readPassword(">>>>>>>>>>>>>> Please enter MySQL password: ").joinToString("")
        } else{
            println(">>>>>>>>>>>>>> Please enter MySQL password: ")
            dbPassword = scanner.nextLine()
        }

        println(">>>>>>>>>>>>>> Please enter authentication username: ")
        username = scanner.nextLine()

        // Hides the password input, but is only available with true console usage, not from IDE
        if (System.console() != null){
            password = System.console().readPassword(">>>>>>>>>>>>>> Please enter authentication password: ").joinToString("")
        } else{
            println(">>>>>>>>>>>>>> Please enter authentication password: ")
            password = scanner.nextLine()
        }
        scanner.close()
    }

    fun getUsername(): String = username
    fun getPassword(): String = password
    fun getDbUsername(): String = dbUsername
    fun getDbPassword(): String = dbPassword
    fun isUserAuthenticated(): Boolean = isUserAuthenticated

}