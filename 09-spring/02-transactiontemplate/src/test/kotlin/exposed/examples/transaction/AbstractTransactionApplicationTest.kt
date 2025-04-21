package exposed.examples.transaction

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [TransactionTemplateApplication::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.exposed.generate-ddl=true"
    ]
)
abstract class AbstractTransactionApplicationTest {
}
