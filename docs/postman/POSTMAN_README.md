# Shop Postman collection — Part 14

## Files to commit

Place these files in `docs/postman/`:

- `shop.postman_collection.json`
- `shop_local.postman_environment.json`
- `POSTMAN_README.md`
- `ENDPOINT_COVERAGE.md`

The collection uses Postman Collection v2.1. It contains 141 visible requests across 13 folders and covers all 84 HTTP routes declared in the supplied controllers, including route aliases. Each request asserts an exact HTTP status. Fixture HTTP requests also assert their expected status.

## Prerequisites

1. Apply SQL files `001` through `013`, in filename order, to an empty PostgreSQL database.
2. Start the shop application against that database. The payment simulation must be enabled (the default).
3. Keep the application running while using Postman.

The existing development database also works if the Part 13 seeds have been applied.

## Import and run

1. In Postman, use **Import** to import both JSON files.
2. Select the **Shop Local — Part 14** environment.
3. Set **baseUrl** to the running application, for example `http://localhost:8080`. For the separate verification server, use `http://localhost:8081`. Do not include `/api`.
4. Run the complete **Auth** folder, in order. Its login test stores `accessToken` and `refreshToken` in the environment automatically. A separate variable stores the admin token.
5. Run any other complete folder from its first request, **00 | Prepare isolated fixtures**. Folders do not depend on another business folder. They refresh tokens and create their own test customer, category, brand, products, address and promo. Orders, Payments and Admin also prepare a pending order.
6. You can also run the whole collection, using one iteration and the original request order.

Only **baseUrl** needs editing. IDs, slugs, generated test credentials, dates and tokens are set by scripts. Do not manually enter personal credentials or copy tokens.

## Demo credentials

The collection logs in the deliberately fake seeded admin `admin@shop.local` with the demonstration password `DemoShop1!`. The SQL seed stores a BCrypt hash. This is a local coursework fixture, not a real account. Auth and Users password/deactivation tests operate on freshly generated customers, never the seeded accounts.

Runtime tokens are blank in the exported environment. If you re-export your used environment, clear token values first.

## Folders

| Folder | Main checks |
|---|---|
| Auth | Registration, login, token saving, refresh, logout, password change, validation and invalid credentials |
| Products | List, search, detail, variants, related, featured, new arrivals, best sellers, 404 and invalid filters |
| Categories | Tree/list, ID/slug detail and products |
| Brands | List, detail and products |
| Users | Profile, password lifecycle, address lifecycle and disposable account deactivation |
| Cart | Add, update, merge, capped promo, remove promo/item, clear and invalid quantity |
| Wishlist | List, add, move to cart and remove |
| Orders | List, ID/number lookup, calculation, cancel, reorder, create and cart clearing |
| Payments | Luhn, all fixed decline scenarios, persisted failures, confirmed order state, successful retry and idempotency |
| Delivery | Methods, shipping threshold below/equal/above, estimated days and dates |
| Promo | Eligibility validation against a populated cart |
| Reviews | Create, update, approve, aggregate rating refresh, list/summary and delete |
| Admin | Category/brand/product/variant/promo CRUD, stock, activation, order filters/note/tracking/status flow and customer 403 |

## Expected error responses are passing tests

The collection deliberately triggers validation **400**, invalid credentials **401**, simulated payment failure **402**, forbidden **403**, not found **404**, and invalid order transition **409**. Their status/error-body tests should be green. Do not change those expected statuses to 200.

The runtime script checks `paymentMethod`, not `method`, in payment requests. Tracking is read from `trackingNumber` in order responses.

## Fixtures and repeated runs

Every folder gets unique names and a fresh customer. Re-running a folder therefore avoids duplicate email, slug, review and promo collisions. Seed products and accounts remain unchanged. Test data is retained in the local database so results can be inspected; account disabling and soft-delete requests are restricted to that run's own fixtures.

Run a folder from its beginning after a server restart or token expiry. If a fixture setup fails, inspect its named failed assertion before running dependent requests. A 401 on the seeded admin login usually means the application is connected to a database where the Part 13 admin seed has not been applied.

## Verification

Executed against the supplied, unmodified Java source, compiled with Java 21, and PostgreSQL 16.4 initialized with all 13 supplied SQL files. Hibernate schema validation passed.

- Complete collection: **141 visible requests**, **267 HTTP requests including setup**, **340 assertions**, **0 failures**.
- Independent execution: **Auth**, then all **12 business folders separately in reverse collection order**, each with a fresh environment: **340 assertions in total**, **0 failures**.
- Static route comparison: **84 of 84 controller routes covered**, including aliases; no missing routes.
- The exported environment contains no runtime access/refresh tokens.

These checks ran on a separate local test database, not on the user's development database.
