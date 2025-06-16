package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"time"
	"user-service/models"
	"user-service/services"

	"github.com/go-playground/validator/v10" // For request validation
	"github.com/gorilla/mux"
)

// UserHandler handles HTTP requests related to user operations.
type UserHandler struct {
	userService *services.UserService
	validate    *validator.Validate // Validator instance
}

// NewUserHandler creates a new instance of UserHandler.
func NewUserHandler(us *services.UserService) *UserHandler {
	return &UserHandler{
		userService: us,
		validate:    validator.New(), // Initialize validator
	}
}

// HealthCheck godoc
// @Summary Show the status of the User Service
// @Description get the status of User Service
// @Tags Health
// @Accept json
// @Produce json
// @Success 200 {object} map[string]string "Service status is healthy"
// @Router /health [get]
func (h *UserHandler) HealthCheck(w http.ResponseWriter, r *http.Request) {
	response := map[string]string{
		"service":   "user-service",
		"status":    "healthy",
		"timestamp": time.Now().Format(time.RFC3339),
		"version":   "1.0.0",
	}
	h.respondWithJSON(w, http.StatusOK, response)
}

// CreateUser godoc
// @Summary Create a new user profile
// @Description Creates a new user profile in the user service.
// @Tags Users
// @Accept json
// @Produce json
// @Param user body models.UserCreateRequest true "User profile to create (name and email only)"
// @Success 201 {object} models.UserResponse "User profile created successfully"
// @Failure 400 {object} map[string]string "Invalid request payload or validation error"
// @Failure 409 {object} map[string]string "Email is already in use"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /users [post]
func (h *UserHandler) CreateUser(w http.ResponseWriter, r *http.Request) {
	var req models.UserCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}

	// Validate the request body using the validator instance
	if err := h.validate.Struct(req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, fmt.Sprintf("Validation error: %v", err.Error()))
		return
	}

	// Call the service layer to create the user
	user, err := h.userService.CreateUser(req)
	if err != nil {
		if err.Error() == "email is already in use" {
			h.respondWithError(w, http.StatusConflict, err.Error())
			return
		}
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to create user: %v", err.Error()))
		return
	}

	h.respondWithJSON(w, http.StatusCreated, user.ToResponse())
}

// GetAllUsers godoc
// @Summary Get all user profiles
// @Description Retrieves a list of all user profiles in the system.
// @Tags Users
// @Accept json
// @Produce json
// @Security BearerAuth
// @Success 200 {array} models.UserResponse "List of user profiles"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /users [get]
func (h *UserHandler) GetAllUsers(w http.ResponseWriter, r *http.Request) {
	users, err := h.userService.GetAllUsers()
	if err != nil {
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to retrieve users: %v", err.Error()))
		return
	}

	// Convert User models to UserResponse models for API output
	var responses []models.UserResponse
	for _, user := range users {
		responses = append(responses, user.ToResponse())
	}

	h.respondWithJSON(w, http.StatusOK, responses)
}

// GetUser godoc
// @Summary Get a user profile by ID
// @Description Retrieves a single user profile by their unique ID.
// @Tags Users
// @Accept json
// @Produce json
// @Security BearerAuth
// @Param id path int true "User ID"
// @Success 200 {object} models.UserResponse "User profile found"
// @Failure 400 {object} map[string]string "Invalid user ID"
// @Failure 404 {object} map[string]string "User not found"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /users/{id} [get]
func (h *UserHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid user ID format")
		return
	}

	user, err := h.userService.GetUserByID(id)
	if err != nil {
		if err.Error() == "user not found" {
			h.respondWithError(w, http.StatusNotFound, err.Error())
			return
		}
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to retrieve user: %v", err.Error()))
		return
	}

	h.respondWithJSON(w, http.StatusOK, user.ToResponse())
}

// UpdateUser godoc
// @Summary Update a user profile by ID
// @Description Updates an existing user profile's information.
// @Tags Users
// @Accept json
// @Produce json
// @Security BearerAuth
// @Param id path int true "User ID"
// @Param user body models.UserUpdateRequest true "Updated user profile (name and email only)"
// @Success 200 {object} models.UserResponse "User profile updated successfully"
// @Failure 400 {object} map[string]string "Invalid request payload or validation error"
// @Failure 404 {object} map[string]string "User not found"
// @Failure 409 {object} map[string]string "Email is already in use by another user"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /users/{id} [put]
func (h *UserHandler) UpdateUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid user ID format")
		return
	}

	var req models.UserUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}

	if err := h.validate.Struct(req); err != nil {
		h.respondWithError(w, http.StatusBadRequest, fmt.Sprintf("Validation error: %v", err.Error()))
		return
	}

	user, err := h.userService.UpdateUser(id, req)
	if err != nil {
		if err.Error() == "user not found" {
			h.respondWithError(w, http.StatusNotFound, err.Error())
			return
		}
		if err.Error() == "email is already in use by another user" {
			h.respondWithError(w, http.StatusConflict, err.Error())
			return
		}
		if err.Error() == "no fields provided for update or no changes detected" {
			h.respondWithError(w, http.StatusBadRequest, err.Error())
			return
		}
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to update user: %v", err.Error()))
		return
	}

	h.respondWithJSON(w, http.StatusOK, user.ToResponse())
}

// DeleteUser godoc
// @Summary Delete a user profile by ID
// @Description Deletes a user profile from the system.
// @Tags Users
// @Accept json
// @Produce json
// @Security BearerAuth
// @Param id path int true "User ID"
// @Success 204 {string} string "User profile deleted successfully"
// @Failure 400 {object} map[string]string "Invalid user ID"
// @Failure 404 {object} map[string]string "User not found"
// @Failure 500 {object} map[string]string "Internal server error"
// @Router /users/{id} [delete]
func (h *UserHandler) DeleteUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil {
		h.respondWithError(w, http.StatusBadRequest, "Invalid user ID format")
		return
	}

	err = h.userService.DeleteUser(id)
	if err != nil {
		if err.Error() == "user not found" {
			h.respondWithError(w, http.StatusNotFound, err.Error())
			return
		}
		h.respondWithError(w, http.StatusInternalServerError, fmt.Sprintf("Failed to delete user: %v", err.Error()))
		return
	}

	w.WriteHeader(http.StatusNoContent) // 204 No Content for successful deletion
}

// Helper functions for consistent API responses.
func (h *UserHandler) respondWithError(w http.ResponseWriter, code int, message string) {
	h.respondWithJSON(w, code, map[string]string{"error": message})
}

func (h *UserHandler) respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload) // Error handling for Marshal might be needed in production
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
