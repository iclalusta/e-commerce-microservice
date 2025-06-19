package handlers

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"log"
	"net/http"
	"auth-service/models"
	"auth-service/services"
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

	// 1. Auth veritabanına kullanıcıyı kaydet
	authUser, err := h.service.Register(&req)
	if err != nil {
		log.Printf("Auth servisinde kayıt hatası: %v", err)
		http.Error(w, `{"error": "Kullanıcı kaydedilemedi"}`, http.StatusInternalServerError)
		return
	}

	// 2. User servisine profil oluşturması için istek gönder
	userProfileRequest := map[string]string{"name": authUser.Name, "email": authUser.Email}
	jsonData, _ := json.Marshal(userProfileRequest)
	
	// Docker network'ü üzerinden user-service'e istek at
	resp, err := http.Post("http://user-service:8082/user/create", "application/json", bytes.NewBuffer(jsonData))
	if err != nil || resp.StatusCode != http.StatusCreated {
		log.Printf("User servisine istek atılırken hata oluştu: %v", err)
		// Burada rollback mantığı eklenebilir (auth_users tablosundan silme)
		http.Error(w, `{"error": "Kullanıcı profili oluşturulamadı"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"message": "Kullanıcı başarıyla oluşturuldu"})
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