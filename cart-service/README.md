# Cart Service

This project is a Spring Boot application that implements a simple shopping cart service. Carts are stored in MongoDB and events are published to RabbitMQ.

## Running locally

Ensure Java 21 and Docker are installed. Start the required services:

```bash
docker-compose up -d
```

The services expose MongoDB on port `27017` and RabbitMQ on `5672` (management
UI on `15672`).

Run the application:

```bash
./mvnw spring-boot:run
```

Default configuration values can be overridden in
`src/main/resources/application.properties`.

## Running tests

```bash
./mvnw test
```

## Docker Compose services

The `docker-compose.yml` file starts MongoDB and RabbitMQ for local development.
It also includes a service definition for the Cart Service itself. When started
via Docker Compose, the application will be available on port `8084`.

The container uses environment variables to override defaults defined in
`application.properties`. Commonly used variables include:

| Variable | Description |
|----------|-------------|
| `CART_SERVICE_PORT` | Port that the application will listen on. |
| `SPRING_DATA_MONGODB_URI` | Connection URI for MongoDB. |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host name. |
| `SPRING_RABBITMQ_PORT` | RabbitMQ port. |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ username. |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ password. |
| `PRODUCT_SERVICE_URL` | Base URL of the Product Service. |
| `AUTH_SERVICE_URL` | Base URL of the Auth Service. |

These variables can be supplied when running the service.

To build and run the container directly:

```bash
docker build -t cart-service .
docker run -p 8084:8084 -e CART_SERVICE_PORT=8084 cart-service
```

## Cart Service API Documentation

This document outlines the API endpoints, data models, and event interactions for the Cart Service.

### 1. Introduction

The Cart Service is a Spring Boot application responsible for managing shopping carts. It stores cart data in MongoDB and communicates with other services via RabbitMQ and REST calls.

### 2. API Endpoints

The Cart Service exposes the following REST API endpoints.

**Base URL**

```
/api/carts
```

#### Add Item to Cart

**URL**: `POST /{cartIdentifier}/items`

**Description**: Adds a product to a specified cart. If the cart does not exist, it will be created. If the item already exists in the cart, its quantity will be updated.

**Request Body**:

```json
{
  "productId": "string",
  "quantity": "integer"
}
```

**Responses**:

- `200 OK`: Returns the updated Cart object.
- `400 Bad Request`: If the product is not found or there is insufficient stock.

#### View Cart

**URL**: `GET /{cartIdentifier}`

**Description**: Retrieves the cart for a given identifier.

**Responses**:

- `200 OK`: Returns the Cart object.
- `404 Not Found`: If the cart does not exist.

#### Update Item Quantity in Cart

**URL**: `PUT /{cartIdentifier}/items/{productId}`

**Description**: Updates the quantity of a specific product in a cart. If the quantity is set to `0` or less, the item will be removed from the cart. If the cart becomes empty after removal, the cart will be deleted.

**Request Body**:

```json
{
  "quantity": "integer"
}
```

**Responses**:

- `200 OK`: Returns the updated Cart object.
- `400 Bad Request`: If the cart or item is not found.

#### Remove Item from Cart

**URL**: `DELETE /{cartIdentifier}/items/{productId}`

**Description**: Removes a specific product from a cart. If the cart becomes empty after removal, the cart will be deleted.

**Responses**:

- `200 OK`: Returns the updated Cart object.
- `400 Bad Request`: If the cart is not found.

### 3. Data Models

#### Cart

Represents a user's shopping cart.

```java
public class Cart {
    private String id; // MongoDB document ID
    private String cartIdentifier; // Unique identifier for the cart (e.g., user ID or session ID)
    private List<CartItem> items; // List of items in the cart
    private LocalDateTime lastModifiedDate; // Timestamp of the last modification
}
```

#### CartItem

Represents a single product within a cart.

```java
public class CartItem {
    private String productId; // ID of the product
    private int quantity; // Quantity of the product
    private BigDecimal price; // Price of the product at the time it was added/updated in the cart
}
```

#### ProductDto

Data transfer object used when interacting with the Product Service.

```java
public class ProductDto {
    private String id; // Product ID
    private String name; // Product name
    private BigDecimal price; // Product price
    private int stock; // Current stock of the product
}
```

### 4. Event Interactions (RabbitMQ)

The Cart Service publishes and consumes messages via RabbitMQ for asynchronous communication.

**Queues Defined by Cart Service**

- `order.created.queue`: Used for events indicating that an order has been created.
- `product.updated.queue`: Carries notifications when a product's details change.

**Events Published by Cart Service**

##### ItemAddedToCartEvent

**Description**: Published when an item is successfully added to a cart.

**Exchange**: `cartExchange`
**Routing Key**: `cart.item.added`

**Payload**:

```java
public class ItemAddedToCartEvent {
    private String cartIdentifier;
    private String productId;
    private int quantityAdded;
}
```

**Events Consumed by Cart Service**

##### OrderCreatedEvent

**Description**: Listens to `order.created.queue`. When an order is created, the corresponding cart is cleared.

**Payload**:

```java
public class OrderCreatedEvent {
    private String cartIdentifier;
}
```

##### ProductUpdatedEvent

**Description**: Listens to `product.updated.queue`. When a product changes (name, price or stock), the Cart Service updates stored data as needed.

**Payload**:

```java
public class ProductUpdatedEvent {
    private Long productId;
    private String newName;
    private BigDecimal newPrice;
    private int newStock;
}
```

### 5. External Service Interactions

#### Product Service

The Cart Service interacts with a Product Service to retrieve product details when adding items to a cart.

**Endpoint**: `GET {product.service.url}/api/products/{productId}`
**Configured URL**: `http://localhost:8083`

