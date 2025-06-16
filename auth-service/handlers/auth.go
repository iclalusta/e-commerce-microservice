package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strings"
	"auth-service/models"
	"auth-service/services"

	"github.com/golang-jwt/jwt/v5"
)

// AuthHandler, veritabanı bağlantısını ve JWT anahtarını tutar.
type AuthHandler struct {
	AuthService *services.AuthService
}

// NewAuthHandler, yeni bir AuthHandler örneği oluşturur.
func NewAuthHandler(db *sql.DB, jwtSecret string) *AuthHandler {
	return &AuthHandler{
		AuthService: services.NewAuthService(db, jwtSecret),
	}
}

// Login, kullanıcı girişi yapar ve token döner.
func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var creds models.Credentials
	if err := json.NewDecoder(r.Body).Decode(&creds); err != nil {
		http.Error(w, `{"error": "Geçersiz istek"}`, http.StatusBadRequest)
		return
	}

	// Login işlemini servise devret
	// Not: Burada auth-service, user-service'in veritabanına erişiyor.
	// Bu, "shared database" desenidir. Alternatif olarak HTTP call yapılabilirdi.
	token, err := h.AuthService.Login(creds.Email, creds.Password)
	if err != nil {
		if err == sql.ErrNoRows || err.Error() == "geçersiz şifre" {
			http.Error(w, `{"error": "Geçersiz e-posta veya şifre"}`, http.StatusUnauthorized)
		} else {
			http.Error(w, `{"error": "Giriş başarısız"}`, http.StatusInternalServerError)
		}
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"token": token})
}

// ValidateToken, gelen token'ın geçerliliğini kontrol eder.
func (h *AuthHandler) ValidateToken(w http.ResponseWriter, r *http.Request) {
	authHeader := r.Header.Get("Authorization")
	if authHeader == "" {
		http.Error(w, "Authorization header eksik", http.StatusUnauthorized)
		return
	}

	tokenString := strings.TrimPrefix(authHeader, "Bearer ")
	if tokenString == authHeader {
		http.Error(w, "Bearer token formatı hatalı", http.StatusUnauthorized)
		return
	}

	isValid := h.AuthService.ValidateToken(tokenString)
	if !isValid {
		http.Error(w, "Geçersiz token", http.StatusUnauthorized)
		return
	}

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]bool{"valid": true})
}