package database

import (
	"database/sql"
	"fmt"
	"log"
	"time"
	"user-service/config"
	"user-service/models" // Make sure models package is imported

	_ "github.com/lib/pq" // PostgreSQL driver
)

// DB struct holds the sql.DB instance.
type DB struct {
	SQL *sql.DB
}

// NewConnection establishes a new PostgreSQL database connection with retry logic.
func NewConnection(cfg *config.Config) *DB {
	// Ensure cfg.DBPort is used as a string in the connection string
	connStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		cfg.DBHost, cfg.DBPort, cfg.DBUser, cfg.DBPassword, cfg.DBName)

	var db *sql.DB
	var err error
	maxRetries := 10
	retryInterval := 5 * time.Second

	for i := 0; i < maxRetries; i++ {
		log.Printf("Attempting to connect to database... Attempt %d/%d", i+1, maxRetries)
		db, err = sql.Open("postgres", connStr)
		if err != nil {
			log.Printf("Failed to open database connection: %v", err)
			time.Sleep(retryInterval)
			continue
		}

		err = db.Ping() // Check if the connection is alive
		if err == nil {
			log.Println("✅ PostgreSQL connection successful!")
			dbWrapper := &DB{SQL: db}
			dbWrapper.createTables() // Ensure tables are created
			return dbWrapper
		}
		log.Printf("Failed to ping database: %v. Retrying...", err)
		db.Close() // Close the current connection before retrying
		time.Sleep(retryInterval)
	}

	log.Fatalf("❌ Failed to connect to database after %d attempts, application cannot start.", maxRetries)
	return nil // This line should not be reached as log.Fatalf exits
}

// createTables creates the necessary database tables if they don't exist.
func (db *DB) createTables() {
	// IMPORTANT: No 'password', 'bio', 'phone' columns here.
	createUsersTableSQL := `
	CREATE TABLE IF NOT EXISTS users (
		id SERIAL PRIMARY KEY,
		name VARCHAR(100) NOT NULL,
		email VARCHAR(100) UNIQUE NOT NULL,
		created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
	);`

	_, err := db.SQL.Exec(createUsersTableSQL) // Use db.SQL for execution
	if err != nil {
		log.Fatalf("❌ Failed to create users table: %v", err)
	}
	log.Println("✅ Database tables are ready!")
}

// InsertUser inserts a new user profile into the database.
func (d *DB) InsertUser(user *models.User) error {
	query := `INSERT INTO users (name, email) VALUES ($1, $2) RETURNING id, created_at, updated_at`
	// Scan into user.ID, user.CreatedAt, user.UpdatedAt
	err := d.SQL.QueryRow(query, user.Name, user.Email).
		Scan(&user.ID, &user.CreatedAt, &user.UpdatedAt)
	if err != nil {
		return fmt.Errorf("failed to insert user: %w", err)
	}
	return nil
}

// GetAllUsers retrieves all users from the database.
func (d *DB) GetAllUsers() ([]models.User, error) {
	rows, err := d.SQL.Query("SELECT id, name, email, created_at, updated_at FROM users")
	if err != nil {
		return nil, fmt.Errorf("failed to get all users: %w", err)
	}
	defer rows.Close()

	var users []models.User
	for rows.Next() {
		var user models.User
		// Scan into user.ID, user.Name, user.Email, user.CreatedAt, user.UpdatedAt
		if err := rows.Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt); err != nil {
			return nil, fmt.Errorf("failed to scan user row: %w", err)
		}
		users = append(users, user)
	}
	if err = rows.Err(); err != nil {
		return nil, fmt.Errorf("rows iteration error: %w", err)
	}
	return users, nil
}

// GetUserByID retrieves a user by their ID.
func (d *DB) GetUserByID(id int) (*models.User, error) {
	var user models.User
	query := "SELECT id, name, email, created_at, updated_at FROM users WHERE id = $1"
	// Scan into user.ID, user.Name, user.Email, user.CreatedAt, user.UpdatedAt
	err := d.SQL.QueryRow(query, id).Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt)
	if err == sql.ErrNoRows {
		return nil, nil // User not found
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get user by ID: %w", err)
	}
	return &user, nil
}

// UpdateUser updates an existing user's information.
func (d *DB) UpdateUser(user *models.User) error {
	query := `UPDATE users SET name = $1, email = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3`
	_, err := d.SQL.Exec(query, user.Name, user.Email, user.ID)
	if err != nil {
		return fmt.Errorf("failed to update user: %w", err)
	}
	return nil
}

// DeleteUser deletes a user by their ID.
func (d *DB) DeleteUser(id int) error {
	query := `DELETE FROM users WHERE id = $1`
	_, err := d.SQL.Exec(query, id)
	if err != nil {
		return fmt.Errorf("failed to delete user: %w", err)
	}
	return nil
}
