package services

import (
	"database/sql"
	"time"
	"user-service/models"
)

type UserService struct {
	DB *sql.DB
}

func NewUserService(db *sql.DB) *UserService {
	return &UserService{DB: db}
}

func (s *UserService) CreateUser(req *models.UserCreateRequest) (*models.User, error) {
	user := &models.User{
		ID:        req.ID,
		Name:      req.Name,
		Email:     req.Email,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	query := `INSERT INTO users (id, name, email, created_at, updated_at) VALUES ($1, $2, $3, $4, $5)`
	_, err := s.DB.Exec(query, user.ID, user.Name, user.Email, user.CreatedAt, user.UpdatedAt)
	if err != nil {
		return nil, err
	}

	return user, nil
}

func (s *UserService) GetUserByEmail(email string) (*models.User, error) {
	user := &models.User{}
	query := `SELECT id, name, email, created_at, updated_at FROM users WHERE email = $1`
	err := s.DB.QueryRow(query, email).Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt)
	if err != nil {
		return nil, err
	}
	return user, nil
}

func (s *UserService) GetUserByID(id int) (*models.User, error) {
	user := &models.User{}
	query := `SELECT id, name, email, created_at, updated_at FROM users WHERE id = $1`
	err := s.DB.QueryRow(query, id).Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt)
	return user, err
}

func (s *UserService) GetAllUsers() ([]models.User, error) {
	rows, err := s.DB.Query(`SELECT id, name, email, created_at, updated_at FROM users`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var users []models.User
	for rows.Next() {
		var user models.User
		if err := rows.Scan(&user.ID, &user.Name, &user.Email, &user.CreatedAt, &user.UpdatedAt); err != nil {
			return nil, err
		}
		users = append(users, user)
	}
	return users, nil
}
