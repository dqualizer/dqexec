package dqualizer.dqexec.adapter

import dqualizer.dqexec.config.ResourcePaths
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
class ThymeleafTemplateEngineFactory(
  private val applicationContext: ApplicationContext,
  private val resourcePaths: ResourcePaths,
) {
  @Cacheable("templateEngines")
  fun getEngine(
    templateMode: TemplateMode,
    prefix: String = "",
  ): SpringTemplateEngine {
    val templateResolver = SpringResourceTemplateResolver()
    templateResolver.setApplicationContext(applicationContext)
    templateResolver.prefix = prefix
    templateResolver.templateMode = templateMode
    templateResolver.isCacheable = false

    val templateEngine = SpringTemplateEngine()
    templateEngine.setTemplateResolver(templateResolver)

    // Enabling the SpringEL compiler with Spring 4.2.4 or newer can
    // speed up execution in most scenarios, but might be incompatible
    // with specific cases when expressions in one template are reused
    // across different data types, so this flag is "false" by default
    // for safer backwards compatibility.
    templateEngine.enableSpringELCompiler = true

    return templateEngine
  }

  @Bean
  @Primary
  fun templateEngine(): SpringTemplateEngine {
    return getEngine(TemplateMode.TEXT)
  }

  @Bean
  @Qualifier("resource")
  fun templateEngineRes(): SpringTemplateEngine {
    return getEngine(TemplateMode.TEXT, "classpath:")
  }

  @Bean("templateEngineJS")
  @Qualifier("js")
  fun templateEngineJS(): SpringTemplateEngine {
    return getEngine(TemplateMode.JAVASCRIPT)
  }

  @Bean("templateEngineJSRes")
  @Qualifier("jsRes")
  fun templateEngineJSRes(): SpringTemplateEngine {
    return getEngine(TemplateMode.JAVASCRIPT, "classpath:")
  }
}
