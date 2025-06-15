package main

import (
	"auth-service/config"
	"auth-service/database"
	"auth-service/handlers"
	"auth-service/services"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/mux"
)

func main() {
	cfg := config.LoadConfig()

	// Initialize database connection
	db := database.NewConnection(cfg)
	// Ensure the database connection is closed when the main function exits
	defer func() {
		if err := db.SQL.Close(); err != nil { // Correct way to close the embedded sql.DB
			log.Fatalf("Error closing database connection: %v", err)
		}
		log.Println("Database connection closed.")
	}()

	// Initialize auth service and handler
	authService := services.NewAuthService(db, cfg.JWTSecret)
	authHandler := handlers.NewAuthHandler(authService)

	// Set up router
	r := mux.NewRouter()

	// Health Check endpoint
	r.HandleFunc("/api/v1/health", authHandler.HealthCheck).Methods("GET")

	// Authentication related routes
	apiRouter := r.PathPrefix("/api/v1/auth").Subrouter()
	apiRouter.HandleFunc("/register", authHandler.Register).Methods("POST")
	apiRouter.HandleFunc("/login", authHandler.Login).Methods("POST")
	apiRouter.HandleFunc("/validate", authHandler.TokenValidate).Methods("POST") // For internal service use

	// Start the HTTP server
	port := ":" + cfg.ServicePort // Assuming ServicePort is a string in config
	log.Printf("Auth Service starting on port %s", port)
	srv := &http.Server{
		Handler:      r,
		Addr:         port,
		WriteTimeout: 15 * time.Second,
		ReadTimeout:  15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("Could not listen on %s: %v\n", port, err)
	}
}
