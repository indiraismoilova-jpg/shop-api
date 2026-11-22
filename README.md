# Shop API

Spring Boot shop backend with PostgreSQL, JWT authentication, role checks, product catalogue, reviews, carts, wishlists, promo codes, orders, simulated payments and delivery.

## Requirements

- JDK 21 or newer, with `JAVA_HOME` pointing to the JDK.
- Docker Desktop running, including Docker Compose v2.
- Git.
- Windows PowerShell 5.1 or PowerShell 7.
- Postman for the exported collection.

Maven is provided by `mvnw` / `mvnw.cmd`. A separate Maven installation is unnecessary.

Run commands from the repository directory containing `pom.xml`, `compose.yaml` and `mvnw.cmd`.

## First run from a clean clone

Clone this repository using its Git hosting URL:

```powershell
git clone <repository-url> shop-clean
cd shop-clean
```

Allow scripts for the current PowerShell process, then create/load local configuration:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\scripts\Set-LocalEnvironment.ps1
.\scripts\Initialize-Database.ps1
```

The environment script creates a local `.env` with a generated database password and a Base64 JWT key. It loads the values into the current process; Spring Boot does not automatically read `.env`. The file is ignored by Git. Existing unrelated `.env` lines are preserved. If a matching existing PostgreSQL container is found and no database password is configured, its configured password is reused.

The database script starts PostgreSQL, waits until it is ready, and applies all 14 numbered SQL files in order. Each file and its checksum are recorded in `schema_migrations`. Re-running the script skips unchanged applied files. A failed migration rolls back together with its ledger entry. Do not edit a migration after it has been recorded.

On an empty database, the final counts are:

| Entity | Count |
|---|---:|
| Categories | 5 |
| Brands | 3 |
| Products | 20 |
| Users | 2 |
| Orders | 5 |
| Reviews | 10 |
| Promo codes | 3 |

Build and run in the same terminal:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Wait for `Started ShopApplication`. The default API address is `http://localhost:8080`. Keep this terminal running and use another terminal or Postman for requests.

The build runs the existing `ShopApplicationTests.contextLoads` test against the configured PostgreSQL database. Initialize that database and load the environment before building. Hibernate uses `ddl-auto=validate`; SQL files, rather than Hibernate, create the schema.

On Linux/macOS with PowerShell 7, use the same `.ps1` scripts, then use `bash ./mvnw clean install` and `bash ./mvnw spring-boot:run` instead of the Windows wrapper. `Set-ExecutionPolicy` is only needed on Windows.

## Fresh verification alongside an existing development database

Use an empty clone directory and a different Compose project and database port:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
$verifyProject = "shopverify" + (Get-Date -Format "yyyyMMddHHmmss")
. .\scripts\Set-LocalEnvironment.ps1 -Port 5434 -ProjectName $verifyProject
.\scripts\Initialize-Database.ps1
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
```

The different Compose project gives this run its own PostgreSQL volume. It does not reset the development database. Set Postman's `baseUrl` to `http://localhost:8082` for this server.

A database previously initialized manually has no migration ledger. The initializer refuses to assume its migration history. Continue using that development database without running the initializer, or perform the fresh verification above.

## Later starts and terminal restarts

Local environment variables belong to a terminal session. Load them before starting Maven in a new terminal:

```powershell
. .\scripts\Set-LocalEnvironment.ps1
docker compose up -d
.\mvnw.cmd spring-boot:run
```

For a manually initialized development database, the environment loader can reuse the existing Compose PostgreSQL container password. Its default Compose project is `shop`.

Stop the API with `Ctrl+C`. `docker compose down` stops the configured Compose stack without deleting its database volume. Run it from a terminal where the matching environment has been loaded.

## Configuration

| Variable | Meaning |
|---|---|
| `DB_USERNAME` | PostgreSQL username; defaults to `shop` |
| `DB_PASSWORD` | Generated or existing local database password |
| `DB_PORT` | Host PostgreSQL port; defaults to `5433` |
| `DB_URL` | JDBC connection URL; generated from the selected local port |
| `COMPOSE_PROJECT_NAME` | Selects the Compose stack and its volume; defaults to `shop` |
| `JWT_SECRET` | Base64 key, generated once locally and reused |
| `PAYMENT_SIMULATION_ENABLED` | Defaults to `true` |
| `SHOP_CURRENCY` | Defaults to `USD` |

Compose passes the local database credentials to PostgreSQL. The environment loader passes them to the Spring Boot process. Do not commit `.env`, runtime JWTs or personal credentials.

## Demo accounts

| Role | Email | Demo password |
|---|---|---|
| ADMIN | `admin@shop.local` | `DemoShop1!` |
| CUSTOMER | `customer@shop.local` | `DemoShop1!` |

These are deliberately fake coursework accounts. SQL stores their passwords as BCrypt hashes. The seeded orders have PENDING, CONFIRMED, PAID, SHIPPED and DELIVERED states. Unpaid orders have reservations; paid orders have completed simulated payments and deducted stock.

Demo promos: `DEMO10` (10%, minimum 50, maximum discount 100), `DEMO20` (20 fixed, minimum 100), `DEMOSHIP` (free shipping, minimum 100).

## Postman

Import both files from `docs/postman/`:

- `shop.postman_collection.json`
- `shop_local.postman_environment.json`

Select **Shop Local — Part 14**, change only `baseUrl`, then run **Auth** followed by any complete folder, or run the whole collection in order with one iteration.

Login stores `accessToken` and `refreshToken` automatically. Each other folder starts by preparing isolated fixtures and refreshing its tokens. No IDs or personal credentials need manual editing. Validation, invalid credentials, failed payments, forbidden access, not-found resources and illegal transitions deliberately return 400/401/402/403/404/409 and have passing assertions for those codes.

The collection contains 141 visible requests covering all 84 declared controller routes, including aliases. Both the separate automated run and the user's Postman run completed with **340 passed assertions, 0 failed, 0 errors**. Detailed usage and route coverage are in `docs/postman/POSTMAN_README.md` and `docs/postman/ENDPOINT_COVERAGE.md`.

## Database deliverables

- `src/main/resources/db/001_*.sql` through `007_*.sql`: schema and initial indexes.
- `008_delivery_seed.sql`: delivery options.
- `009_extra_indexes.sql`: additional filter/sort indexes.
- `010_reference_seed.sql`: demo accounts, categories and brands.
- `011_products_seed.sql`: 20 products.
- `012_orders_seed.sql`: orders, order items, payments, reservations and stock counters.
- `013_reviews_promos_seed.sql`: reviews, product rating aggregates and promo codes.
- `docs/database/shop_er.puml`: editable PlantUML ER source.
- `docs/database/shop_er.png`: exported ER diagram, containing 21 domain tables and 29 foreign-key relations.

`schema_migrations` is script metadata rather than a domain entity. Delivery methods and copied promo codes are application values; shipping addresses are historical snapshots embedded in orders.

## Main behaviour

- Products are soft-deleted. Available stock is stock quantity minus reserved quantity.
- Cart lines use current prices and flag changes from `priceAtAdd`; duplicate guest items merge by adding quantities.
- Promo validation checks dates, limits, applicable items and the cart minimum; discounts respect their cap.
- Order creation reserves stock and clears the cart in one transaction; cancellation releases reservations where applicable.
- Paid orders commit reserved stock. Repeating a completed payment returns its existing transaction.
- Only allowed order transitions are accepted. DELIVERED refunds require the optional return workflow.
- Approved reviews refresh the product's average rating and review count.
- Delivery fees follow the selected option and free-shipping threshold; dates and tracking are simulated.

## Simulated card payments

Use `paymentMethod`, not `method`, in payment JSON. Luhn validation happens before processing. Only the last four card digits are persisted.

| Test card | Result |
|---|---|
| `4111111111111111` | Always succeeds |
| `4000000000000002` | Insufficient funds |
| `4000000000000069` | Network error |
| `4000000000000077` | Suspected fraud |
| Other Luhn-valid cards | 90% success, 10% random failure |

The gateway simulates a 1–3 second delay. Failed payments leave the order CONFIRMED, never PAID. These are simulation test numbers, not real payment credentials.

## Scope

Required core modules are implemented. Part 12 optional modules (bonus, returns, support tickets, email delivery, Redis, rate limiting, scheduled jobs, Swagger/OpenAPI, AOP/events and the upload/MapStruct/Specifications bundle) are not claimed as completed. Swagger is not installed merely because its paths are permitted by security configuration.


## Product details
014_product_details_seed.sql adds 20 demo product images and 40 specifications. Product responses include images and specifications. The demo image is served at /images/demo-product.svg.
