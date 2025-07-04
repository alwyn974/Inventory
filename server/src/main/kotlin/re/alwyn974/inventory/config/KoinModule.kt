package re.alwyn974.inventory.config

import org.koin.dsl.module
import re.alwyn974.inventory.service.DatabaseFactory
import re.alwyn974.inventory.service.JwtService
import re.alwyn974.inventory.service.MinioService
import re.alwyn974.inventory.service.PasswordService

val appModule = module {

    // Configuration
    single { AppConfig }

    // Services
    single { DatabaseFactory(get()) }
    single { JwtService(get()) }
    single { PasswordService }
    single {
        MinioService(
            endpoint = get<AppConfig>().minioEndpoint,
            accessKey = get<AppConfig>().minioAccessKey,
            secretKey = get<AppConfig>().minioSecretKey,
            bucketName = get<AppConfig>().minioBucketName
        )
    }
}
