package handlers

import (
	"auth-service/models"
	"auth-service/services"
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/golang-jwt/jwt/v5"
)

type AuthHandler struct {
	service *services.AuthService
}

func NewAuthHandler(db *sql.DB, jwtSecret string) *AuthHandler {
	return &AuthHandler{
		service: services.NewAuthService(db, jwtSecret),
	}
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req models.RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error": "Geçersiz istek"}`, http.StatusBadRequest)
		return
	}

	authUser, err := h.service.Register(&req)
	if err != nil {
		log.Printf("Auth servisinde kayıt hatası: %v", err)
		http.Error(w, `{"error": "Kullanıcı kaydedilemedi"}`, http.StatusInternalServerError)
		return
	}

	// Kullanıcı profili oluştur (User Service) - ID ile birlikte gönderiyoruz!
	userProfileRequest := map[string]interface{}{
		"id":    authUser.ID, // ← kritik: aynı ID'yi gönderiyoruz
		"name":  authUser.Name,
		"email": authUser.Email,
	}
	jsonData, _ := json.Marshal(userProfileRequest)
	resp, err := http.Post("http://user-service:8082/api/user/create", "application/json", bytes.NewBuffer(jsonData))
	if err != nil || resp.StatusCode != http.StatusCreated {
		log.Printf("User servisine istek atılırken hata oluştu: %v", err)
		http.Error(w, `{"error": "Kullanıcı profili oluşturulamadı"}`, http.StatusInternalServerError)
		return
	}

	token, err := h.service.GenerateToken(authUser)
	if err != nil {
		http.Error(w, `{"error": "Token oluşturulamadı"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"token": token})
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var req models.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error": "Geçersiz istek"}`, http.StatusBadRequest)
		return
	}

	token, err := h.service.Login(&req)
	if err != nil {
		http.Error(w, `{"error": "Geçersiz e-posta veya şifre"}`, http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"token": token})
}

func (h *AuthHandler) ValidateToken(w http.ResponseWriter, r *http.Request) {
	tokenString := r.Header.Get("Authorization")
	tokenString = strings.TrimPrefix(tokenString, "Bearer ")

	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("Beklenmeyen imzalama yöntemi")
		}
		return []byte(h.service.JWTSecret), nil
	})

	if err != nil || !token.Valid {
		http.Error(w, `{"error": "Geçersiz token"}`, http.StatusUnauthorized)
		return
	}

	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		http.Error(w, `{"error": "Token çözümlenemedi"}`, http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(claims)
}
