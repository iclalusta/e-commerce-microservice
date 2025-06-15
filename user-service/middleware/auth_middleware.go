package middleware

import (
	"encoding/json"
	"fmt"
	"io" // ioutil yerine io kullanıyoruz
	"log"
	"net/http"
	"strings"
	"time"
)

// AuthMiddleware handles JWT token validation by communicating with the Auth Service.
type AuthMiddleware struct {
	authServiceURL string
}

// NewAuthMiddleware creates a new AuthMiddleware instance.
func NewAuthMiddleware(authServiceURL string) *AuthMiddleware {
	return &AuthMiddleware{
		authServiceURL: authServiceURL,
	}
}

// Authenticate is the middleware function to validate JWT tokens.
func (m *AuthMiddleware) Authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			log.Println("Authentication failed: Authorization header missing")
			respondWithError(w, http.StatusUnauthorized, "Authorization header required")
			return
		}

		tokenString := strings.TrimPrefix(authHeader, "Bearer ")
		if tokenString == authHeader { // No "Bearer " prefix found
			log.Println("Authentication failed: Bearer token not found in header")
			respondWithError(w, http.StatusUnauthorized, "Bearer token required")
			return
		}

		// Call Auth Service to validate the token
		validateURL := m.authServiceURL + "/api/v1/auth/validate"
		req, err := http.NewRequest("POST", validateURL, nil)
		if err != nil {
			log.Printf("Authentication failed: Error creating validation request: %v", err)
			respondWithError(w, http.StatusInternalServerError, "Internal server error during authentication")
			return
		}
		req.Header.Set("Authorization", authHeader) // Pass the original header

		client := &http.Client{Timeout: 5 * time.Second}
		resp, err := client.Do(req)
		if err != nil {
			log.Printf("Authentication failed: Error calling Auth Service at %s: %v", validateURL, err)
			respondWithError(w, http.StatusServiceUnavailable, "Authentication service unavailable")
			return
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusOK {
			// Read the error message from Auth Service if available
			bodyBytes, readErr := io.ReadAll(resp.Body) // io.ReadAll kullanıldı
			if readErr != nil {
				log.Printf("Authentication failed: Could not read Auth Service response body: %v", readErr)
			}

			errorMessage := fmt.Sprintf("Token validation failed with status %d", resp.StatusCode)
			var authSvcError map[string]string
			if err := json.Unmarshal(bodyBytes, &authSvcError); err == nil {
				if msg, ok := authSvcError["error"]; ok {
					errorMessage = msg
				}
			} else {
				log.Printf("Authentication failed: Could not unmarshal Auth Service error response as JSON: %v. Raw body: %s", err, string(bodyBytes))
				if len(bodyBytes) > 0 { // Eğer JSON değilse ve boş değilse, ham yanıtı göster
					errorMessage = fmt.Sprintf("%s. Auth Service response: %s", errorMessage, string(bodyBytes))
				}
			}

			log.Printf("Authentication failed: Auth Service responded with status %d: %s", resp.StatusCode, errorMessage)
			respondWithError(w, http.StatusUnauthorized, errorMessage)
			return
		}

		// Token is valid, get claims and pass them to the context
		var claims map[string]interface{}
		if err := json.NewDecoder(resp.Body).Decode(&claims); err != nil {
			log.Printf("Authentication failed: Error decoding claims from Auth Service: %v", err)
			respondWithError(w, http.StatusInternalServerError, "Internal server error during authentication")
			return
		}

		// Add claims to the request context for downstream handlers
		// For example, you can add user_id, email, etc.
		// ctx := context.WithValue(r.Context(), "userID", int(claims["user_id"].(float64)))
		// r = r.WithContext(ctx)
		// For now, let's just log and pass through.

		log.Printf("Token validated successfully for user_id: %v", claims["user_id"])

		next.ServeHTTP(w, r)
	})
}

// Helper for consistent error responses in middleware
func respondWithError(w http.ResponseWriter, code int, message string) {
	response := map[string]string{"error": message}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(response)
}
