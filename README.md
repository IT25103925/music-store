# 🎸 Melodia — Web-based Music Store
**SLIIT — SE2030 Software Engineering — Group Project**

A full-stack Java web application for buying and renting musical instruments,
built with **Spring Boot 3**, **Spring Data JPA**, **Spring Security**, and
**Thymeleaf**.

---

## 1. Group Member → Module Mapping

| Member | Module | Key classes |
|---|---|---|
| **Member 1** | Product Management | `Product`, `Category`, `ProductService`, `ProductController`, `AdminProductController` |
| **Member 2** | Order Management (Cart & Wishlist) | `Cart`, `CartItem`, `Wishlist`, `WishlistItem`, `Order`, `OrderItem`, `CartService`, `WishlistService`, `OrderService`, `CartController`, `WishlistController`, `OrderController`, `AdminOrderController` |
| **Member 3** | Payment Management | `Payment`, `PaymentService`, `PaymentController`, `AdminPaymentController` |
| **Member 4** | Instrument Rental Management | `Rental`, `RentalService`, `RentalController`, `AdminRentalController` |
| **Member 5** | Offer & Discount Management | `Offer`, `OfferService`, `OfferController`, `AdminOfferController` |
| **Member 6** | Review, Rating & Notification Management | `Review`, `Notification`, `ReviewService`, `NotificationService`, `ReviewController`, `NotificationController`, `AdminReviewController` |

Every entity, service, and controller file has a one-line Javadoc comment at
the top naming which member's module it belongs to — use this as your guide
when preparing individual contribution write-ups for the report.

**How the modules connect** (good material for your System Overview /
sequence diagrams):
- Product (M1) underpins everything — Cart, Orders, and Rentals all
  reference `Product`.
- Order Management (M2) calls into Offer & Discount (M5) at checkout to
  apply a coupon, and into Payment (M3) to collect money.
- Rental (M4) also uses Payment (M3) to charge the rental fee + deposit.
- Every state change (order placed, payment made, rental approved, review
  posted) raises a `Notification` through M6's `NotificationService`.

---

## 2. Tech Stack

- Java 17
- Spring Boot 3.3 (Web MVC, Data JPA, Security, Validation, Mail)
- Thymeleaf (server-rendered HTML — no separate frontend build needed)
- H2 (file-based, zero-setup) by default; MySQL profile included for your
  final deployment
- Maven

---

## 3. Running the Project

### Option A — H2 (fastest, no database installation needed)
```bash
mvn spring-boot:run
```
Then open **http://localhost:8080**

The app auto-creates `./data/musicstore.mv.db` on first run and seeds it
with demo data (see credentials below). The H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/musicstore`).

### Option B — MySQL (recommended for your final submission/demo)
1. Install MySQL and make sure it's running.
2. Update `src/main/resources/application-mysql.properties` with your
   MySQL username/password.
3. Run:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
The `musicstore_db` database and all tables are created automatically by
Hibernate (`ddl-auto=update`) — you don't need to write any SQL by hand.

### Building a runnable JAR
```bash
mvn clean package
java -jar target/music-store.jar
```

---

## 4. Demo Accounts (seeded automatically)

| Role | Email | Password |
|---|---|---|
| Admin | `admin` + `@` + `musicstore.lk` | `Admin@123` |
| Customer | `customer` + `@` + `musicstore.lk` | `Customer@123` |

Sample offer codes seeded for testing checkout discounts: `WELCOME10` (10%
off, min order LKR 5,000) and `SAVE2000` (flat LKR 2,000 off, min order
LKR 20,000).

You can also register a brand-new customer account from `/register`.

---

## 5. Project Structure

```
src/main/java/com/sliit/musicstore/
 ├── entity/          JPA entities (one table each)
 ├── repository/      Spring Data JPA repositories
 ├── service/          Interfaces (one per module)
 │    └── impl/        Implementations — the actual business logic
 ├── controller/       Spring MVC controllers (customer-facing + /admin/*)
 └── config/           Security config, file-upload config, demo data seeder

src/main/resources/
 ├── templates/        Thymeleaf HTML views, organised by module
 ├── static/css/       Stylesheet
 └── application*.properties
```

## 6. Customer-Facing Pages

| URL | Description |
|---|---|
| `/` | Home page with categories & featured instruments |
| `/products` | Browse / search / filter the catalogue |
| `/products/{id}` | Product detail, reviews, add to cart/wishlist |
| `/cart` | Cart with offer code entry |
| `/wishlist` | Saved items |
| `/orders/checkout` → `/payment/pay` | Checkout & payment |
| `/orders` | Order history |
| `/rentals` | Browse rentable instruments |
| `/rentals/request/{productId}` | Submit a rental request |
| `/rentals/my` | My rental requests |
| `/offers` | Active discount codes |
| `/notifications` | In-app notifications |

## 7. Admin Pages (login as the admin account above)

`/admin/dashboard`, `/admin/products`, `/admin/orders`, `/admin/payments`,
`/admin/rentals`, `/admin/offers`, `/admin/reviews`

---

## 8. Notes for Your Report / Viva

- **Why Spring Boot?** It's the most widely used enterprise Java web
  framework, matches the "Java-based web application" requirement in the
  brief, and its layered architecture (Controller → Service → Repository →
  Entity) maps directly onto a clean n-tier design you can draw in your
  architecture diagram.
- **Payment Management (M3)** in this prototype *simulates* a gateway
  (no real card is charged) — this is normal and expected for a campus
  project; the `PaymentServiceImpl` class explains exactly where you'd plug
  in a real gateway SDK (e.g. Stripe, PayHere) later.
- **Notifications (M6)** are always stored in-app; email sending is wired
  up via Spring Mail but disabled by default (`app.notifications.email-enabled=false`
  in `application.properties`) since it needs real SMTP credentials.
- Make sure every member can explain their own module's entity, service,
  and controller in detail — this is exactly what the marking rubric
  checks for in the viva.

---

## 9. Troubleshooting

### "required a bean named 'entityManagerFactory' that could not be found" on startup
This means the `hibernate-core` jar didn't download properly into your local
Maven cache (`~/.m2` or `C:\Users\<you>\.m2`) — it's a local-machine/cache
issue, not a bug in the code. Fix it once with:

```bash
cd musicstore
mvn clean install -U
```

If the same error still appears afterwards, delete the corrupted Hibernate
folder from your local repo and force a fresh download:

**Windows:**
```bash
rmdir /s "%USERPROFILE%\.m2\repository\org\hibernate"
mvn clean install -U
```
**macOS/Linux:**
```bash
rm -rf ~/.m2/repository/org/hibernate
mvn clean install -U
```

Then in IntelliJ: right-click `pom.xml` → **Maven → Reload Project**, and/or
**File → Invalidate Caches / Restart**. You can confirm the fix worked with:
```bash
mvn dependency:tree | grep hibernate-core
```
(use `findstr` instead of `grep` on Windows) — you should see a
`hibernate-core-x.x.Final.jar` line.

## 10. Known Simplifications (be upfront about these in your report)

- Card payments are simulated, not connected to a real gateway.
- Email notifications require SMTP credentials to actually send (in-app
  notifications always work).
- No automated test suite is included — consider adding JUnit tests for
  each service class as a stretch goal for extra marks.
