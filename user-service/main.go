package main

import (
	"log"
	"net/http"
	"time"
	"user-service/config"
	"user-service/database"
	"user-service/handlers"
	"user-service/middleware"
	"user-service/services"

	"github.com/gorilla/mux"
)

func main() {
	cfg := config.LoadConfig()

	db := database.NewConnection(cfg)
	defer func() {
		if err := db.SQL.Close(); err != nil {
			log.Fatalf("Error closing database connection: %v", err)
		}
		log.Println("Database connection closed.")
	}()

	userService := services.NewUserService(db)
	userHandler := handlers.NewUserHandler(userService)

	// Auth Middleware'i oluştur
	authMiddleware := middleware.NewAuthMiddleware(cfg.AuthServiceURL)

	r := mux.NewRouter()

	// Health Check endpoint
	r.HandleFunc("/api/v1/health", userHandler.HealthCheck).Methods("GET")

	// API rotaları için alt router oluştur
	apiRouter := r.PathPrefix("/api/v1").Subrouter()

	// Kimlik doğrulama gerektirmeyen rotalar
	// Yeni kullanıcı oluşturma (Auth Service'ten çağrılacak)
	apiRouter.HandleFunc("/users", userHandler.CreateUser).Methods("POST")

	// Kimlik doğrulama gerektiren rotalar için ayrı bir alt router
	// Bu kısım, Auth Middleware ile korunacak
	protectedRouter := apiRouter.PathPrefix("/users").Subrouter()
	protectedRouter.Use(authMiddleware.Authenticate) // Bu middleware'i burada kullanıyoruz

	// DİKKAT: Buradaki method adlarını, user_handler.go dosyasındaki metod adlarıyla EŞLEŞTİRDİK.
	protectedRouter.HandleFunc("", userHandler.GetAllUsers).Methods("GET")  // Düzeltildi: GetUsers yerine GetAllUsers
	protectedRouter.HandleFunc("/{id}", userHandler.GetUser).Methods("GET") // GetUserByID yerine GetUser
	protectedRouter.HandleFunc("/{id}", userHandler.UpdateUser).Methods("PUT")
	protectedRouter.HandleFunc("/{id}", userHandler.DeleteUser).Methods("DELETE")

	port := ":" + cfg.ServicePort
	log.Printf("User Service starting on port %s", port)
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
