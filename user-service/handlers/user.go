package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"user-service/models"
	"user-service/services"

	"github.com/gorilla/mux"
)

type UserHandler struct {
	service *services.UserService
}

func NewUserHandler(db *sql.DB) *UserHandler {
	return &UserHandler{
		service: services.NewUserService(db),
	}
}

// CreateUser, auth-service'ten gelen dahili istek üzerine kullanıcı profili oluşturur.
func (h *UserHandler) CreateUser(w http.ResponseWriter, r *http.Request) {
	var req models.UserCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"error": "Geçersiz istek"}`, http.StatusBadRequest)
		return
	}

	user, err := h.service.CreateUser(&req)
	if err != nil {
		http.Error(w, `{"error": "Kullanıcı profili oluşturulamadı"}`, http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(user.ToResponse())
}

func (h *UserHandler) GetUserByID(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		http.Error(w, `{"error": "Geçersiz ID"}`, http.StatusBadRequest)
		return
	}

	user, err := h.service.GetUserByID(id)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, `{"error": "Kullanıcı bulunamadı"}`, http.StatusNotFound)
			return
		}
		http.Error(w, `{"error": "Sunucu hatası"}`, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(user.ToResponse())
}
func (h *UserHandler) GetAllUsers(w http.ResponseWriter, r *http.Request) {
	users, err := h.service.GetAllUsers()
	if err != nil {
		http.Error(w, `{"error": "Sunucu hatası"}`, http.StatusInternalServerError)
		return
	}

	var responses []models.UserResponse
	for _, user := range users {
		responses = append(responses, user.ToResponse())
	}
	json.NewEncoder(w).Encode(responses)
}
func (h *UserHandler) GetMe(w http.ResponseWriter, r *http.Request) {
	userIDHeader := r.Header.Get("X-User-Id")
	if userIDHeader == "" {
		http.Error(w, `{"error": "X-User-Id header'ı eksik"}`, http.StatusUnauthorized)
		return
	}

	// 🧼 Trim whitespace gibi şeyleri temizle
	userIDHeader = strings.TrimSpace(userIDHeader)

	// 🔢 String → Int dönüşümü
	userID, err := strconv.Atoi(userIDHeader)
	if err != nil {
		http.Error(w, `{"error": "Geçersiz kullanıcı ID formatı"}`, http.StatusBadRequest)
		return
	}

	user, err := h.service.GetUserByID(userID)
	if err != nil {
		http.Error(w, `{"error": "Kullanıcı bulunamadı"}`, http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(user.ToResponse())
}
