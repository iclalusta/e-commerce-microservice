package main

import (
	"fmt"
	"log"
	"os"
	"time"
	"user-service/config"
	"user-service/database"
	"user-service/handlers"

	"github.com/gin-gonic/gin"
)

func main() {
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Yapılandırma yüklenemedi: %v", err)
	}

	db, err := database.Connect(cfg)
	if err != nil {
		log.Fatalf("Veritabanına bağlanılamadı: %v", err)
	}
	defer db.Close()

	// GIN LOGGING CONFIG
	logConfig := gin.LoggerConfig{
		Formatter: func(param gin.LogFormatterParams) string {
			return fmt.Sprintf("[User-Service] %s | %s | Status: %d | Latency: %s \nPath: %s\nHeaders:\n%s\n",
				param.TimeStamp.Format(time.RFC1123),
				param.Method,
				param.StatusCode,
				param.Latency,
				param.Path,
				param.Request.Header,
			)
		},
		Output: os.Stdout,
	}
	r := gin.New()
	r.Use(gin.LoggerWithConfig(logConfig), gin.Recovery())

	userHandler := handlers.NewUserHandler(db)

	// ROUTES
	userGroup := r.Group("/api/user")
	{
		userGroup.POST("/create", gin.WrapF(userHandler.CreateUser))
		userGroup.GET("/all", gin.WrapF(userHandler.GetAllUsers))
		userGroup.GET("/:id", gin.WrapF(userHandler.GetUserByID))
		userGroup.GET("/me", gin.WrapF(userHandler.GetMe))

	}

	port := "8082"
	log.Printf("User service %s portunda başlatılıyor...", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatalf("Server başlatılamadı: %v", err)
	}
}
