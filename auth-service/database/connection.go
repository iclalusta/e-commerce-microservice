package database

import (
	"auth-service/config"
	"auth-service/models" // Make sure models package is imported
	"database/sql"
	"fmt"
	"log"
	"time"

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
		log.Printf("Attempting to connect to Auth database... Attempt %d/%d", i+1, maxRetries)
		db, err = sql.Open("postgres", connStr)
		if err != nil {
			log.Printf("Failed to open Auth database connection: %v", err)
			time.Sleep(retryInterval)
			continue
		}

		err = db.Ping() // Check if the connection is alive
		if err == nil {
			log.Println("✅ PostgreSQL Auth DB connection successful!")
			dbWrapper := &DB{SQL: db}
			dbWrapper.createTables() // Ensure tables are created
			return dbWrapper
		}
		log.Printf("Failed to ping Auth database: %v. Retrying...", err)
		db.Close() // Close the current connection before retrying
		time.Sleep(retryInterval)
	}

	log.Fatalf("❌ Failed to connect to Auth database after %d attempts, application cannot start.", maxRetries)
	return nil // This line should not be reached as log.Fatalf exits
}

// createTables creates the necessary database tables if they don't exist for Auth Service.
func (db *DB) createTables() {
	createAuthUsersTableSQL := `
	CREATE TABLE IF NOT EXISTS auth_users (
		id SERIAL PRIMARY KEY,
		name VARCHAR(100) NOT NULL,
		email VARCHAR(100) UNIQUE NOT NULL,
		password_hash VARCHAR(255) NOT NULL,
		created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
	);`

	_, err := db.SQL.Exec(createAuthUsersTableSQL) // Use db.SQL for execution
	if err != nil {
		log.Fatalf("❌ Failed to create auth_users table: %v", err)
	}
	log.Println("✅ Auth database tables are ready!")
}

// InsertAuthUser inserts a new user's authentication data into the database.
func (d *DB) InsertAuthUser(user *models.AuthUser) (int, error) {
	query := `INSERT INTO auth_users (name, email, password_hash) VALUES ($1, $2, $3) RETURNING id`
	var id int
	err := d.SQL.QueryRow(query, user.Name, user.Email, user.PasswordHash).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("failed to insert auth user: %w", err)
	}
	return id, nil
}

// GetAuthUserByEmail retrieves an authentication user by their email address.
func (d *DB) GetAuthUserByEmail(email string) (*models.AuthUser, error) {
	var user models.AuthUser
	query := "SELECT id, name, email, password_hash, created_at, updated_at FROM auth_users WHERE email = $1"
	err := d.SQL.QueryRow(query, email).Scan(
		&user.ID, &user.Name, &user.Email, &user.PasswordHash, &user.CreatedAt, &user.UpdatedAt)
	if err == sql.ErrNoRows {
		return nil, nil // User not found, which is a valid scenario
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get auth user by email: %w", err)
	}
	return &user, nil
}
