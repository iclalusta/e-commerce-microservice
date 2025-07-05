package models

import (
	"time"
)

// User represents the user profile data stored in the User Service database.
type User struct {
	ID        int       `json:"id" db:"id"`
	Name      string    `json:"name" db:"name"`
	Email     string    `json:"email" db:"email"`
	CreatedAt time.Time `json:"created_at" db:"created_at"` // Consistent field naming
	UpdatedAt time.Time `json:"updated_at" db:"updated_at"` // Consistent field naming
}

// UserCreateRequest is the payload for creating a new user profile.
// This is what Auth Service will send to User Service.
type UserCreateRequest struct {
	ID    int    `json:"id"` // ← eklendi
	Name  string `json:"name" validate:"required,min=2,max=100"`
	Email string `json:"email" validate:"required,email"`
}

// UserUpdateRequest is the payload for updating an existing user profile.
type UserUpdateRequest struct {
	Name  string `json:"name" validate:"omitempty,min=2,max=100"`
	Email string `json:"email" validate:"omitempty,email"`
}

// UserResponse is the simplified user data returned to clients, excluding sensitive or internal fields.
type UserResponse struct {
	ID        int       `json:"id"`
	Name      string    `json:"name"`
	Email     string    `json:"email"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// ToResponse converts a User model to a UserResponse for API output.
func (u *User) ToResponse() UserResponse {
	return UserResponse{
		ID:        u.ID,
		Name:      u.Name,
		Email:     u.Email,
		CreatedAt: u.CreatedAt,
		UpdatedAt: u.UpdatedAt,
	}
}
