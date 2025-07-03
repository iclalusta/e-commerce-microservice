package services

import (
	"auth-service/models"
	"database/sql"
	"errors"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

type AuthService struct {
	DB        *sql.DB
	JWTSecret string
}

func NewAuthService(db *sql.DB, jwtSecret string) *AuthService {
	return &AuthService{DB: db, JWTSecret: jwtSecret}
}

func (s *AuthService) Register(req *models.RegisterRequest) (*models.AuthUser, error) {
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return nil, err
	}

	authUser := &models.AuthUser{
		Name:         req.Name,
		Email:        req.Email,
		PasswordHash: string(hashedPassword),
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}

	query := `INSERT INTO auth_users (name, email, password_hash, created_at, updated_at) VALUES ($1, $2, $3, $4, $5) RETURNING id`
	err = s.DB.QueryRow(query, authUser.Name, authUser.Email, authUser.PasswordHash, authUser.CreatedAt, authUser.UpdatedAt).Scan(&authUser.ID)
	if err != nil {
		return nil, err
	}

	return authUser, nil
}

func (s *AuthService) Login(req *models.LoginRequest) (string, error) {
	authUser := &models.AuthUser{}
	query := `SELECT id, email, password_hash FROM auth_users WHERE email = $1`
	err := s.DB.QueryRow(query, req.Email).Scan(&authUser.ID, &authUser.Email, &authUser.PasswordHash)
	if err != nil {
		if err == sql.ErrNoRows {
			return "", errors.New("geçersiz e-posta veya şifre")
		}
		return "", err
	}

	err = bcrypt.CompareHashAndPassword([]byte(authUser.PasswordHash), []byte(req.Password))
	if err != nil {
		return "", errors.New("geçersiz e-posta veya şifre")
	}

	claims := jwt.MapClaims{
		"sub":   authUser.ID,
		"email": authUser.Email,
		"name":  authUser.Name,
		"exp":   time.Now().Add(time.Hour * 72).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)

	return token.SignedString([]byte(s.JWTSecret))
}

func (s *AuthService) GenerateToken(authUser *models.AuthUser) (string, error) {
	claims := jwt.MapClaims{
		"sub":   authUser.ID,
		"email": authUser.Email,
		"name":  authUser.Name,
		"exp":   time.Now().Add(time.Hour * 72).Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(s.JWTSecret))
}
