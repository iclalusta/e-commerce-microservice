package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"user-service/models"
	"user-service/services"

	"github.com/gorilla/mux"
)

// UserHandler, veritabanı bağlantısını tutar.
type UserHandler struct {
	UserService *services.UserService
}

// NewUserHandler, yeni bir UserHandler örneği oluşturur.
func NewUserHandler(db *sql.DB) *UserHandler {
	return &UserHandler{
		UserService: services.NewUserService(db),
	}
}

// RegisterUser, yeni bir kullanıcı kaydeder.
func (h *UserHandler) RegisterUser(w http.ResponseWriter, r *http.Request) {
	var user models.User
	if err := json.NewDecoder(r.Body).Decode(&user); err != nil {
		http.Error(w, `{"error": "Geçersiz istek gövdesi"}`, http.StatusBadRequest)
		return
	}

	// Kullanıcıyı kaydetme işlemini servise devret
	createdUser, err := h.UserService.RegisterUser(&user)
	if err != nil {
		// Burada daha detaylı hata yönetimi yapılabilir (örn: email zaten var)
		http.Error(w, `{"error": "Kullanıcı kaydedilemedi"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(createdUser)
}

// GetUserByEmail, bir kullanıcıyı email adresine göre bulur.
// Bu endpoint, auth-service tarafından login işlemi için kullanılır.
func (h *UserHandler) GetUserByEmail(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	email := vars["email"]

	user, err := h.UserService.GetUserByEmail(email)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, `{"error": "Kullanıcı bulunamadı"}`, http.StatusNotFound)
		} else {
			http.Error(w, `{"error": "Sunucu hatası"}`, http.StatusInternalServerError)
		}
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(user)
}