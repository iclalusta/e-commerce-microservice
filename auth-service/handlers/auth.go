package handlers

import (
	"auth-service/models"
	"auth-service/services"
	"bytes"
	"encoding/json"
	"fmt"
	"io" // Burası artık doğru bir şekilde eklendi
	"log"
	"net/http"

	// "strings" // Bu import artık kullanılmadığı için kaldırılabilir, otomatik go mod tidy ile de temizlenir
	"time"

	"github.com/go-playground/validator/v10"
	"golang.org/x/crypto/bcrypt"
)

type AuthHandler struct {
	authService *services.AuthService
	validate    *validator.Validate
}

func NewAuthHandler(as *services.AuthService) *AuthHandler {
	return &AuthHandler{
		authService: as,
		validate:    validator.New(),
	}
}

// HealthCheck godoc
// @Summary Show the status of the Auth Service
// @Description get the status of Auth Service
// @Tags Health
// @Accept json
// @Produce json
// @Success 200 {object} map[string]string "Service status is healthy"
// @Router /health [get]
func (h *AuthHandler) HealthCheck(w http.ResponseWriter, r *http.Request) {
	response := map[string]string{
		"service":   "auth-service",
		"status":    "healthy",
		"timestamp": time.Now().Format(time.RFC3339),
		"version":   "1.0.0",
	}
	h.respondWithJSON(w, http.StatusOK, response)
}

// Register godoc
// @Summary Register a new user
// @Description Registers a new user and creates their profile in the user service.
// @Tags Auth
// @Accept json
// @Produce json
// @Param user body models.RegisterRequest true "User registration details"
// @Success 201 {object} map[string]interface{} "User registered successfully"
// @Failure 400 {object} map[string]string "Invalid request payload or validation error"
// @Failure 409 {object} map[string]string "Email is already registered"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /auth/register [post]
func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req models.RegisterRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}

	if err := h.validate.Struct(req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, fmt.Sprintf("Validation error: %v", err.Error()))
		return
	}

	// Check if email already exists in Auth Service
	existingUser, err := h.authService.GetUserByEmail(req.Email)
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, "Database error during email check")
		return
	}
	if existingUser != nil {
		h.respondWithError(w, http.StatusConflict, "Email is already registered")
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, "Failed to hash password")
		return
	}

	// 1. Register user in Auth Service's database
	authUserID, err := h.authService.Register(req.Name, req.Email, string(hashedPassword))
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to register user in Auth DB: %v", err.Error()))
		return
	}
	log.Printf("User registered in Auth DB with ID: %d", authUserID)

	// 2. Create user profile in User Service (Send only name and email)
	userProfileData := map[string]interface{}{
		"name":  req.Name,
		"email": req.Email,
	}
	jsonUserProfile, err := json.Marshal(userProfileData)
	if err != nil {
		log.Printf("Error marshalling user profile data for User Service: %v", err)
		h.respondWithError(w, http.StatusInternalServerError, "Internal server error during user profile creation")
		return
	}

	// Use service name "user-service" from docker-compose for inter-service communication
	userSvcURL := "http://user-service:8080/api/v1/users" // Assuming User Service runs on 8080
	resp, err := http.Post(userSvcURL, "application/json", bytes.NewBuffer(jsonUserProfile))
	if err != nil {
		log.Printf("Error sending user profile to User Service (%s): %v", userSvcURL, err)
		h.respondWithError(w, http.StatusServiceUnavailable, "Failed to connect to User Service for profile creation")
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusCreated {
		var userSvcErrorMsg string
		var userSvcErrorMap map[string]string // Nil olarak başlatılabilir, veya make ile
		// Deneme: User Servisi'nden gelen yanıtı JSON olarak ayrıştırmaya çalış
		if err := json.NewDecoder(resp.Body).Decode(&userSvcErrorMap); err != nil {
			// Eğer JSON ayrıştırma başarısız olursa, yanıtı string olarak oku
			bodyBytes, readErr := io.ReadAll(resp.Body)
			if readErr != nil {
				log.Printf("Failed to read User Service error response body: %v", readErr)
				userSvcErrorMsg = "Unknown error from User Service (could not read response)"
			} else {
				userSvcErrorMsg = fmt.Sprintf("User Service returned non-JSON error (status %d): %s", resp.StatusCode, string(bodyBytes))
			}
			log.Printf("Failed to decode User Service error response: %v. Raw response: %s", err, userSvcErrorMsg)
		} else if errMsg, ok := userSvcErrorMap["error"]; ok {
			userSvcErrorMsg = errMsg
			log.Printf("User Service responded with status %d: %s", resp.StatusCode, userSvcErrorMsg)
		} else {
			userSvcErrorMsg = "Unknown error from User Service (no 'error' field in JSON)"
			log.Printf("User Service responded with status %d: JSON without 'error' field: %+v", resp.StatusCode, userSvcErrorMap)
		}

		// Eğer User Service 409 Conflict dönerse, bunu doğrudan istemciye iletelim
		if resp.StatusCode == http.StatusConflict {
			h.respondWithError(w, http.StatusConflict, userSvcErrorMsg)
			return
		}

		// Diğer tüm başarısız durumlar için genel bir Internal Server Error döndür
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to create user profile in User Service: %s", userSvcErrorMsg))
		return
	}
	log.Printf("User profile created successfully in User Service for user: %s", req.Email)

	// 3. Generate JWT token
	token, err := h.authService.GenerateToken(authUserID, req.Email, req.Name)
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, "Failed to generate token")
		return
	}

	response := map[string]interface{}{
		"message": "User registered successfully",
		"user_id": authUserID, // This is the ID from Auth DB
		"email":   req.Email,
		"name":    req.Name,
		"token":   token,
	}

	h.respondWithJSON(w, http.StatusCreated, response)
}

// Login godoc
// @Summary User login
// @Description Authenticates a user and returns a JWT token.
// @Tags Auth
// @Accept json
// @Produce json
// @Param credentials body models.LoginRequest true "User login credentials"
// @Success 200 {object} map[string]interface{} "Login successful, returns JWT token"
// @Failure 400 {object} map[string]string "Invalid request payload or validation error"
// @Failure 401 {object} map[string]string "Invalid credentials"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /auth/login [post]
func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var req models.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}

	if err := h.validate.Struct(req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, fmt.Sprintf("Validation error: %v", err.Error()))
		return
	}

	user, err := h.authService.AuthenticateUser(req.Email, req.Password)
	if err != nil {
		if err.Error() == "user not found" || err.Error() == "invalid credentials" {
			h.respondWithError(w, http.StatusUnauthorized, "Invalid credentials")
			return
		}
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Authentication failed: %v", err.Error()))
		return
	}

	token, err := h.authService.GenerateToken(user.ID, user.Email, user.Name)
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, "Failed to generate token")
		return
	}

	response := map[string]interface{}{
		"message": "Login successful",
		"user_id": user.ID,
		"email":   user.Email,
		"name":    user.Name,
		"token":   token,
	}

	h.respondWithJSON(w, http.StatusOK, response)
}

// TokenValidate godoc
// @Summary Validate JWT Token
// @Description Validates the provided JWT token. This endpoint is primarily for internal service use (e.g., by User Service middleware).
// @Tags Auth
// @Accept json
// @Produce json
// @Security BearerAuth
// @Success 200 {object} map[string]string "Token is valid and returns user claims"
// @Failure 401 {object} map[string]string "Invalid or expired token"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /auth/validate [post]
func (h *AuthHandler) TokenValidate(w http.ResponseWriter, r *http.Request) {
	authHeader := r.Header.Get("Authorization")
	if authHeader == "" {
		h.respondWithError(w, http.StatusUnauthorized, "Authorization header required")
		return
	}

	tokenString := ""
	// strings.TrimPrefix de kullanılabilir
	_, err := fmt.Sscanf(authHeader, "Bearer %s", &tokenString)
	if err != nil || tokenString == "" {
		h.respondWithError(w, http.StatusUnauthorized, "Invalid Authorization header format")
		return
	}

	claims, err := h.authService.ValidateToken(tokenString)
	if err != nil {
		h.respondWithError(w, http.StatusUnauthorized, fmt.Sprintf("Invalid or expired token: %v", err.Error()))
		return
	}

	// Respond with claims so the calling service can use user info
	h.respondWithJSON(w, http.StatusOK, claims)
}

// Helper functions for consistent API responses.
func (h *AuthHandler) respondWithError(w http.ResponseWriter, code int, message string) {
	h.respondWithJSON(w, code, map[string]string{"error": message})
}

func (h *AuthHandler) respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload) // Error handling for Marshal might be needed in production
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
