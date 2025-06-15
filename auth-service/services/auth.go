package services

import (
	"auth-service/database"
	"auth-service/models"
	"fmt"
	"time"

	jwt "github.com/golang-jwt/jwt/v5" // Using v5 of go-jwt
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	db        *database.DB
	jwtSecret string
}

func NewAuthService(db *database.DB, jwtSecret string) *AuthService {
	return &AuthService{db: db, jwtSecret: jwtSecret}
}

// Register registers a new user in the auth database.
func (s *AuthService) Register(name, email, hashedPassword string) (int, error) {
	user := &models.AuthUser{
		Name:         name,
		Email:        email,
		PasswordHash: hashedPassword,
	}
	id, err := s.db.InsertAuthUser(user)
	if err != nil {
		return 0, fmt.Errorf("service failed to register user: %w", err)
	}
	return id, nil
}

// AuthenticateUser checks user credentials and returns the user if valid.
func (s *AuthService) AuthenticateUser(email, password string) (*models.AuthUser, error) {
	user, err := s.db.GetAuthUserByEmail(email)
	if err != nil {
		return nil, fmt.Errorf("service failed to get user by email: %w", err)
	}
	if user == nil {
		return nil, fmt.Errorf("user not found")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)); err != nil {
		return nil, fmt.Errorf("invalid credentials")
	}
	return user, nil
}

// GetUserByEmail retrieves an auth user by email (used for registration check).
func (s *AuthService) GetUserByEmail(email string) (*models.AuthUser, error) {
	user, err := s.db.GetAuthUserByEmail(email)
	if err != nil {
		return nil, fmt.Errorf("service failed to get user by email: %w", err)
	}
	return user, nil
}

// GenerateToken generates a JWT token for the authenticated user.
func (s *AuthService) GenerateToken(userID int, email, name string) (string, error) {
	claims := jwt.MapClaims{
		"user_id": userID,
		"email":   email,
		"name":    name,
		"exp":     time.Now().Add(time.Hour * 24).Unix(), // Token expires in 24 hours
		"iss":     "auth-service",                        // Issuer
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(s.jwtSecret))
	if err != nil {
		return "", fmt.Errorf("failed to sign token: %w", err)
	}
	return tokenString, nil
}

// ValidateToken validates a JWT token and returns its claims.
func (s *AuthService) ValidateToken(tokenString string) (jwt.MapClaims, error) {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		// Verify the signing method
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(s.jwtSecret), nil
	})

	if err != nil {
		return nil, fmt.Errorf("token validation error: %w", err)
	}

	if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
		return claims, nil
	}
	return nil, fmt.Errorf("invalid token")
}
