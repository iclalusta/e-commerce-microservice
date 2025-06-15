package services

import (
	"database/sql"
	"fmt"
	"user-service/database"
	"user-service/models"
)

type UserService struct {
	db *database.DB // Reference to the database connection
}

func NewUserService(db *database.DB) *UserService {
	return &UserService{db: db}
}

// CreateUser creates a new user profile in the database.
// It receives a UserCreateRequest (from Auth Service, for example).
func (s *UserService) CreateUser(req models.UserCreateRequest) (*models.User, error) {
	// Check if user with this email already exists
	existingUser, err := s.GetUserByEmail(req.Email)
	if err != nil {
		return nil, fmt.Errorf("failed to check existing user during creation: %w", err)
	}
	if existingUser != nil {
		return nil, fmt.Errorf("email is already in use")
	}

	user := &models.User{
		Name:  req.Name,
		Email: req.Email,
	}

	// Insert the new user into the database. ID, CreatedAt, UpdatedAt will be set by DB.
	err = s.db.InsertUser(user) // Use the InsertUser method from database package
	if err != nil {
		return nil, fmt.Errorf("failed to create user: %w", err)
	}
	return user, nil
}

// GetUserByID retrieves a user profile by their ID.
func (s *UserService) GetUserByID(id int) (*models.User, error) {
	user, err := s.db.GetUserByID(id) // Use the GetUserByID method from database package
	if err != nil {
		// Differentiate between "not found" and other database errors
		if err == sql.ErrNoRows { // This check should ideally be done in database layer
			return nil, fmt.Errorf("user not found") // Propagate specific error
		}
		return nil, fmt.Errorf("failed to retrieve user by ID: %w", err)
	}
	return user, nil
}

// GetAllUsers retrieves all user profiles from the database.
func (s *UserService) GetAllUsers() ([]models.User, error) {
	users, err := s.db.GetAllUsers() // Use the GetAllUsers method from database package
	if err != nil {
		return nil, fmt.Errorf("failed to retrieve all users: %w", err)
	}
	return users, nil
}

// UpdateUser updates an existing user's profile information.
func (s *UserService) UpdateUser(id int, req models.UserUpdateRequest) (*models.User, error) {
	existingUser, err := s.GetUserByID(id)
	if err != nil {
		return nil, err // This already handles "user not found" case
	}
	if existingUser == nil { // Explicit check if GetUserByID returns nil for not found
		return nil, fmt.Errorf("user not found")
	}

	// Track if any field is actually updated
	updated := false

	// Update only if provided and different
	if req.Name != "" && req.Name != existingUser.Name {
		existingUser.Name = req.Name
		updated = true
	}
	if req.Email != "" && req.Email != existingUser.Email {
		// Check if the new email is already in use by another user
		conflictUser, err := s.GetUserByEmail(req.Email)
		if err != nil {
			return nil, fmt.Errorf("failed to check email conflict: %w", err)
		}
		if conflictUser != nil && conflictUser.ID != existingUser.ID {
			return nil, fmt.Errorf("email is already in use by another user")
		}
		existingUser.Email = req.Email
		updated = true
	}

	if !updated {
		return existingUser, fmt.Errorf("no fields provided for update or no changes detected")
	}

	err = s.db.UpdateUser(existingUser) // Use the UpdateUser method from database package
	if err != nil {
		return nil, fmt.Errorf("failed to update user: %w", err)
	}

	// Fetch the updated user to ensure latest timestamps are returned
	return s.db.GetUserByID(existingUser.ID)
}

// DeleteUser deletes a user profile by their ID.
func (s *UserService) DeleteUser(id int) error {
	existingUser, err := s.GetUserByID(id)
	if err != nil {
		return err // Handles "user not found"
	}
	if existingUser == nil { // Explicit check if GetUserByID returns nil for not found
		return fmt.Errorf("user not found")
	}

	err = s.db.DeleteUser(id) // Use the DeleteUser method from database package
	if err != nil {
		return fmt.Errorf("failed to delete user: %w", err)
	}
	return nil
}

// GetUserByEmail retrieves a user by their email address.
// This is often used internally for conflict checks.
func (s *UserService) GetUserByEmail(email string) (*models.User, error) {
	var user models.User
	query := "SELECT id, name, email, created_at, updated_at FROM users WHERE email = $1"
	err := s.db.SQL.QueryRow(query, email).Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt)
	if err == sql.ErrNoRows {
		return nil, nil // User not found, which is a valid scenario for email checks
	}
	if err != nil {
		return nil, fmt.Errorf("failed to get user by email from DB: %w", err)
	}
	return &user, nil
}
