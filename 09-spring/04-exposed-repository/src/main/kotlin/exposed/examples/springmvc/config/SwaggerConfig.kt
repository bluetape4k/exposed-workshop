package exposed.examples.springmvc.config

import io.bluetape4k.support.uninitialized
import io.bluetape4k.support.unsafeLazy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger 설정을 위한 Configuration
 */
@Configuration
class SwaggerConfig {

    @Autowired
    private val buildProps: BuildProperties = uninitialized()

    @Bean
    fun apiInfo(): OpenAPI {
        return OpenAPI().info(info)
    }

    private val info by unsafeLazy {
        Info().title(buildProps.name)
            .description("Spring MVC + Virtual Threads + ExposedRepository 예제입니다")
            .version(buildProps.version)
            .contact(contact)
            .license(license)
    }

    private val contact by unsafeLazy {
        Contact()
            .name("Exposed Workshop")
            .email("sunghyouk.bae@gmail.com")
            .url("https://github.com/bluetape4k/exposed-workshop")
    }

    private val license by unsafeLazy {
        License()
            .name("Bluetape4k License 1.0")
            .url("https://bluetape4k.io/license")
    }
}
