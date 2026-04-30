# Secrets Inventory

| Name | Type | File:line | secret_id |
|---|---|---|---|
| DB_PASSWORD | PostgreSQL password | src/main/resources/application.yaml:25 | DB_PASSWORD |
| DB_PASSWORD | PostgreSQL password | src/main/resources/application.yaml:158 | DB_PASSWORD |
| DB_PASSWORD | PostgreSQL password | deploy/cloudrun.env.yaml:5 | DB_PASSWORD |
| DB_PASSWORD | PostgreSQL password duplicate build artifact | target/classes/application.yaml:25 | DB_PASSWORD |
| DB_PASSWORD | PostgreSQL password duplicate build artifact | target/classes/application.yaml:158 | DB_PASSWORD |
| REDIS_PASSWORD | Redis password default | src/main/resources/application.yaml:39 | REDIS_PASSWORD |
| REDIS_PASSWORD | Redis password duplicate build artifact | target/classes/application.yaml:39 | REDIS_PASSWORD |
| GOOGLE_CLIENT_ID | Google OAuth client ID | src/main/resources/application.yaml:82 | GOOGLE_CLIENT_ID |
| GOOGLE_CLIENT_ID | Google OAuth client ID | deploy/cloudrun.env.yaml:9 | GOOGLE_CLIENT_ID |
| GOOGLE_CLIENT_ID | Google OAuth client ID duplicate build artifact | target/classes/application.yaml:82 | GOOGLE_CLIENT_ID |
| GOOGLE_CLIENT_SECRET | Google OAuth client secret | src/main/resources/application.yaml:83 | GOOGLE_CLIENT_SECRET |
| GOOGLE_CLIENT_SECRET | Google OAuth client secret | deploy/cloudrun.env.yaml:10 | GOOGLE_CLIENT_SECRET |
| GOOGLE_CLIENT_SECRET | Google OAuth client secret duplicate build artifact | target/classes/application.yaml:83 | GOOGLE_CLIENT_SECRET |
| MAIL_PASSWORD | Gmail SMTP app password | src/main/resources/application.yaml:91 | MAIL_PASSWORD |
| MAIL_PASSWORD | Gmail SMTP app password | deploy/cloudrun.env.yaml:13 | MAIL_PASSWORD |
| MAIL_PASSWORD | Gmail SMTP app password duplicate build artifact | target/classes/application.yaml:91 | MAIL_PASSWORD |
| APP_REMEMBER_ME_KEY | Spring Security remember-me signing key | src/main/resources/application.yaml:103 | APP_REMEMBER_ME_KEY |
| APP_REMEMBER_ME_KEY | Spring Security remember-me signing key duplicate build artifact | target/classes/application.yaml:103 | APP_REMEMBER_ME_KEY |

No JSON, PEM, P12, PFX, JKS, keystore, or private-key files were found in the project tree.